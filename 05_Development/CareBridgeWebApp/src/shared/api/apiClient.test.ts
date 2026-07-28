// @vitest-environment jsdom

import { describe, expect, it } from 'vitest';
import { shouldRedirectToMaintenance } from './apiClient';

describe('maintenance response routing', () => {
  it('redirects normal API calls for the maintenance error', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/community/questions',
    )).toBe(true);
  });

  it('keeps the system configuration recovery endpoint accessible', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/admin/system-configuration',
    )).toBe(false);
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      'http://localhost:8080/api/v1/admin/system-configuration/?refresh=true',
    )).toBe(false);
  });

  it('does not exempt near matches or query-string mentions of the recovery endpoint', () => {
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/admin/system-configuration-export',
    )).toBe(true);
    expect(shouldRedirectToMaintenance(
      503,
      'SYSTEM_MAINTENANCE',
      '/api/v1/community/questions?next=/api/v1/admin/system-configuration',
    )).toBe(true);
  });

  it('does not redirect unrelated failures', () => {
    expect(shouldRedirectToMaintenance(503, 'UPSTREAM_UNAVAILABLE', '/api/v1/community/questions')).toBe(false);
    expect(shouldRedirectToMaintenance(500, 'SYSTEM_MAINTENANCE', '/api/v1/community/questions')).toBe(false);
  });
});
