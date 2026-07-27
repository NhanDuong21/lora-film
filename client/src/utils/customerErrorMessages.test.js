import { describe, expect, it } from 'vitest';
import {
  getCustomerErrorCode,
  getCustomerErrorMessage
} from './customerErrorMessages';

describe('customer error messages', () => {
  it('reads codes from plain and Axios-shaped errors', () => {
    expect(getCustomerErrorCode({ errorCode: 'PLAIN_CODE' })).toBe('PLAIN_CODE');
    expect(getCustomerErrorCode({
      response: { data: { code: 'AXIOS_CODE' } }
    })).toBe('AXIOS_CODE');
  });

  it('never exposes an unknown raw English service message', () => {
    expect(getCustomerErrorMessage(
      { message: 'Unexpected upstream response' },
      'Không thể hoàn thành thao tác.'
    )).toBe('Không thể hoàn thành thao tác.');
  });

  it('preserves a Vietnamese service message and translates network errors', () => {
    expect(getCustomerErrorMessage({ message: 'Đơn đã hết hạn.' }))
      .toBe('Đơn đã hết hạn.');
    expect(getCustomerErrorMessage({ code: 'ERR_NETWORK' }))
      .toContain('Không thể kết nối');
  });
});
