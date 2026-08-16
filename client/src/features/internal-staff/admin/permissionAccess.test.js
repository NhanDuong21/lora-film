import { describe, expect, it } from 'vitest';
import {
  getAccountingLandingPath,
  getAccountingRoleLabel,
  getAccountingWorkspaceMode,
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
    expect(hasAdminAreaAccess('EMPLOYEE', ['PAYMENT_VIEW'])).toBe(false);
  });

  it('selects a usable landing page for limited operational roles', () => {
    expect(getAdminLandingPath('CUSTOMER_MANAGER', ['CUSTOMER_VIEW'])).toBe('/admin/members');
    expect(getAdminLandingPath('HR_OPERATOR', ['EMPLOYEE_VIEW'])).toBe('/admin/hr');
    expect(getAdminLandingPath('ACCOUNTANT', ['PERM_VIEW_FINANCE']))
      .toBe('/admin/accounting/operations');
    expect(getAdminLandingPath('EMPLOYEE', ['PAYMENT_RECONCILE', 'PAYROLL_VIEW']))
      .toBe('/admin/accounting/operations');
    expect(getAdminLandingPath('EMPLOYEE', ['PAYMENT_VIEW'])).toBe('/403');
    expect(getAdminLandingPath('ROLE_ADMIN', [])).toBe('/admin');
    expect(getAdminLandingPath('CUSTOM_ROLE', [])).toBe('/403');
  });

  it('routes operational and control accountants to distinct workspaces', () => {
    const operations = ['SETTLEMENT_IMPORT', 'REFUND_REQUEST', 'PAYROLL_SUBMIT_PAYMENT'];
    const control = ['SETTLEMENT_LOCK', 'REFUND_APPROVE', 'PAYROLL_APPROVE'];

    expect(getAccountingWorkspaceMode(operations)).toBe('operations');
    expect(getAccountingLandingPath(operations)).toBe('/admin/accounting/operations');
    expect(getAccountingRoleLabel(operations)).toBe('Kế toán vận hành');
    expect(getAccountingWorkspaceMode(control)).toBe('control');
    expect(getAccountingLandingPath(control)).toBe('/admin/accounting/control');
    expect(getAccountingRoleLabel(control)).toBe('Kế toán kiểm soát');
    expect(getAdminLandingPath('EMPLOYEE', control)).toBe('/admin/accounting/control');
  });

  it('lands system operators on the consolidated operational screens', () => {
    expect(getAdminLandingPath('ACCESS_OPERATOR', ['ROLE_VIEW'])).toBe('/admin/accounts?tab=access');
    expect(getAdminLandingPath('ACCESS_OPERATOR', ['PERMISSION_VIEW'])).toBe('/admin/accounts?tab=access');
    expect(getAdminLandingPath('AUDITOR', ['USER_AUDIT_VIEW'])).toBe('/admin/audits?tab=operations');
  });
});
