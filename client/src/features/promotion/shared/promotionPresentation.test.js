import { describe, expect, it } from 'vitest';
import {
  ACTION_TYPES,
  friendlyPromotionError,
  PROMOTION_TYPES,
} from './promotionPresentation';

describe('promotionPresentation', () => {
  it('exposes the unified promotion and action types', () => {
    expect(PROMOTION_TYPES).toEqual(['AUTO', 'VOUCHER', 'COUPON']);
    expect(ACTION_TYPES).toContain('FULL_DISCOUNT');
  });

  it('explains an unavailable coupon in business language', () => {
    const error = {
      response: {
        data: {
          message: 'Coupon is invalid or unavailable',
        },
      },
    };

    expect(friendlyPromotionError(error)).toBe('Coupon không hợp lệ hoặc đã hết hiệu lực');
  });
});
