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

const customerPromotionService = {
  getMyPromotions: async (params = {}) =>
    normalizeWalletPage(
      await apiClient.get("/api/customers/me/promotions", { params }),
    ),

  getMyVouchers: async (params = {}) =>
    normalizeWalletPage(
      await apiClient.get("/api/customers/me/promotions", { params }),
    ),

  getPublicPromotions: async (params = {}) =>
    normalizePublicPage(
      await apiClient.get("/api/promotions/public", { params }),
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
