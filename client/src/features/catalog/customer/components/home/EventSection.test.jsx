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
    customerPromotionService.getPublicPromotions.mockResolvedValue({
      content: [
        promotion('welcome', 'Welcome to LoraFilm - 10% off', 'PERCENTAGE', 10),
        promotion('instant', 'LoraFilm 50K instant discount', 'FIXED_AMOUNT', 50000),
        promotion('night', 'Combo night - 20% off', 'PERCENTAGE', 20),
      ],
    });
  });

  it('uses Vietnamese marketing copy and the guest promotion link', async () => {
    render(
      <MemoryRouter>
        <EventSection />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Chào thành viên mới – giảm 10%')).toBeInTheDocument();
    expect(screen.getByText('Giảm ngay 50.000đ')).toBeInTheDocument();
    expect(screen.getByText('Combo tối – giảm 20%')).toBeInTheDocument();
    expect(screen.queryByText(/Welcome to|instant discount|Combo night/)).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem tất cả ưu đãi' })).toHaveAttribute('href', '/promotions');
    expect(screen.getAllByRole('button', { name: 'Đăng nhập để nhận' })).toHaveLength(3);
  });
});
