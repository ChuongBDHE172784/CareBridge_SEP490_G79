import type { RegistrationDraft } from '../models/auth';

// Registration credentials deliberately live only in this module's memory.
// They are never written to browser history, localStorage, sessionStorage, or
// the URL. A refresh therefore restarts registration instead of retaining a
// plaintext password in a durable browser surface.
let activeDraft: RegistrationDraft | undefined;

export function setRegistrationDraft(draft: RegistrationDraft): void {
  activeDraft = { ...draft };
}

export function getRegistrationDraft(): RegistrationDraft | undefined {
  return activeDraft ? { ...activeDraft } : undefined;
}

export function clearRegistrationDraft(): void {
  activeDraft = undefined;
}
