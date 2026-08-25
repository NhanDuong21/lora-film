import apiClient from "@/services/apiClient";

const unwrap = (response) => response?.data?.data ?? response?.data ?? response;

const flattenWalletItem = (item) => ({
  ...(item?.promotion || {}),
  ...item,
  promotionPublicId: item?.promotion?.publicId || item?.promotionPublicId,
  walletPublicId: item?.publicId,
  publicId: item?.publicId,
  source: "CUSTOMER_WALLET",
  ownershipType: "OWNED",
  selectionPublicId: item?.publicId,
});

const normalizePublicItem = (item) => ({
  ...item,
  promotionPublicId: item?.publicId,
  walletPublicId: null,
  source: "PUBLIC_EVENT",
  ownershipType: "CLAIMABLE",
  selectionPublicId: null,
});

const normalizeSystemItem = (item) => ({
  ...item,
  promotionPublicId: item?.publicId,
  walletPublicId: null,
  source: "SYSTEM_AUTO",
  ownershipType: "SYSTEM",
  selectionPublicId: null,
});

const normalizeWalletPage = (response) => {
  const page = unwrap(response) || {};
  const content = Array.isArray(page) ? page : page.content || [];
  return Array.isArray(page)
    ? content.map(flattenWalletItem)
    : { ...page, content: content.map(flattenWalletItem) };
};

const normalizePublicPage = (response) => {
  const page = unwrap(response) || {};
  const content = Array.isArray(page) ? page : page.content || [];
  return Array.isArray(page)
    ? content.map(normalizePublicItem)
    : { ...page, content: content.map(normalizePublicItem) };
};

const normalizeSystemPage = (response) => {
  const page = unwrap(response) || {};
  const content = Array.isArray(page) ? page : page.content || [];
  return Array.isArray(page)
    ? content.map(normalizeSystemItem)
    : { ...page, content: content.map(normalizeSystemItem) };
};

const normalizeOffer = (item) => ({
  ...item,
  primaryPromotion: item?.primaryPromotion
    ? item.primaryPromotion.promotionType === "AUTO"
      ? normalizeSystemItem(item.primaryPromotion)
      : normalizePublicItem(item.primaryPromotion)
    : null,
});

const normalizeOfferPage = (response) => {
  const page = unwrap(response) || {};
  const content = Array.isArray(page) ? page : page.content || [];
  return Array.isArray(page)
    ? content.map(normalizeOffer)
    : { ...page, content: content.map(normalizeOffer) };
};

const walletParams = (params = {}) => {
  const resolved = { status: "AVAILABLE", ...params };
  if (resolved.status === "ALL" || resolved.status === null) {
    delete resolved.status;
  }
  return resolved;
};

const customerPromotionService = {
  getMyPromotions: async (params = {}) =>
    normalizeWalletPage(
      await apiClient.get("/api/customers/me/promotions", {
        params: walletParams(params),
      }),
    ),

  getMyVouchers: async (params = {}) =>
    normalizeWalletPage(
      await apiClient.get("/api/customers/me/promotions", {
        params: walletParams(params),
      }),
    ),

  getMyPromotionDetail: async (walletPublicId) =>
    flattenWalletItem(
      unwrap(
        await apiClient.get(
          `/api/customers/me/promotions/${walletPublicId}`,
        ),
      ),
    ),

  getMyPromotionHistory: async () =>
    unwrap(await apiClient.get("/api/customers/me/promotion-history")) || [],

  getPublicPromotions: async (params = {}) =>
    normalizePublicPage(
      await apiClient.get("/api/promotions/public", { params }),
    ),

  getPublicOffers: async (params = {}) =>
    normalizeOfferPage(
      await apiClient.get("/api/promotions/offers", { params }),
    ),

  getSystemPromotions: async (params = {}) =>
    normalizeSystemPage(
      await apiClient.get("/api/promotions/system", { params }),
    ),

  claimVoucher: async (promotionPublicId) =>
    flattenWalletItem(
      unwrap(
        await apiClient.post(`/api/promotions/${promotionPublicId}/claim`),
      ),
    ),
};

export default customerPromotionService;
