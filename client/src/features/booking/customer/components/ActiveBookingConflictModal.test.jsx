import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ActiveBookingConflictModal from './ActiveBookingConflictModal';

describe('ActiveBookingConflictModal', () => {
  it('offers the two explicit production-safe choices in Vietnamese', () => {
    const onResume = vi.fn();
    const onCancel = vi.fn();
    render(
      <ActiveBookingConflictModal
        bookingCode="LORAFILM-000001"
        seatNames="B1, B2"
        timeLeft="12:34"
        onClose={vi.fn()}
        onResume={onResume}
        onCancel={onCancel}
      />
    );

    expect(screen.getByRole('alertdialog', { name: /Bạn đã có đơn giữ ghế/i }))
      .toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText('B1, B2')).toBeInTheDocument();
    expect(screen.getByText('12:34')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Tiếp tục thanh toán/i }));
    fireEvent.click(screen.getByRole('button', { name: /Hủy đơn cũ để chọn lại/i }));

    expect(onResume).toHaveBeenCalledOnce();
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('shows cancellation errors and locks all actions while processing', () => {
    const onClose = vi.fn();
    render(
      <ActiveBookingConflictModal
        error="Không thể hủy đơn đang giữ ghế."
        pending
        onClose={onClose}
        onResume={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Không thể hủy đơn đang giữ ghế.');
    expect(screen.getByRole('button', { name: 'Đóng' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Đang hủy đơn cũ/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Tiếp tục thanh toán/i })).toBeDisabled();
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).not.toHaveBeenCalled();
  });
});
