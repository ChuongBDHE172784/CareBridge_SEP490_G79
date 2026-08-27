// DCC-TC-020: owner-only Firestore inbox reads and absolute client-write denial.
import { initializeApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, signInAnonymously } from 'firebase/auth';
import { connectFirestoreEmulator, doc, getDoc, getFirestore, setDoc } from 'firebase/firestore';
import { initializeApp as initializeAdminApp } from 'firebase-admin/app';
import { getFirestore as getAdminFirestore } from 'firebase-admin/firestore';

const PROJECT_ID = 'demo-carebridge';
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8088';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';

const adminApp = initializeAdminApp({ projectId: PROJECT_ID });
const clientApp = initializeApp({ apiKey: 'fake-api-key', projectId: PROJECT_ID });
const auth = getAuth(clientApp);
connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
const db = getFirestore(clientApp);
connectFirestoreEmulator(db, '127.0.0.1', 8088);

let failures = 0;
function check(name, condition) {
  console.log((condition ? 'PASS' : 'FAIL') + ' — ' + name);
  if (!condition) failures++;
}
function isPermissionDenied(error) {
  return error.code === 'permission-denied' || /permission.?denied/i.test(error.message ?? '');
}

const credA = await signInAnonymously(auth);
const uidA = credA.user.uid;
const ownEvent = doc(db, 'userConversationEvents', uidA, 'events', 'evt1');
await getAdminFirestore(adminApp).doc(`userConversationEvents/${uidA}/events/evt1`).set({
  eventId: 'evt1', eventType: 'MESSAGE_SENT', conversationId: 'c1', resourceId: 'm1', occurredAt: Date.now(),
});

try {
  check('owner can read own event', (await getDoc(ownEvent)).exists());
} catch (error) {
  check(`owner can read own event (${error.message})`, false);
}

await auth.signOut();
const credB = await signInAnonymously(auth);
const uidB = credB.user.uid;

try {
  await getDoc(doc(db, 'userConversationEvents', uidA, 'events', 'evt1'));
  check('cross-user read is denied', false);
} catch (error) {
  check('cross-user read is denied', isPermissionDenied(error));
}

try {
  await setDoc(doc(db, 'userConversationEvents', uidB, 'events', 'fake-event'), { hello: 'world' });
  check('client write to own inbox is denied', false);
} catch (error) {
  check('client write to own inbox is denied', isPermissionDenied(error));
}

await auth.signOut();
try {
  await getDoc(doc(db, 'userConversationEvents', uidA, 'events', 'evt1'));
  check('unauthenticated read is denied', false);
} catch (error) {
  check('unauthenticated read is denied', isPermissionDenied(error));
}

console.log(failures === 0 ? '\nALL FIRESTORE RULES ASSERTIONS PASSED' : `\n${failures} ASSERTION(S) FAILED`);
process.exit(failures === 0 ? 0 : 1);
