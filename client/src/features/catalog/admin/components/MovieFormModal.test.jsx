import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import MovieFormModal from './MovieFormModal';

vi.mock('@/features/catalog/admin/services/adminMovieService', () => ({
  default: {
    createMovie: vi.fn(),
    updateMovie: vi.fn(),
  },
}));

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="current-location">{location.pathname}{location.search}</span>;
}

const renderModal = (selectedMovie = null, overrides = {}) => {
  const props = {
    selectedMovie,
    triggerToast: vi.fn(),
    onClose: vi.fn(),
    onRefreshList: vi.fn(),
    detailQuery: '?status=DRAFT',
    ...overrides,
  };

  render(
    <MemoryRouter initialEntries={['/admin/movies']}>
      <MovieFormModal {...props} />
      <LocationProbe />
    </MemoryRouter>,
  );

  return props;
};

describe('MovieFormModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('focuses creation on required fields and keeps optional information collapsed', () => {
    renderModal();

    expect(screen.getByRole('heading', { name: 'Tạo hồ sơ phim' })).toBeInTheDocument();
    expect(screen.getByText('Bước 1 · Thông tin cơ bản')).toBeInTheDocument();
    expect(screen.getByLabelText('Tên phim')).toBeInTheDocument();
    expect(screen.getByLabelText('Thời lượng (phút)')).toBeInTheDocument();
    expect(screen.queryByLabelText('Tên gốc')).not.toBeInTheDocument();

    const optionalToggle = screen.getByRole('button', { name: /Thông tin bổ sung/ });
    expect(optionalToggle).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(optionalToggle);

    expect(optionalToggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByLabelText('Tên gốc')).toBeInTheDocument();
    expect(screen.getByLabelText('Tóm tắt nội dung')).toBeInTheDocument();
  });

  it('creates the base movie record and continues to its detail page', async () => {
    adminMovieService.createMovie.mockResolvedValue({
      data: { publicId: 'movie-public-1' },
    });
    const props = renderModal();

    fireEvent.change(screen.getByLabelText('Tên phim'), {
      target: { value: 'Paper Tiger' },
    });
    fireEvent.change(screen.getByLabelText('Thời lượng (phút)'), {
      target: { value: '115' },
    });
    fireEvent.change(screen.getByLabelText('Ngày bắt đầu khai thác tại rạp'), {
      target: { value: '2026-11-13' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Tạo và tiếp tục' }));

    await waitFor(() => {
      expect(adminMovieService.createMovie).toHaveBeenCalledWith({
        title: 'Paper Tiger',
        originalTitle: null,
        durationMinutes: 115,
        ageRating: 'P',
        originalReleaseDate: null,
        releaseDate: '2026-11-13',
        endDate: null,
        country: null,
        synopsis: null,
      });
    });

    expect(props.onRefreshList).toHaveBeenCalled();
    expect(props.onClose).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.getByTestId('current-location')).toHaveTextContent(
        '/admin/movies/movie-public-1?status=DRAFT',
      );
    });
  });

  it('opens populated optional fields when editing and saves the base information', async () => {
    adminMovieService.updateMovie.mockResolvedValue({ success: true });
    const selectedMovie = {
      publicId: 'movie-public-1',
      title: 'Paper Tiger',
      originalTitle: 'Paper Tiger',
      durationMinutes: 115,
      ageRating: 'P',
      releaseDate: '2026-11-13',
      endDate: '',
      country: 'US',
      synopsis: 'A family story.',
    };
    renderModal(selectedMovie);

    expect(screen.getByRole('heading', { name: 'Sửa thông tin cơ bản' })).toBeInTheDocument();
    expect(await screen.findByLabelText('Tên gốc')).toHaveValue('Paper Tiger');
    expect(screen.queryByText('Sau khi tạo hồ sơ')).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Tên phim'), {
      target: { value: 'Paper Tiger Updated' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }));

    await waitFor(() => {
      expect(adminMovieService.updateMovie).toHaveBeenCalledWith(
        'movie-public-1',
        expect.objectContaining({
          title: 'Paper Tiger Updated',
          originalTitle: 'Paper Tiger',
          country: 'US',
          synopsis: 'A family story.',
        }),
      );
    });
  });
});
