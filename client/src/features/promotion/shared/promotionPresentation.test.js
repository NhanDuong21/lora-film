import { describe, expect, it } from 'vitest';
import {
  CREATABLE_CAMPAIGN_TYPES,
  friendlyPromotionError,
} from './promotionPresentation';

describe('promotionPresentation', () => {
  it('offers only campaign types supported by the checkout runtime', () => {
    expect(CREATABLE_CAMPAIGN_TYPES).toEqual(['COUPON', 'VOUCHER']);
    expect(CREATABLE_CAMPAIGN_TYPES).not.toContain('AUTOMATIC_DISCOUNT');
  });

  it('explains the unsupported automatic discount response in business language', () => {
    const error = {
      response: {
        data: {
          message: 'AUTOMATIC_DISCOUNT is not supported by the current checkout runtime',
        },
      },
    };

    expect(friendlyPromotionError(error)).toBe(
      'Checkout hiện chưa hỗ trợ chiến dịch giảm giá tự động. Hãy dùng chiến dịch Coupon hoặc Voucher.'
    );
  });
});
