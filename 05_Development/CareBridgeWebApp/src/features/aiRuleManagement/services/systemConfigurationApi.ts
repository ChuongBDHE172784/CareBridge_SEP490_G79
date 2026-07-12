import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';

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

export async function fetchSystemConfiguration(): Promise<SystemConfiguration> {
  const response = await apiClient.get<ApiResponse<SystemConfiguration>>('/api/v1/admin/system-configuration');
  return response.data.data;
}

export async function saveSystemConfiguration(configuration: SystemConfiguration): Promise<SystemConfiguration> {
  const response = await apiClient.put<ApiResponse<SystemConfiguration>>(
    '/api/v1/admin/system-configuration',
    configuration,
  );
  return response.data.data;
}
