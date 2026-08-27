/// Customer-support contact shown when a user cannot resolve an account problem
/// in the app.
///
/// The in-app account-lock appeal workflow was retired, so this is the only
/// route back for a locked account. Override per build with
/// `--dart-define=SUPPORT_EMAIL=...` / `--dart-define=SUPPORT_PHONE=...`.
const supportEmail = String.fromEnvironment(
  'SUPPORT_EMAIL',
  defaultValue: 'support@carebridge.dev',
);

const supportPhone = String.fromEnvironment('SUPPORT_PHONE');
