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

  it('keeps STAFF on the supported cash workflow entry point', () => {
    expect(resolvePostLoginPath({ role: 'ROLE_STAFF' })).toBe('/employee');
  });

  it('keeps ADMIN on the admin landing page', () => {
    expect(resolvePostLoginPath({ role: 'ADMIN' })).toBe('/admin');
  });
});
