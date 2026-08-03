import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ConversationSignalingPort } from './conversationSignalingPort';

const apiMocks = vi.hoisted(() => ({
  post: vi.fn(),
}));

const authMocks = vi.hoisted(() => ({
  state: {
    accessToken: 'access-token-1',
    user: { id: 'user-1' },
  } as { accessToken: string | null; user: { id: string } | null },
}));

const firebaseMocks = vi.hoisted(() => ({
  collection: vi.fn(),
  getAuth: vi.fn(),
  getFirebaseWebApp: vi.fn(),
  getFirestore: vi.fn(),
  isFirebaseConfigured: vi.fn(),
  limit: vi.fn(),
  onSnapshot: vi.fn(),
  orderBy: vi.fn(),
  query: vi.fn(),
  signInWithCustomToken: vi.fn(),
  signOut: vi.fn(),
}));

vi.mock('../../api/apiClient', () => ({
  default: { post: apiMocks.post },
}));

vi.mock('../../auth/authStore', () => ({
  useAuthStore: { getState: () => authMocks.state },
}));

vi.mock('./firebaseApp', () => ({
  getFirebaseWebApp: firebaseMocks.getFirebaseWebApp,
  isFirebaseConfigured: firebaseMocks.isFirebaseConfigured,
}));

vi.mock('firebase/auth', () => ({
  getAuth: firebaseMocks.getAuth,
  signInWithCustomToken: firebaseMocks.signInWithCustomToken,
  signOut: firebaseMocks.signOut,
}));

vi.mock('firebase/firestore', () => ({
  collection: firebaseMocks.collection,
  getFirestore: firebaseMocks.getFirestore,
  limit: firebaseMocks.limit,
  onSnapshot: firebaseMocks.onSnapshot,
  orderBy: firebaseMocks.orderBy,
  query: firebaseMocks.query,
}));

const capabilityEnvelope = (
  firestoreSignalingEnabled: boolean,
  firebaseCustomToken: unknown
) => ({
  data: {
    success: true,
    data: { firestoreSignalingEnabled, firebaseCustomToken },
    message: null,
    timestamp: '2026-08-03T00:00:00Z',
  },
});

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((complete) => {
    resolve = complete;
  });
  return { promise, resolve };
}

