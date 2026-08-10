import { describe, expect, it } from "vitest";
import { promotionStackingState } from "./promotionStackingState";

describe("promotionStackingState", () => {
  it("shows a configured promotion as blocked by its campaign", () => {
    expect(
      promotionStackingState(
        {
          stackable: true,
          campaignStackable: false,
          effectiveStackable: false,
          stackingBlockedReason: "CAMPAIGN_STACKING_DISABLED",
        },
        { stackable: true },
      ),
    ).toEqual({
      configured: true,
      campaignAllows: false,
      effective: false,
      blockedReason: "CAMPAIGN_STACKING_DISABLED",
    });
  });

  it("falls back to campaign data for older promotion responses", () => {
    expect(
      promotionStackingState({ stackable: true }, { stackable: false }),
    ).toEqual({
      configured: true,
      campaignAllows: false,
      effective: false,
      blockedReason: "CAMPAIGN_STACKING_DISABLED",
    });
  });

  it("reports effective stacking only when both levels allow it", () => {
    expect(
      promotionStackingState(
        { stackable: true, campaignStackable: true, effectiveStackable: true },
        { stackable: true },
      ),
    ).toMatchObject({
      configured: true,
      campaignAllows: true,
      effective: true,
      blockedReason: null,
    });
  });
});
