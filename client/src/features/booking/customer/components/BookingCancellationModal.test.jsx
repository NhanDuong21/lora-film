import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import BookingCancellationModal from './BookingCancellationModal';

describe('BookingCancellationModal', () => {
  it('explains the consequences and submits the optional reason', () => {
    const onConfirm = vi.fn();
    render(
      <BookingCancellationModal
        bookingCode="BK-001"
        onClose={vi.fn()}
        onConfirm={onConfirm}
      />
    );

    expect(screen.getByRole('dialog', { name: 'Xác nhận hủy giữ ghế' }))
      .toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText(/ghế sẽ được trả lại ngay/i)).toBeInTheDocument();
    fireEvent.change(screen.getByPlaceholderText(/chọn lại suất chiếu khác/i), {
      target: { value: 'Đổi suất chiếu' }
    });
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }));

    expect(onConfirm).toHaveBeenCalledWith('Đổi suất chiếu');
  });

  it('renders API errors and locks dismissal while processing', () => {
    const onClose = vi.fn();
    render(
      <BookingCancellationModal
        error="Đơn đã hết hạn"
        pending
        onClose={onClose}
        onConfirm={vi.fn()}
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Đơn đã hết hạn');
    expect(screen.getByRole('button', { name: 'Đóng' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Đang hủy...' })).toBeDisabled();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).not.toHaveBeenCalled();
  });
});
