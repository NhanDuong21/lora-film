import { describe, expect, it } from 'vitest';
import {
  getAdminLandingPath,
  hasAdminAreaAccess,
  hasPermissionAccess
} from './permissionAccess';

describe('admin permission access', () => {
  it('recognizes built-in and root administrators', () => {
    expect(hasPermissionAccess('ROLE_ADMIN', [], 'CUSTOMER_UPDATE')).toBe(true);
    expect(hasPermissionAccess('CUSTOM_ROLE', ['PERM_ROOT_ACCESS'], 'ROLE_DELETE')).toBe(true);
  });

  it('requires a matching granular permission for a custom role', () => {
    expect(hasPermissionAccess('CUSTOM_ROLE', ['CUSTOMER_VIEW'], 'CUSTOMER_VIEW')).toBe(true);
    expect(hasPermissionAccess('CUSTOM_ROLE', ['CUSTOMER_VIEW'], 'EMPLOYEE_VIEW')).toBe(false);
    expect(hasAdminAreaAccess('CUSTOM_ROLE', ['CUSTOMER_VIEW'])).toBe(true);
  });

  it('selects a usable landing page for limited operational roles', () => {
    expect(getAdminLandingPath('CUSTOMER_MANAGER', ['CUSTOMER_VIEW'])).toBe('/admin/members');
    expect(getAdminLandingPath('ACCOUNTANT', ['PERM_VIEW_FINANCE'])).toBe('/admin/analytics');
    expect(getAdminLandingPath('ROLE_ADMIN', [])).toBe('/admin');
    expect(getAdminLandingPath('CUSTOM_ROLE', [])).toBe('/403');
  });
});
