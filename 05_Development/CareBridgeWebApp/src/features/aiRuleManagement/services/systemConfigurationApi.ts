import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';

export interface SystemConfiguration {
  id: string;
  aiModerationEnabled: boolean;
  maintenanceModeEnabled: boolean;
  rowVersion: number;
  updatedBy: string;
  updatedAt: string;
}

export interface UpdateSystemConfigurationRequest {
  aiModerationEnabled: boolean;
  maintenanceModeEnabled: boolean;
  rowVersion: number;
}

export async function fetchSystemConfiguration(): Promise<SystemConfiguration> {
  const response = await apiClient.get<ApiResponse<SystemConfiguration>>('/api/v1/admin/system-configuration');
  return response.data.data;
}

export async function saveSystemConfiguration(configuration: UpdateSystemConfigurationRequest): Promise<SystemConfiguration> {
  const response = await apiClient.put<ApiResponse<SystemConfiguration>>(
    '/api/v1/admin/system-configuration',
    configuration,
  );
  return response.data.data;
}
