export interface SystemConfiguration {
  apiRateLimit: number;
  connectionTimeoutMs: number;
  maxUploadSizeMb: number;
  administratorEmail: string;
  emailAlerts: boolean;
  smsAlerts: boolean;
  webhookAlerts: boolean;
  aiModerationEnabled: boolean;
  maintenanceModeEnabled: boolean;
}

// MOCK — the backend does not expose a system-configuration API yet.
// Replace this adapter with apiClient calls when the approved contract exists.
const mockConfiguration: SystemConfiguration = {
  apiRateLimit: 5000,
  connectionTimeoutMs: 30000,
  maxUploadSizeMb: 25,
  administratorEmail: 'admin@carebridge.dev',
  emailAlerts: true,
  smsAlerts: true,
  webhookAlerts: false,
  aiModerationEnabled: true,
  maintenanceModeEnabled: false,
};

export async function fetchSystemConfiguration(): Promise<SystemConfiguration> {
  return Promise.resolve({ ...mockConfiguration });
}

export async function saveSystemConfiguration(configuration: SystemConfiguration): Promise<SystemConfiguration> {
  Object.assign(mockConfiguration, configuration);
  return Promise.resolve({ ...mockConfiguration });
}