describe('ConversationSignalingPort', () => {
  let listenerErrors: Array<(error: unknown) => void>;
  let listenerUnsubscribes: Array<ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.resetAllMocks();
    listenerErrors = [];
    listenerUnsubscribes = [];
    authMocks.state = {
      accessToken: 'access-token-1',
      user: { id: 'user-1' },
    };

    firebaseMocks.isFirebaseConfigured.mockReturnValue(true);
    firebaseMocks.getFirebaseWebApp.mockReturnValue({ name: 'carebridge' });
    firebaseMocks.getAuth.mockReturnValue({ name: 'auth' });
    firebaseMocks.getFirestore.mockReturnValue({ name: 'firestore' });
    firebaseMocks.collection.mockReturnValue({ name: 'inbox' });
    firebaseMocks.orderBy.mockReturnValue({ name: 'order' });
    firebaseMocks.limit.mockReturnValue({ name: 'limit' });
    firebaseMocks.query.mockReturnValue({ name: 'query' });
    firebaseMocks.signInWithCustomToken.mockResolvedValue({ user: { uid: 'user-1' } });
    firebaseMocks.signOut.mockResolvedValue(undefined);
    firebaseMocks.onSnapshot.mockImplementation(
      (_snapshotQuery, _onNext, onError: (error: unknown) => void) => {
        const unsubscribe = vi.fn();
        listenerErrors.push(onError);
        listenerUnsubscribes.push(unsubscribe);
        return unsubscribe;
      }
    );
  });

  afterEach(() => {
    vi.clearAllTimers();
    vi.useRealTimers();
  });

  it('stops after one capability request when Firestore signaling is disabled', async () => {
    apiMocks.post.mockResolvedValue(capabilityEnvelope(false, null));
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    await vi.advanceTimersByTimeAsync(30_000);

    expect(apiMocks.post).toHaveBeenCalledOnce();
    expect(apiMocks.post).toHaveBeenCalledWith(
      '/api/v1/firebase/custom-token',
      undefined,
      expect.objectContaining({
        signal: expect.any(AbortSignal),
        carebridgeSession: { userId: 'user-1', accessToken: 'access-token-1' },
      })
    );
    expect(firebaseMocks.getFirebaseWebApp).not.toHaveBeenCalled();
    expect(firebaseMocks.signInWithCustomToken).not.toHaveBeenCalled();
    expect(firebaseMocks.onSnapshot).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('treats DCC-012 as terminal for the active session', async () => {
    apiMocks.post.mockRejectedValue({
      isAxiosError: true,
      response: { status: 503, data: { error: 'DCC-012' } },
    });
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    await vi.advanceTimersByTimeAsync(30_000);

    expect(apiMocks.post).toHaveBeenCalledOnce();
    expect(firebaseMocks.getFirebaseWebApp).not.toHaveBeenCalled();
    expect(firebaseMocks.signInWithCustomToken).not.toHaveBeenCalled();
    expect(firebaseMocks.onSnapshot).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('treats an enabled response without a nonblank token as terminal', async () => {
    apiMocks.post.mockResolvedValue(capabilityEnvelope(true, '   '));
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    await vi.advanceTimersByTimeAsync(30_000);

    expect(apiMocks.post).toHaveBeenCalledOnce();
    expect(firebaseMocks.getFirebaseWebApp).not.toHaveBeenCalled();
    expect(firebaseMocks.signInWithCustomToken).not.toHaveBeenCalled();
    expect(firebaseMocks.onSnapshot).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('preserves Firebase sign-in and the recipient-scoped listener when enabled', async () => {
    apiMocks.post.mockResolvedValue(capabilityEnvelope(true, ' firebase-token '));
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());

    expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledWith(
      { name: 'auth' },
      'firebase-token'
    );
    expect(firebaseMocks.collection).toHaveBeenCalledWith(
      { name: 'firestore' },
      'userConversationEvents',
      'user-1',
      'events'
    );
    expect(firebaseMocks.orderBy).toHaveBeenCalledWith('occurredAt', 'desc');
    expect(firebaseMocks.limit).toHaveBeenCalledWith(1);
    expect(firebaseMocks.onSnapshot).toHaveBeenCalledOnce();
  });

  it('treats a null API envelope as terminal instead of retrying', async () => {
    apiMocks.post.mockResolvedValue({ data: null });
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    await vi.advanceTimersByTimeAsync(30_000);

    expect(apiMocks.post).toHaveBeenCalledOnce();
    expect(firebaseMocks.getFirebaseWebApp).not.toHaveBeenCalled();
    expect(firebaseMocks.signInWithCustomToken).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('keeps the five-second retry for transient failures', async () => {
    apiMocks.post
      .mockRejectedValueOnce(new Error('network unavailable'))
      .mockResolvedValueOnce(capabilityEnvelope(false, null));
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    await vi.advanceTimersByTimeAsync(4_999);
    expect(apiMocks.post).toHaveBeenCalledOnce();

    await vi.advanceTimersByTimeAsync(1);
    expect(apiMocks.post).toHaveBeenCalledTimes(2);
    expect(vi.getTimerCount()).toBe(0);
  });

  it('ignores a capability response that completes after dispose', async () => {
    const pending = deferred<ReturnType<typeof capabilityEnvelope>>();
    apiMocks.post.mockReturnValue(pending.promise);
    const port = new ConversationSignalingPort();

    const connecting = port.connect(vi.fn());
    port.dispose();
    pending.resolve(capabilityEnvelope(true, 'late-token'));
    await connecting;
    await vi.advanceTimersByTimeAsync(30_000);

    expect(firebaseMocks.signInWithCustomToken).not.toHaveBeenCalled();
    expect(firebaseMocks.onSnapshot).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });

  it('does not apply a late token result from a replaced session', async () => {
    const oldRequest = deferred<ReturnType<typeof capabilityEnvelope>>();
    const newRequest = deferred<ReturnType<typeof capabilityEnvelope>>();
    apiMocks.post
      .mockReturnValueOnce(oldRequest.promise)
      .mockReturnValueOnce(newRequest.promise);
    const port = new ConversationSignalingPort();

    const oldConnect = port.connect(vi.fn());
    port.dispose();
    authMocks.state = {
      accessToken: 'access-token-2',
      user: { id: 'user-2' },
    };
    const newConnect = port.connect(vi.fn());
    newRequest.resolve(capabilityEnvelope(true, 'new-session-token'));
    await newConnect;
    oldRequest.resolve(capabilityEnvelope(true, 'old-session-token'));
    await oldConnect;

    expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledOnce();
    expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledWith(
      { name: 'auth' },
      'new-session-token'
    );
    expect(firebaseMocks.onSnapshot).toHaveBeenCalledOnce();
    expect(apiMocks.post.mock.calls[0]?.[2]).toEqual(expect.objectContaining({
      carebridgeSession: { userId: 'user-1', accessToken: 'access-token-1' },
    }));
    expect(apiMocks.post.mock.calls[1]?.[2]).toEqual(expect.objectContaining({
      carebridgeSession: { userId: 'user-2', accessToken: 'access-token-2' },
    }));
  });

  it('serializes a late old-session Firebase sign-in before the replacement sign-in', async () => {
    const oldSignIn = deferred<{ user: { uid: string } }>();
    const auth = { name: 'auth', currentUser: null as { uid: string } | null };
    firebaseMocks.getAuth.mockReturnValue(auth);
    apiMocks.post
      .mockResolvedValueOnce(capabilityEnvelope(true, 'old-session-token'))
      .mockResolvedValueOnce(capabilityEnvelope(true, 'new-session-token'));
    firebaseMocks.signInWithCustomToken
      .mockReturnValueOnce(oldSignIn.promise)
      .mockImplementationOnce(async () => {
        auth.currentUser = { uid: 'user-2' };
        return { user: { uid: 'user-2' } };
      });
    firebaseMocks.signOut.mockImplementation(async () => {
      auth.currentUser = null;
    });
    const port = new ConversationSignalingPort();

    const oldConnect = port.connect(vi.fn());
    await vi.waitFor(() => expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledOnce());
    port.dispose();
    authMocks.state = {
      accessToken: 'access-token-2',
      user: { id: 'user-2' },
    };
    const newConnect = port.connect(vi.fn());
    await vi.waitFor(() => expect(apiMocks.post).toHaveBeenCalledTimes(2));
    expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledOnce();

    auth.currentUser = { uid: 'user-1' };
    oldSignIn.resolve({ user: { uid: 'user-1' } });
    await Promise.all([oldConnect, newConnect]);

    expect(firebaseMocks.signOut).toHaveBeenCalledOnce();
    expect(firebaseMocks.signOut).toHaveBeenCalledWith(auth);
    expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledTimes(2);
    expect(firebaseMocks.signInWithCustomToken.mock.calls[1]?.[1]).toBe('new-session-token');
    expect(firebaseMocks.collection).toHaveBeenCalledOnce();
    expect(firebaseMocks.collection.mock.calls[0]?.[2]).toBe('user-2');
    expect(firebaseMocks.onSnapshot).toHaveBeenCalledOnce();
  });

  it('clears a stale Firebase sign-in that completes after dispose', async () => {
    const pendingSignIn = deferred<{ user: { uid: string } }>();
    const auth = { name: 'auth', currentUser: null as { uid: string } | null };
    firebaseMocks.getAuth.mockReturnValue(auth);
    apiMocks.post.mockResolvedValue(capabilityEnvelope(true, 'old-session-token'));
    firebaseMocks.signInWithCustomToken.mockReturnValue(pendingSignIn.promise);
    firebaseMocks.signOut.mockImplementation(async () => {
      auth.currentUser = null;
    });
    const port = new ConversationSignalingPort();

    const connecting = port.connect(vi.fn());
    await vi.waitFor(() => expect(firebaseMocks.signInWithCustomToken).toHaveBeenCalledOnce());
    port.dispose();
    auth.currentUser = { uid: 'user-1' };
    pendingSignIn.resolve({ user: { uid: 'user-1' } });
    await connecting;

    expect(firebaseMocks.signOut).toHaveBeenCalledWith(auth);
    expect(auth.currentUser).toBeNull();
    expect(firebaseMocks.onSnapshot).not.toHaveBeenCalled();
  });

  it('ignores a stale listener error after a replacement session starts', async () => {
    apiMocks.post
      .mockResolvedValueOnce(capabilityEnvelope(true, 'old-session-token'))
      .mockResolvedValueOnce(capabilityEnvelope(true, 'new-session-token'));
    const port = new ConversationSignalingPort();

    await port.connect(vi.fn());
    port.dispose();
    await port.connect(vi.fn());
    expect(listenerErrors).toHaveLength(2);
    expect(listenerUnsubscribes[0]).toHaveBeenCalledOnce();

    listenerErrors[0](new Error('stale listener failure'));
    await vi.advanceTimersByTimeAsync(5_000);

    expect(apiMocks.post).toHaveBeenCalledTimes(2);
    expect(listenerUnsubscribes[1]).not.toHaveBeenCalled();
    expect(vi.getTimerCount()).toBe(0);
  });
});
