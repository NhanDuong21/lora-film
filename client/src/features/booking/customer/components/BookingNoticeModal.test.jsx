import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import BookingNoticeModal from './BookingNoticeModal';

describe('BookingNoticeModal', () => {
  it('renders a production notice and closes with its action', () => {
    const onClose = vi.fn();
    render(
      <BookingNoticeModal
        title="Không thể cập nhật"
        message="Vui lòng thử lại."
        variant="error"
        onClose={onClose}
      />
    );

    expect(screen.getByRole('alertdialog', { name: 'Không thể cập nhật' }))
      .toHaveAttribute('aria-modal', 'true');
    expect(screen.getByText('Vui lòng thử lại.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Đã hiểu' }));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
