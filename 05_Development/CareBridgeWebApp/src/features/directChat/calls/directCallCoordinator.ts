import type { ConversationEventSignal } from '../../../shared/integrations/firebaseRealtime/conversationEventSignal';
import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import type { DirectCallApiPort, DirectCallSignalSource } from './directCallApi';
import {
  idleDirectCallState,
  reduceDirectCallState,
  type DirectCallPhase,
  type DirectCallState,
} from './directCallState';

type StateListener = (state: DirectCallState) => void;

export class DirectCallCoordinator {
  private readonly api: DirectCallApiPort;
  private readonly signals: DirectCallSignalSource;
  private readonly currentUserId: () => string | null | undefined;
  private readonly seenEventIds = new Set<string>();
  private readonly pendingCalls = new Map<string, string>();
  private readonly listeners = new Set<StateListener>();
  private unsubscribeSignal: (() => void) | null = null;
  private syncing = false;
  private pendingSync = false;
  private disposed = false;
  private generation = 0;
  private currentState = idleDirectCallState;

  constructor(
    api: DirectCallApiPort,
    signals: DirectCallSignalSource,
    currentUserId: () => string | null | undefined
  ) {
    this.api = api;
    this.signals = signals;
    this.currentUserId = currentUserId;
  }

  get state(): DirectCallState {
    return this.currentState;
  }

  subscribe(listener: StateListener): () => void {
    this.listeners.add(listener);
    listener(this.currentState);
    return () => this.listeners.delete(listener);
  }

  async start(): Promise<void> {
    if (this.disposed || this.unsubscribeSignal) return;
    this.unsubscribeSignal = this.signals.subscribe((signal) => this.onSignal(signal));
    await this.reconcileActiveCalls();
  }

  async reconcileActiveCalls(): Promise<void> {
    if (this.disposed) return;
    if (this.syncing) {
      this.pendingSync = true;
      return;
    }
    this.syncing = true;
    const generation = ++this.generation;
    try {
      const calls = await this.api.listActiveCalls();
      if (this.disposed || generation !== this.generation) return;
      for (const call of calls) await this.apply(call);
      const pending = [...this.pendingCalls.entries()];
      this.pendingCalls.clear();
      for (const [callId, conversationId] of pending) {
        if (!calls.some((call) => call.callId === callId)) {
          await this.fetchDetail(conversationId, callId);
        }
      }
    } catch {
      // Firestore and REST are both recovery nudges; visibility/auth changes retry.
    } finally {
      this.syncing = false;
      if (this.pendingSync && !this.disposed) {
        this.pendingSync = false;
        queueMicrotask(() => void this.reconcileActiveCalls());
      }
    }
  }

  private onSignal(signal: ConversationEventSignal): void {
    if (
      this.disposed ||
      !signal.eventType.startsWith('CALL_') ||
      !this.seenEventIds.add(signal.eventId)
    ) {
      return;
    }
    if (this.syncing) {
      this.pendingCalls.set(signal.resourceId, signal.conversationId);
      return;
    }
    void this.fetchDetail(signal.conversationId, signal.resourceId);
  }

  private async fetchDetail(conversationId: string, callId: string): Promise<void> {
    try {
      await this.apply(await this.api.getCall(conversationId, callId));
    } catch {
      // A delayed/replayed nudge may target a state already reconciled elsewhere.
    }
  }

  private async apply(call: ConversationCall): Promise<void> {
    const userId = this.currentUserId();
    if (this.disposed || !userId) return;
    let authoritative = call;
    if (call.callStatus === 'INITIATED' && call.initiatedByUserId !== userId) {
      try {
        authoritative = await this.api.markRinging(call.conversationId, call.callId);
      } catch {
        try {
          authoritative = await this.api.getCall(call.conversationId, call.callId);
        } catch {
          return;
        }
      }
    }
    this.emit(reduceDirectCallState(userId, authoritative, this.currentState));
  }

  async initiate(conversationId: string, callType: 'VOICE' | 'VIDEO'): Promise<void> {
    await this.apply(await this.api.initiate(conversationId, callType));
  }

  async answerCurrent(): Promise<ConversationCall | null> {
    const call = this.currentState.call;
    if (!call) return null;
    this.emit({ phase: 'answering', call });
    try {
      const answered = await this.api.answer(call.conversationId, call.callId);
      await this.apply(answered);
      return answered;
    } catch (error) {
      await this.fetchDetail(call.conversationId, call.callId);
      throw error;
    }
  }

  async declineCurrent(): Promise<void> {
    const call = this.currentState.call;
    if (call) await this.apply(await this.api.decline(call.conversationId, call.callId));
  }

  async endCurrent(): Promise<void> {
    const call = this.currentState.call;
    if (!call) return;
    try {
      await this.apply(await this.api.end(call.conversationId, call.callId));
    } catch {
      await this.fetchDetail(call.conversationId, call.callId);
    }
  }

  async issueJoinCredentialsForCurrent(): Promise<ZegoJoinCredentials | null> {
    const call = this.currentState.call;
    if (!call || call.callStatus !== 'ANSWERED') {
      throw new Error('Call is not ready to join');
    }
    const credentials = await this.api.issueJoinCredentials(call.conversationId, call.callId);
    if (
      this.disposed ||
      this.currentState.call?.callId !== call.callId ||
      this.currentState.call.callStatus !== 'ANSWERED'
    ) {
      return null;
    }
    return credentials;
  }

  setPhase(phase: Extract<DirectCallPhase, 'joining' | 'inCall' | 'reconnecting'>): void {
    if (this.currentState.call) this.emit({ phase, call: this.currentState.call });
  }

  fail(message: string): void {
    this.emit({ phase: 'failed', call: this.currentState.call, message });
  }

  private emit(state: DirectCallState): void {
    if (this.disposed) return;
    this.currentState = state;
    for (const listener of this.listeners) listener(state);
  }

  dispose(): void {
    if (this.disposed) return;
    this.disposed = true;
    this.generation += 1;
    this.unsubscribeSignal?.();
    this.unsubscribeSignal = null;
    this.listeners.clear();
  }
}
