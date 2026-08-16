import { describe, expect, it } from 'vitest';
import { resolvePostLoginPath } from './loginRedirect';

describe('resolvePostLoginPath', () => {
  it('returns a customer to the protected booking URL they requested', () => {
    expect(resolvePostLoginPath({
      role: 'CUSTOMER',
      from: { pathname: '/bookings/success', search: '?bookingId=booking-1' },
    })).toBe('/bookings/success?bookingId=booking-1');
  });

  it('does not send a customer back into an employee route', () => {
    expect(resolvePostLoginPath({
      role: 'CUSTOMER',
      from: { pathname: '/employee/payments/cash' },
    })).toBe('/');
  });

  it('keeps EMPLOYEE on the permission-aware employee entry point', () => {
    expect(resolvePostLoginPath({ role: 'ROLE_EMPLOYEE' })).toBe('/employee');
  });

  it('keeps box-office employees out of accounting when PAYMENT_VIEW is shared', () => {
    expect(resolvePostLoginPath({
      role: 'ROLE_EMPLOYEE',
      permissions: [
        'BOOKING_MANAGE',
        'BOOKING_VIEW',
        'MOVIE_VIEW',
        'PAYMENT_CASH_COLLECT',
        'PAYMENT_VIEW',
        'USER_VIEW',
      ],
    })).toBe('/employee');
  });

  it('opens the accounting workspace for an EMPLOYEE with accounting permissions', () => {
    expect(resolvePostLoginPath({
      role: 'ROLE_EMPLOYEE',
      permissions: ['PAYMENT_VIEW', 'PAYMENT_RECONCILE', 'ANALYTICS_VIEW', 'PAYROLL_VIEW'],
    })).toBe('/admin/accounting/operations');
  });

  it('opens the independent control workspace for an accounting controller', () => {
    expect(resolvePostLoginPath({
      role: 'ROLE_EMPLOYEE',
      permissions: [
        'PAYMENT_VIEW',
        'PAYMENT_RECONCILE',
        'PAYROLL_VIEW',
        'PAYROLL_APPROVE',
        'REFUND_APPROVE',
        'SETTLEMENT_LOCK',
        'ACCOUNTING_PERIOD_CLOSE',
      ],
    })).toBe('/admin/accounting/control');
  });

  it('keeps MANAGER inside the assigned-cinema workspace', () => {
    expect(resolvePostLoginPath({
      role: 'ROLE_MANAGER',
      permissions: ['ANALYTICS_VIEW', 'PAYMENT_VIEW', 'SHOWTIME_MANAGE'],
    })).toBe('/manager');
  });

  it('does not treat the retired STAFF role as an employee identity', () => {
    expect(resolvePostLoginPath({ role: 'ROLE_STAFF' })).toBe('/');
  });

  it('keeps ADMIN on the admin landing page', () => {
    expect(resolvePostLoginPath({ role: 'ADMIN' })).toBe('/admin');
  });
});
