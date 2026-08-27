/**
 * Customer-support contact shown to users who cannot resolve an account problem
 * in the app themselves.
 *
 * Since the in-app account-lock appeal workflow was retired, a locked user's only
 * route back is customer support, so this contact has to be present and correct
 * in every deployed environment. Override it per environment with
 * VITE_SUPPORT_EMAIL / VITE_SUPPORT_PHONE.
 */
export const SUPPORT_EMAIL: string =
  import.meta.env.VITE_SUPPORT_EMAIL ?? 'support@carebridge.dev';

export const SUPPORT_PHONE: string | undefined = import.meta.env.VITE_SUPPORT_PHONE;
