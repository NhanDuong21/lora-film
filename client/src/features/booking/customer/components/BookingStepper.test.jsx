import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import BookingStepper from './BookingStepper';

describe('BookingStepper', () => {
  it('shows food as the optional third step and payment as the fourth step', () => {
    render(<BookingStepper currentStep={2} />);

    expect(screen.getByText('Chọn suất')).toBeInTheDocument();
    expect(screen.getAllByText('Chọn ghế')).toHaveLength(2);
    expect(screen.getByText('Bắp nước')).toBeInTheDocument();
    expect(screen.getByText('Không bắt buộc')).toBeInTheDocument();
    expect(screen.getByText('Thanh toán')).toBeInTheDocument();
    expect(screen.queryByText('Nhận vé')).not.toBeInTheDocument();
  });

  it('renders all four steps as completed on the success page', () => {
    render(<BookingStepper currentStep={4} completed />);

    expect(screen.getByLabelText('Tiến trình đặt vé').querySelectorAll('svg')).toHaveLength(4);
  });
});
