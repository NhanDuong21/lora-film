import { describe, expect, it } from "vitest";
import {
  ACTION_TYPES,
  conditionSummary,
  friendlyPromotionError,
  promotionModelFor,
  PROMOTION_TYPES,
} from "./promotionPresentation";

describe("promotionPresentation", () => {
  it("exposes the unified promotion and action types", () => {
    expect(PROMOTION_TYPES).toEqual(["AUTO", "VOUCHER", "COUPON"]);
    expect(ACTION_TYPES).toContain("FULL_DISCOUNT");
  });

  it("maps backend types to the three Promotion Center models", () => {
    expect(promotionModelFor("AUTO").key).toBe("system");
    expect(promotionModelFor("VOUCHER").key).toBe("event");
    expect(promotionModelFor("COUPON").key).toBe("coupon");
  });

  it("explains an unavailable coupon in business language", () => {
    const error = {
      response: {
        data: {
          message: "Coupon is invalid or unavailable",
        },
      },
    };

    expect(friendlyPromotionError(error)).toBe(
      "Coupon không hợp lệ hoặc đã hết hiệu lực",
    );
  });

  it("summarizes public movie and cinema constraints", () => {
    const summary = conditionSummary({
      moviePublicIds: ["movie-1"],
      cinemaPublicIds: ["cinema-1"],
    });

    expect(summary).toContain("Giới hạn theo phim");
    expect(summary).toContain("Giới hạn theo rạp");
  });
});
