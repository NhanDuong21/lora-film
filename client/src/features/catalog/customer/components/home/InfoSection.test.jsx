import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import InfoSection from './InfoSection';
import { getCinemas, getShowtimes } from '@/features/catalog/customer/services/movieService';

vi.mock('@/features/catalog/customer/services/movieService', () => ({
  getCinemas: vi.fn(),
  getShowtimes: vi.fn(),
}));

describe('InfoSection cinema discovery', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getCinemas.mockResolvedValue({
      content: [
        {
          publicId: 'cinema-1',
          slug: 'lorafilm-landmark-81',
          name: 'LoraFilm Landmark 81',
          district: 'Binh Thanh',
          city: 'Ho Chi Minh City',
          address: '208 Nguyen Huu Canh, Vinhomes Central Park',
        },
        {
          publicId: 'cinema-2',
          slug: 'lorafilm-crescent-mall',
          name: 'LoraFilm Crescent Mall',
          district: 'District 7',
          city: 'Ho Chi Minh City',
          address: '101 Ton Dat Tien, Tan Phu Ward',
        },
      ],
    });
    getShowtimes.mockImplementation(({ cinemaSlug }) => Promise.resolve({
      content: [],
      totalElements: cinemaSlug === 'lorafilm-landmark-81' ? 12 : 8,
    }));
  });

  it('loads real cinemas, localizes their addresses and shows todays showtime totals', async () => {
    render(
      <MemoryRouter>
        <InfoSection />
      </MemoryRouter>,
    );

    expect(await screen.findByText('LoraFilm Landmark 81')).toBeInTheDocument();
    expect(screen.getByText('Bình Thạnh, TP. Hồ Chí Minh')).toBeInTheDocument();
    expect(screen.getByText('208 Nguyễn Hữu Cảnh, Vinhomes Central Park')).toBeInTheDocument();
    expect(screen.getByText('12 suất đang mở hôm nay')).toBeInTheDocument();
    expect(screen.getByText('8 suất đang mở hôm nay')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Xem lịch chiếu' })[0]).toHaveAttribute(
      'href',
      '/cinema/lorafilm-landmark-81',
    );
    await waitFor(() => expect(getShowtimes).toHaveBeenCalledTimes(2));
  });
});
