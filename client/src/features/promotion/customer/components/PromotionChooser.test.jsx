import { render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import PromotionChooser from './PromotionChooser';

const defaultProps = {
  open: true,
  bookingAmount: 200000,
  onSelect: vi.fn(),
  onClear: vi.fn(),
  onClose: vi.fn(),
  onRefresh: vi.fn(),
};

const voucher = overrides => ({
  publicId: 'voucher-1',
  name: 'Voucher 20K',
  code: 'SAVE20',
  status: 'ACTIVE',
  validTo: '2099-12-31T23:59:59Z',
  conditionsJson: '{}',
  actionsJson: JSON.stringify([{
    discountType: 'FIXED_AMOUNT',
    discountValue: 20000,
  }]),
  ...overrides,
});

describe('PromotionChooser', () => {
  it('shows an empty wallet state without inventing promotions', () => {
    render(<PromotionChooser {...defaultProps} vouchers={[]} />);

    expect(screen.getByText('Ví voucher đang trống')).toBeInTheDocument();
    expect(screen.getByText(/voucher được phát hành cho bạn/i)).toBeInTheDocument();
  });

  it('disables a voucher when the booking is below its minimum amount', () => {
    render(
      <PromotionChooser
        {...defaultProps}
        bookingAmount={100000}
        vouchers={[voucher({
          minimumOrderAmount: 300000,
          conditionsJson: JSON.stringify({ minimumOrderAmount: 300000 }),
        })]}
      />
    );

    expect(screen.getByText(/cần đơn tối thiểu 300.000/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chọn voucher' })).toBeDisabled();
  });

  it('recommends the eligible voucher with the highest estimated reduction', () => {
    render(
      <PromotionChooser
        {...defaultProps}
        vouchers={[
          voucher({ publicId: 'voucher-20', name: 'Voucher 20K' }),
          voucher({
            publicId: 'voucher-50',
            name: 'Voucher 50K',
            code: 'SAVE50',
            actionsJson: JSON.stringify([{
              discountType: 'FIXED_AMOUNT',
              discountValue: 50000,
            }]),
          }),
        ]}
      />
    );

    const bestVoucher = screen.getByText('Voucher 50K').closest('article');
    const lowerVoucher = screen.getByText('Voucher 20K').closest('article');

    expect(within(bestVoucher).getByText('Gợi ý')).toBeInTheDocument();
    expect(within(lowerVoucher).queryByText('Gợi ý')).not.toBeInTheDocument();
    expect(within(bestVoucher).getByText(/ước tính giảm 50.000/i)).toBeInTheDocument();
  });
});
