import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import Header from './Header';
import { useAuth } from '@/contexts/AuthContext';
import { getCinemas } from '@/features/catalog/customer/services/movieService';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn()
}));

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn()
}));

function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{`${location.pathname}${location.search}`}</output>;
}

function renderHeader(initialEntry = '/') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Header />
      <Routes>
        <Route path="*" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuth.mockReturnValue({
      user: null,
      userRole: null,
      isAuthenticated: false,
      logout: vi.fn()
    });
    getCinemas.mockResolvedValue({
      data: [{
        publicId: 'cinema-public-1',
        slug: 'lorafilm-01',
        name: 'LoraFilm Sense City Cần Thơ'
      }]
    });
  });

  it('opens an accessible movie menu and navigates with the selected status', () => {
    renderHeader();

    const moviesButton = screen.getByRole('button', { name: /^Phim$/ });
    fireEvent.click(moviesButton);

    expect(moviesButton).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(screen.getByRole('menuitem', { name: 'Phim sắp chiếu' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/movies?status=UPCOMING');
  });

  it('submits the desktop search to the movie discovery page', () => {
    renderHeader();

    fireEvent.change(screen.getByLabelText('Tìm kiếm phim', { selector: '#header-search' }), {
      target: { value: '  Dune  ' }
    });
    fireEvent.click(screen.getAllByRole('button', { name: 'Tìm kiếm' })[0]);

    expect(screen.getByTestId('location')).toHaveTextContent('/movies?search=Dune');
  });

  it('shows the complete mobile navigation', () => {
    renderHeader();

    fireEvent.click(screen.getByRole('button', { name: 'Mở menu điều hướng' }));

    expect(screen.getByRole('heading', { name: 'Phim' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Góc Điện Ảnh' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Rạp/Giá Vé' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Mua vé nhanh' })).toHaveLength(2);
  });

  it('loads real cinemas for the cinema menu and uses the API slug', async () => {
    renderHeader();

    fireEvent.click(screen.getByRole('button', { name: 'Rạp/Giá Vé' }));
    fireEvent.click(await screen.findByRole('menuitem', {
      name: 'LoraFilm Sense City Cần Thơ'
    }));

    expect(getCinemas).toHaveBeenCalledWith({ page: 0, size: 100 });
    expect(screen.getByTestId('location')).toHaveTextContent('/cinema/lorafilm-01');
  });
});
