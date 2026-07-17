import { ConversationSignalingPort } from './conversationSignalingPort';
import type { ConversationEventSignal } from './conversationEventSignal';

type SignalListener = (signal: ConversationEventSignal) => void;

class ConversationSignalHub {
  private readonly port = new ConversationSignalingPort();
  private readonly listeners = new Set<SignalListener>();
  private started = false;

  subscribe(listener: SignalListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  async start(): Promise<void> {
    if (this.started) return;
    this.started = true;
    await this.port.connect((signal) => {
      for (const listener of this.listeners) listener(signal);
    });
  }

  stop(): void {
    if (!this.started) return;
    this.started = false;
    this.port.dispose();
  }
}

export const conversationSignalHub = new ConversationSignalHub();
