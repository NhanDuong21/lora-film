import { beforeEach, describe, expect, it } from 'vitest';
import { consumeAuthReturn, rememberAuthReturn } from './authReturn';

describe('auth return context', () => {
  beforeEach(() => sessionStorage.clear());

  it('round-trips a booking destination without storing form data', () => {
    rememberAuthReturn({
      pathname: '/seat-selection',
      search: '?showtime=abc',
      hash: '#seats',
    });

    expect(consumeAuthReturn()).toEqual({
      pathname: '/seat-selection',
      search: '?showtime=abc',
      hash: '#seats',
    });
    expect(consumeAuthReturn()).toBeUndefined();
  });

  it('rejects external and recursive auth destinations', () => {
    rememberAuthReturn({ pathname: '//evil.example' });
    expect(consumeAuthReturn()).toBeUndefined();

    rememberAuthReturn({ pathname: '/login' });
    expect(consumeAuthReturn()).toBeUndefined();
  });
});
