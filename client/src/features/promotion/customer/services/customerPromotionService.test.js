import { beforeEach, describe, expect, it, vi } from "vitest";
import apiClient from "@/services/apiClient";
import customerPromotionService from "./customerPromotionService";

vi.mock("@/services/apiClient", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe("customerPromotionService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads and flattens the authenticated customer promotion wallet", async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          content: [
            {
              publicId: "wallet-1",
              status: "AVAILABLE",
              promotion: { publicId: "promotion-1", name: "Summer" },
            },
          ],
          totalElements: 1,
        },
      },
    });

    const result = await customerPromotionService.getMyPromotions({
      page: 0,
      size: 50,
      sort: "validTo,asc",
    });

    expect(apiClient.get).toHaveBeenCalledWith("/api/customers/me/promotions", {
      params: { page: 0, size: 50, sort: "validTo,asc", status: "AVAILABLE" },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0]).toMatchObject({
      publicId: "wallet-1",
      promotionPublicId: "promotion-1",
      name: "Summer",
      source: "CUSTOMER_WALLET",
      ownershipType: "OWNED",
    });
  });

  it("can load the full wallet history without sending a status filter", async () => {
    apiClient.get.mockResolvedValue({
      data: { data: { content: [] } },
    });

    await customerPromotionService.getMyVouchers({
      page: 0,
      size: 100,
      sort: "validTo,asc",
      status: "ALL",
    });

    expect(apiClient.get).toHaveBeenCalledWith("/api/customers/me/promotions", {
      params: { page: 0, size: 100, sort: "validTo,asc" },
    });
  });

  it("loads and flattens a wallet promotion detail", async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          publicId: "wallet-1",
          status: "AVAILABLE",
          promotion: { publicId: "promotion-1", name: "Summer" },
        },
      },
    });

    const result = await customerPromotionService.getMyPromotionDetail("wallet-1");

    expect(apiClient.get).toHaveBeenCalledWith(
      "/api/customers/me/promotions/wallet-1",
    );
    expect(result).toMatchObject({
      publicId: "wallet-1",
      walletPublicId: "wallet-1",
      promotionPublicId: "promotion-1",
      name: "Summer",
      source: "CUSTOMER_WALLET",
    });
  });

  it("marks public event vouchers as claimable instead of owned", async () => {
    apiClient.get.mockResolvedValue({
      data: {
        data: {
          content: [{ publicId: "promotion-1", promotionType: "VOUCHER" }],
        },
      },
    });

    const result = await customerPromotionService.getPublicPromotions({
      page: 0,
    });

    expect(result.content[0]).toMatchObject({
      publicId: "promotion-1",
      promotionPublicId: "promotion-1",
      source: "PUBLIC_EVENT",
      ownershipType: "CLAIMABLE",
      walletPublicId: null,
    });
  });

  it("loads system discounts separately from the customer wallet", async () => {
    apiClient.post.mockResolvedValue({
      data: {
        data: { publicId: "wallet-1", promotion: { publicId: "promotion-1" } },
      },
    });
    apiClient.get.mockResolvedValue({
      data: {
        data: { content: [{ publicId: "system-1", promotionType: "AUTO" }] },
      },
    });

    await customerPromotionService.claimVoucher("promotion-1");
    const systems = await customerPromotionService.getSystemPromotions({
      page: 0,
    });

    expect(apiClient.post).toHaveBeenCalledWith(
      "/api/promotions/promotion-1/claim",
    );
    expect(apiClient.get).toHaveBeenCalledWith("/api/promotions/system", {
      params: { page: 0 },
    });
    expect(systems.content[0]).toMatchObject({
      publicId: "system-1",
      source: "SYSTEM_AUTO",
      ownershipType: "SYSTEM",
      walletPublicId: null,
    });
  });
});
