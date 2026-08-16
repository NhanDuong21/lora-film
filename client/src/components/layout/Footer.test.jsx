import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Footer from './Footer';

describe('Footer', () => {
  it('exposes the customer policies, cinema link and supported payment methods', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    );

    expect(screen.getByText('LoraFilm — Vé phim trong tầm tay.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Hệ thống rạp' })).toHaveAttribute('href', '/#rap');
    expect(screen.getByRole('link', { name: 'Chính sách đổi, hủy và hoàn vé' })).toHaveAttribute('href', '/support/refunds');
    expect(screen.getByRole('link', { name: 'Chính sách bảo mật' })).toHaveAttribute('href', '/support/privacy');
    expect(screen.getByText('VNPay · MoMo · Tiền mặt tại quầy')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /1900 6868/ })).toHaveAttribute('href', 'tel:19006868');
  });
});
