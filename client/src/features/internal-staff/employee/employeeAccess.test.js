import { describe, expect, it } from 'vitest';
import {
  EMPLOYEE_PERMISSIONS,
  getEmployeeLandingPath,
  hasEmployeeAccess,
} from './employeeAccess';

describe('employee permission access', () => {
  it('requires the canonical EMPLOYEE role and a matching permission', () => {
    expect(hasEmployeeAccess(
      'EMPLOYEE',
      [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW],
      [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW],
    )).toBe(true);
    expect(hasEmployeeAccess(
      'STAFF',
      [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW],
      [EMPLOYEE_PERMISSIONS.SCHEDULE_VIEW],
    )).toBe(false);
  });

  it('supports all-permission requirements for the check-in workflow', () => {
    const required = [
      EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW,
      EMPLOYEE_PERMISSIONS.ATTENDANCE_UPDATE,
    ];
    expect(hasEmployeeAccess(
      'EMPLOYEE',
      [EMPLOYEE_PERMISSIONS.ATTENDANCE_VIEW],
      required,
      true,
    )).toBe(false);
    expect(hasEmployeeAccess('EMPLOYEE', required, required, true)).toBe(true);
  });

  it('chooses the first landing page the employee can access', () => {
    expect(getEmployeeLandingPath('EMPLOYEE', [EMPLOYEE_PERMISSIONS.DASHBOARD_VIEW]))
      .toBe('/employee/dashboard');
    expect(getEmployeeLandingPath('EMPLOYEE', [EMPLOYEE_PERMISSIONS.CASH_PAYMENT_COLLECT]))
      .toBe('/employee/payments/cash');
    expect(getEmployeeLandingPath('EMPLOYEE', [])).toBe('/403');
  });

  it('keeps the administrator override', () => {
    expect(getEmployeeLandingPath('ADMIN', [])).toBe('/employee/dashboard');
    expect(hasEmployeeAccess('ADMIN', [], [EMPLOYEE_PERMISSIONS.PAYROLL_VIEW])).toBe(true);
  });
});
