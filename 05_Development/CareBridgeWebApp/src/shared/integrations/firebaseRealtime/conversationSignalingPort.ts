import { getAuth, signInWithCustomToken } from 'firebase/auth';
import { collection, getFirestore, limit, onSnapshot, orderBy, query, type Unsubscribe } from 'firebase/firestore';
import { fetchFirebaseCustomToken } from '../../../features/directChat/services/firebaseTokenApi';
import { getFirebaseWebApp, isFirebaseConfigured } from './firebaseApp';
import { parseConversationEventSignal, type ConversationEventSignal } from './conversationEventSignal';

/** Signs in with the backend-issued token, then listens to the caller's Firestore inbox. */
export class ConversationSignalingPort {
  private unsubscribe: Unsubscribe | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private disposed = false;
  private onEvent: ((signal: ConversationEventSignal) => void) | null = null;

  async connect(onEvent: (signal: ConversationEventSignal) => void): Promise<void> {
    this.disposed = false;
    this.onEvent = onEvent;
    if (!isFirebaseConfigured()) {
      // Degrade gracefully — chat delivery is never gated on Firebase (BR-DCC-007-adjacent).
      return;
    }
    try {
      await this.establishListener();
    } catch {
      this.scheduleReconnect();
    }
  }

  private async establishListener(): Promise<void> {
    if (this.disposed || !this.onEvent) return;
    const app = getFirebaseWebApp();
    const token = await fetchFirebaseCustomToken();
    if (this.disposed) return;
    const credential = await signInWithCustomToken(getAuth(app), token);
    if (this.disposed) return;
    const inbox = collection(getFirestore(app), 'userConversationEvents', credential.user.uid, 'events');
    const latestSignal = query(inbox, orderBy('occurredAt', 'desc'), limit(1));
    this.unsubscribe = onSnapshot(
      latestSignal,
      (snapshot) => {
        for (const change of snapshot.docChanges()) {
          if (change.type !== 'added') continue;
          const signal = parseConversationEventSignal(change.doc.data());
          if (signal) this.onEvent?.(signal);
        }
      },
      () => this.scheduleReconnect()
    );
  }

  private scheduleReconnect(): void {
    this.unsubscribe?.();
    this.unsubscribe = null;
    if (this.disposed || this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      void this.establishListener().catch(() => this.scheduleReconnect());
    }, 5000);
  }

  dispose(): void {
    this.disposed = true;
    this.unsubscribe?.();
    this.unsubscribe = null;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    this.onEvent = null;
  }
}
