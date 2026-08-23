import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import InfoSection from './InfoSection';

describe('InfoSection company introduction', () => {
  it('presents the company information and allows the details to be collapsed', () => {
    render(<InfoSection />);

    expect(screen.getByRole('heading', { name: 'Thông tin' })).toBeInTheDocument();
    expect(screen.getByText(/Thành lập từ năm 2003/)).toBeInTheDocument();
    expect(screen.getByText('IMAX Laser')).toBeInTheDocument();
    expect(screen.getByText('Góc Điện Ảnh')).toBeVisible();

    const collapseButton = screen.getByRole('button', { name: 'Thu gọn' });
    expect(collapseButton).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(collapseButton);

    expect(screen.getByRole('button', { name: 'Xem thêm' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(document.querySelector('#lorafilm-information-details')).toHaveClass('hidden');
    expect(document.querySelector('#gioi-thieu')).toBeInTheDocument();
  });
});
