export type BlockedAccountCode =
  | 'ACCOUNT_TEMPORARILY_LOCKED'
  | 'ACCOUNT_ADMIN_LOCKED'
  | 'ACCOUNT_DISABLED'
  | 'ACCOUNT_SUSPENDED';

export interface BlockedAccountState {
  code: BlockedAccountCode;
  message?: string;
  lockType?: 'TEMPORARY' | 'ADMIN';
  reason?: string;
  retryAt?: string;
  appealAllowed?: boolean;
  appealToken?: string;
  appealPending?: boolean;
  appealStatus?: 'PENDING' | 'APPROVED' | 'REJECTED';
}

const STORAGE_KEY = 'carebridge.blocked-account';

export function saveBlockedAccountState(state: BlockedAccountState) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

export function loadBlockedAccountState(): BlockedAccountState | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw) as BlockedAccountState; } catch { return null; }
}

export function clearBlockedAccountState() {
  sessionStorage.removeItem(STORAGE_KEY);
}

export function parseBlockedAccountError(error: unknown): BlockedAccountState | null {
  const data = (error as { response?: { data?: Record<string, unknown> } }).response?.data;
  const code = data?.error as BlockedAccountCode | undefined;
  if (!code || !['ACCOUNT_TEMPORARILY_LOCKED', 'ACCOUNT_ADMIN_LOCKED', 'ACCOUNT_DISABLED', 'ACCOUNT_SUSPENDED'].includes(code)) return null;
  const metadata = (data?.metadata ?? {}) as Partial<BlockedAccountState>;
  return { code, message: data?.message as string | undefined, ...metadata };
}
