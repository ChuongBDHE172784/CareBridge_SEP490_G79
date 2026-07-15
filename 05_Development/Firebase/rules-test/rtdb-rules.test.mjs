// DCC-TC-020 — verifies database.rules.json (ADR-DCC-004 §4) against a REAL running
// Firebase RTDB + Auth emulator, not a mock. Must be run with the emulators already up:
//
//   cd 05_Development/Firebase
//   firebase emulators:start --only database,auth --project demo-carebridge
//   # in a second terminal:
//   cd rules-test && npm install && npm test
//
// Uses anonymous sign-in (fully supported by the Auth emulator, no service-account/custom-
// token setup needed) purely to obtain two distinct authenticated uids — the Rule under
// test only cares about `auth.uid === $uid`, not the sign-in method used to get there.
import { initializeApp } from 'firebase/app';
import { getAuth, signInAnonymously, connectAuthEmulator } from 'firebase/auth';
import { getDatabase, connectDatabaseEmulator, ref, get, set } from 'firebase/database';
import { initializeApp as initializeAdminApp } from 'firebase-admin/app';
import { getDatabase as getAdminDatabase } from 'firebase-admin/database';

const PROJECT_ID = 'demo-carebridge';
const DB_URL = `http://127.0.0.1:9000/?ns=${PROJECT_ID}-default-rtdb`;

process.env.FIREBASE_DATABASE_EMULATOR_HOST = '127.0.0.1:9000';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';

const adminApp = initializeAdminApp({ projectId: PROJECT_ID, databaseURL: DB_URL });
const clientApp = initializeApp({ apiKey: 'fake-api-key', projectId: PROJECT_ID, databaseURL: DB_URL });
const auth = getAuth(clientApp);
connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
const db = getDatabase(clientApp);
connectDatabaseEmulator(db, '127.0.0.1', 9000);

let failures = 0;
function check(name, condition) {
  console.log((condition ? 'PASS' : 'FAIL') + ' — ' + name);
  if (!condition) failures++;
}

function isPermissionDenied(error) {
  return error.code === 'PERMISSION_DENIED' || /permission.?denied/i.test(error.message ?? '');
}

// User A signs in and gets an event seeded into their own inbox (simulating a backend
// Admin SDK write after a real message send — Admin SDK bypasses Rules just like production).
const credA = await signInAnonymously(auth);
const uidA = credA.user.uid;

try {
  await getAdminDatabase(adminApp).ref(`/user-conversation-events/${uidA}/evt1`).set({
    eventId: 'evt1',
    eventType: 'MESSAGE_SENT',
    conversationId: 'c1',
    resourceId: 'm1',
    occurredAt: Date.now(),
  });
  check('Admin SDK write (simulates backend publish) succeeds — bypasses Rules', true);
} catch (e) {
  check('Admin SDK write (simulates backend publish) succeeds (threw: ' + e.message + ')', false);
}

// 1. A can read A's own inbox.
try {
  const snap = await get(ref(db, `/user-conversation-events/${uidA}`));
  check('own-inbox read succeeds for the owner', snap.exists());
} catch (e) {
  check('own-inbox read succeeds for the owner (threw: ' + e.message + ')', false);
}

// Switch to a second anonymous user B.
await auth.signOut();
const credB = await signInAnonymously(auth);
const uidB = credB.user.uid;

// 2. B cannot read A's inbox.
try {
  await get(ref(db, `/user-conversation-events/${uidA}`));
  check('cross-user read is denied', false);
} catch (e) {
  check('cross-user read is denied', isPermissionDenied(e));
}

// 3. B cannot write to B's own inbox from the client SDK — `.write: false` is absolute,
// even for the owner; only the Admin SDK (server) ever writes.
try {
  await set(ref(db, `/user-conversation-events/${uidB}/fake-event`), { hello: 'world' });
  check('client write to own inbox is denied (write:false is absolute)', false);
} catch (e) {
  check('client write to own inbox is denied (write:false is absolute)', isPermissionDenied(e));
}

console.log(failures === 0 ? '\nALL RTDB RULES ASSERTIONS PASSED (DCC-TC-020)' : `\n${failures} ASSERTION(S) FAILED`);
process.exit(failures === 0 ? 0 : 1);
