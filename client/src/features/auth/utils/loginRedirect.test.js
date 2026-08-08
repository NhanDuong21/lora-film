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

  it('does not treat the retired STAFF role as an employee identity', () => {
    expect(resolvePostLoginPath({ role: 'ROLE_STAFF' })).toBe('/');
  });

  it('keeps ADMIN on the admin landing page', () => {
    expect(resolvePostLoginPath({ role: 'ADMIN' })).toBe('/admin');
  });
});
