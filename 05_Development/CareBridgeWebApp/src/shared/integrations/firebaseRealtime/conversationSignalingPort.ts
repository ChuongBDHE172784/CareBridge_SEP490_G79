import { getAuth, signInWithCustomToken } from 'firebase/auth';
import { getDatabase, onChildAdded, ref, type Unsubscribe } from 'firebase/database';
import { fetchFirebaseCustomToken } from '../../../features/directChat/services/firebaseTokenApi';
import { getFirebaseWebApp, isFirebaseConfigured } from './firebaseApp';
import { parseConversationEventSignal, type ConversationEventSignal } from './conversationEventSignal';

/** ADR-DCC-004: signs in with the backend-issued custom token (uid == CareBridge user id),
 * then listens at `/user-conversation-events/{uid}` — the caller's own inbox, enforced
 * server-side by RTDB Rules (`auth.uid === $uid`). */
export class ConversationSignalingPort {
  private unsubscribe: Unsubscribe | null = null;

  async connect(onEvent: (signal: ConversationEventSignal) => void): Promise<void> {
    if (!isFirebaseConfigured()) {
      // Degrade gracefully — chat delivery is never gated on Firebase (BR-DCC-007-adjacent).
      return;
    }
    const app = getFirebaseWebApp();
    const token = await fetchFirebaseCustomToken();
    const auth = getAuth(app);
    const credential = await signInWithCustomToken(auth, token);
    const uid = credential.user.uid;

    const db = getDatabase(app);
    const inboxRef = ref(db, `/user-conversation-events/${uid}`);
    this.unsubscribe = onChildAdded(inboxRef, (snapshot) => {
      const signal = parseConversationEventSignal(snapshot.val());
      if (signal) onEvent(signal);
    });
  }

  dispose(): void {
    this.unsubscribe?.();
    this.unsubscribe = null;
  }
}
