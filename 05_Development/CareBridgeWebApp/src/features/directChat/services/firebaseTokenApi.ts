import axios from 'axios';
import apiClient, {
  type ApiRequestSessionIdentity,
  type ApiSessionBoundRequestConfig,
} from '../../../shared/api/apiClient';
import { useAuthStore } from '../../../shared/auth/authStore';
import type { ApiResponse } from '../../auth/models/user';

export interface FirebaseSignalingCapability {
  firestoreSignalingEnabled: boolean;
  firebaseCustomToken: string | null;
}

interface BackendErrorResponse {
  error?: string;
}

export class FirebaseSignalingProtocolError extends Error {
  constructor(message = 'Invalid Firebase signaling capability response') {
    super(message);
    this.name = 'FirebaseSignalingProtocolError';
  }
}

function captureSession(): ApiRequestSessionIdentity {
  const auth = useAuthStore.getState();
  if (!auth.user?.id || !auth.accessToken) {
    throw new FirebaseSignalingProtocolError('Firebase signaling requires an active account session');
  }
  return { userId: auth.user.id, accessToken: auth.accessToken };
}

function isSessionCurrent(session: ApiRequestSessionIdentity): boolean {
  const auth = useAuthStore.getState();
  return auth.user?.id === session.userId && auth.accessToken === session.accessToken;
}

function parseCapability(value: unknown): FirebaseSignalingCapability {
  if (typeof value !== 'object' || value === null) {
    throw new FirebaseSignalingProtocolError();
  }

  const capability = value as Record<string, unknown>;
  if (typeof capability.firestoreSignalingEnabled !== 'boolean') {
    throw new FirebaseSignalingProtocolError();
  }

  const token = capability.firebaseCustomToken;
  if (capability.firestoreSignalingEnabled) {
    if (typeof token !== 'string' || token.trim().length === 0) {
      throw new FirebaseSignalingProtocolError();
    }
    return {
      firestoreSignalingEnabled: true,
      firebaseCustomToken: token.trim(),
    };
  }

  if (token !== null) {
    throw new FirebaseSignalingProtocolError();
  }
  return {
    firestoreSignalingEnabled: false,
    firebaseCustomToken: null,
  };
}

// BR-DCC-013: server derives the uid strictly from the caller's own JWT — this client
// never sends a target user id, there is no field to send one in.
export async function fetchFirebaseCustomToken(
  signal?: AbortSignal,
): Promise<FirebaseSignalingCapability> {
  const session = captureSession();
  const config: ApiSessionBoundRequestConfig = {
    signal,
    carebridgeSession: session,
  };
  const { data } = await apiClient.post<ApiResponse<unknown>>(
    '/api/v1/firebase/custom-token',
    undefined,
    config,
  );
  if (!isSessionCurrent(session)) {
    throw new FirebaseSignalingProtocolError('Firebase signaling session changed during negotiation');
  }
  if (typeof data !== 'object' || data === null || data.success !== true) {
    throw new FirebaseSignalingProtocolError();
  }
  return parseCapability(data.data);
}

export function isTerminalFirebaseSignalingError(error: unknown): boolean {
  return error instanceof FirebaseSignalingProtocolError
    || (axios.isAxiosError<BackendErrorResponse>(error)
      && error.response?.data?.error === 'DCC-012');
}
