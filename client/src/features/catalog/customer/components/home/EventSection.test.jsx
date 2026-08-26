import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EventSection from './EventSection';
import { useAuth } from '@/contexts/AuthContext';
import customerPromotionService from '@/features/promotion/customer/services/customerPromotionService';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/features/promotion/customer/services/customerPromotionService', () => ({
  default: {
    getPublicOffers: vi.fn(),
    getPublicPromotions: vi.fn(),
    getMyPromotions: vi.fn(),
    claimVoucher: vi.fn(),
  },
}));

const promotion = (publicId, name, discountType, discountValue) => ({
  publicId,
  promotionPublicId: publicId,
  name,
  promotionType: 'VOUCHER',
  validTo: '2026-12-31T23:59:00',
  maxRedemptions: 100,
  redemptionCount: 5,
  conditionsJson: { minimumOrderAmount: 100000 },
  actionsJson: [{ discountType, discountValue }],
});

describe('EventSection homepage promotions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({ isAuthenticated: false, isInitializing: false });
    customerPromotionService.getPublicOffers.mockResolvedValue({ content: [] });
    customerPromotionService.getPublicPromotions.mockResolvedValue({
      content: [
        promotion('welcome', 'Welcome to LoraFilm - 10% off', 'PERCENTAGE', 10),
        promotion('instant', 'LoraFilm 50K instant discount', 'FIXED_AMOUNT', 50000),
        promotion('night', 'Combo night - 20% off', 'PERCENTAGE', 20),
      ],
    });
  });

  it('does not fall back to active public vouchers without a published home presentation', async () => {
    render(
      <MemoryRouter>
        <EventSection />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Chưa có chương trình đang mở')).toBeInTheDocument();
    expect(screen.queryByText(/Welcome to|instant discount|Combo night/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem tất cả ưu đãi' })).toHaveAttribute('href', '/promotions');
    expect(customerPromotionService.getPublicPromotions).not.toHaveBeenCalled();
  });

  it('renders one campaign presentation instead of duplicating its benefit template', async () => {
    customerPromotionService.getPublicOffers.mockResolvedValue({
      content: [{
        campaignPublicId: 'campaign-1',
        headline: 'Thứ hai vui vẻ – đồng giá vé 60K',
        summary: 'Mở đầu tuần mới với giá vé nhẹ nhàng hơn.',
        coverImageUrl: '/api/promotions/assets/monday.webp',
        imageAltText: 'Khán phòng trong chương trình Thứ hai vui vẻ',
        validTo: '2026-12-31T23:59:00',
        primaryPromotion: promotion('monday', 'Monday 60K', 'FIXED_AMOUNT', 60000),
      }],
    });

    render(
      <MemoryRouter>
        <EventSection />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Thứ hai vui vẻ – đồng giá vé 60K')).toBeInTheDocument();
    expect(screen.getByAltText('Khán phòng trong chương trình Thứ hai vui vẻ'))
      .toHaveAttribute('src', '/api/promotions/assets/monday.webp');
    expect(screen.queryByText('Chào thành viên mới – giảm 10%')).not.toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Đăng nhập để nhận' })).toHaveLength(1);
  });
});
