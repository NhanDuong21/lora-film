export const promotionStackingState = (promotion = {}, campaign) => {
  const configured = Boolean(promotion.stackable);
  const campaignAllows =
    typeof promotion.campaignStackable === "boolean"
      ? promotion.campaignStackable
      : Boolean(campaign?.stackable);
  const effective =
    typeof promotion.effectiveStackable === "boolean"
      ? promotion.effectiveStackable
      : configured && campaignAllows;
  const blockedReason =
    promotion.stackingBlockedReason ||
    (!configured
      ? "PROMOTION_STACKING_DISABLED"
      : !campaignAllows
        ? "CAMPAIGN_STACKING_DISABLED"
        : null);

  return { configured, campaignAllows, effective, blockedReason };
};
