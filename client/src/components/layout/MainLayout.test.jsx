import { fireEvent, render, screen } from '@testing-library/react';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import MainLayout from './MainLayout';

vi.mock('./Header', () => ({
  default: () => (
    <header>
      <Link to="/">Trang chủ</Link>
      <Link to="/movies">Phim</Link>
    </header>
  )
}));

vi.mock('./Footer', () => ({
  default: () => <footer>Footer</footer>
}));

vi.mock('@/features/booking/customer/components/ActiveBookingRecoveryBanner', () => ({
  default: () => <aside data-testid="active-booking-recovery">Đang giữ ghế</aside>
}));

describe('MainLayout active booking recovery', () => {
  it('keeps the recovery banner mounted while the customer changes screens', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<MainLayout />}>
            <Route index element={<div>Màn hình trang chủ</div>} />
            <Route path="/movies" element={<div>Màn hình danh sách phim</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Màn hình trang chủ')).toBeInTheDocument();
    expect(screen.getAllByTestId('active-booking-recovery')).toHaveLength(1);

    fireEvent.click(screen.getByRole('link', { name: 'Phim' }));

    expect(screen.getByText('Màn hình danh sách phim')).toBeInTheDocument();
    expect(screen.getAllByTestId('active-booking-recovery')).toHaveLength(1);
  });
});
