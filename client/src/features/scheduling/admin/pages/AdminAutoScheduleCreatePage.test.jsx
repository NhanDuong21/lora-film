import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoScheduleForm from '../hooks/useAutoScheduleForm';
import AdminAutoScheduleCreatePage from './AdminAutoScheduleCreatePage';

vi.mock('../hooks/useAutoScheduleForm');
vi.mock('@/components/common/SearchableSelect', () => ({
  default: ({ id, placeholder }) => <input id={id} aria-label={placeholder} readOnly />,
}));

const baseForm = () => ({
  cinemas: [{ publicId: 'cinema-1', name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh' }],
  movies: [],
  auditoriums: [],
  versionsByMovie: {},
  selectedCinemaId: 'cinema-1',
  setSelectedCinemaId: vi.fn(),
  selectedCinema: { name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh' },
  scheduleFrom: '2099-08-22',
  setScheduleFrom: vi.fn(),
  scheduleTo: '2099-08-28',
  setScheduleTo: vi.fn(),
  slotGranularityMinutes: 15,
  setSlotGranularityMinutes: vi.fn(),
  previewTtlMinutes: 60,
  setPreviewTtlMinutes: vi.fn(),
  selectedAuditoriumIds: [],
  toggleAuditorium: vi.fn(),
  selectAllActiveAuditoriums: vi.fn(),
  clearAuditoriums: vi.fn(),
  selectedMovieVersionIds: [],
  selectedVersions: [],
  toggleVersion: vi.fn(),
  selectEligibleMovieVersions: vi.fn(),
  clearMovieVersions: vi.fn(),
  isLoadingCinemas: false,
  isLoadingAuditoriums: false,
  isLoadingMovies: false,
  isSubmitting: false,
  errors: {},
  readinessIssues: ['Chọn ít nhất một phòng chiếu.', 'Chọn ít nhất một định dạng phim.'],
  isReady: false,
  selectionNotice: '',
  movieLoadError: '',
  retryMovies: vi.fn(),
  dateRangeInfo: {
    dayCount: 7,
    cinemaToday: '2099-07-23',
    isTooLong: false,
    suggestedScheduleFrom: null,
    suggestedScheduleTo: null,
  },
  toggleMovieExpansion: vi.fn(),
  handleSubmit: vi.fn(),
});

describe('AdminAutoScheduleCreatePage', () => {
  beforeEach(() => useAutoScheduleForm.mockReturnValue(baseForm()));

  it('keeps an oversized range, explains it inline, and blocks submission', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      scheduleTo: '2099-08-29',
      readinessIssues: ['Khoảng ngày vượt quá giới hạn 7 ngày.'],
      dateRangeInfo: {
        dayCount: 8,
        cinemaToday: '2099-07-23',
        isTooLong: true,
        suggestedScheduleFrom: '2099-08-22',
        suggestedScheduleTo: '2099-08-28',
      },
    });

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('Mỗi bản xem trước tối đa 7 ngày. Bạn có thể tạo nhiều bản liên tiếp để lập lịch trước cho cả tháng.')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Khoảng đã chọn gồm 8 ngày');
    expect(screen.getByRole('alert')).toHaveTextContent('22/08/2099 – 28/08/2099');
    expect(screen.getByRole('alert')).not.toHaveTextContent('2099-08-22');
    expect(screen.getByDisplayValue('2099-08-29')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tạo bản xem trước/i })).toBeDisabled();
  });

  it('offers explicit room actions and explains ineligible movies', () => {
    const form = baseForm();
    form.auditoriums = [{
      publicId: 'aud-1', name: 'Phòng 1', status: 'ACTIVE', screenType: '2D', soundType: 'Dolby', capacity: 120,
    }];
    form.selectedAuditoriumIds = ['aud-1'];
    form.movies = [{
      publicId: 'movie-1', title: 'Phim chưa phát hành', eligible: false,
      reasons: [{ code: 'OUTSIDE_RELEASE_WINDOW', message: 'Ngoài thời gian phát hành' }],
      releaseDate: '2099-09-01', durationMinutes: 110,
    }];
    form.versionsByMovie = {
      'movie-1': [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE', format: '2D' }],
    };
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Chọn tất cả đang hoạt động' }));
    fireEvent.click(screen.getByRole('button', { name: 'Xóa chọn' }));
    expect(form.selectAllActiveAuditoriums).toHaveBeenCalledTimes(1);
    expect(form.clearAuditoriums).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Ngoài thời gian phát hành')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Phim chưa phát hành/i }));
    expect(screen.getAllByRole('checkbox', { name: /2D/i }).find(control => control.disabled)).toBeDisabled();
  });

  it('offers date presets and bulk movie selection actions', () => {
    const form = baseForm();
    form.movies = [{
      publicId: 'movie-1',
      title: 'Phim A',
      eligible: true,
      reasons: [],
      releaseDate: '2099-09-01',
      durationMinutes: 110,
    }];
    form.versionsByMovie = {
      'movie-1': [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE', format: '2D' }],
    };
    form.selectedMovieVersionIds = ['version-1'];
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: '3 ngày' }));
    expect(form.setScheduleFrom).toHaveBeenCalledWith('2099-08-22');
    expect(form.setScheduleTo).toHaveBeenCalledWith('2099-08-24');
    fireEvent.click(screen.getByRole('button', { name: 'Chọn phim đủ điều kiện' }));
    fireEvent.click(screen.getByRole('button', { name: 'Bỏ chọn tất cả' }));
    expect(form.selectEligibleMovieVersions).toHaveBeenCalledWith(['movie-1']);
    expect(form.clearMovieVersions).toHaveBeenCalledTimes(1);
  });

  it('renders the primary movie poster and falls back safely when it cannot load', () => {
    const form = baseForm();
    form.movies = [{
      publicId: 'movie-1',
      title: 'Phim có poster',
      originalTitle: 'Movie With Poster',
      primaryPoster: 'https://cdn.example.test/poster.jpg',
      eligible: true,
      reasons: [],
      releaseDate: '2099-09-01',
      durationMinutes: 110,
    }];
    form.versionsByMovie = {
      'movie-1': [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE', format: '2D' }],
    };
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    const poster = screen.getByRole('img', { name: 'Poster Phim có poster' });
    expect(poster).toHaveAttribute('src', 'https://cdn.example.test/poster.jpg');
    fireEvent.error(poster);
    expect(screen.getByText('Chưa có poster')).toBeInTheDocument();
  });

  it('shows selected chips and keeps the primary action beside readiness', () => {
    const form = baseForm();
    form.selectedAuditoriumIds = ['aud-1'];
    form.selectedMovieVersionIds = ['version-1'];
    form.selectedVersions = [{
      publicId: 'version-1', moviePublicId: 'movie-1', movieTitle: 'Phim A', versionName: 'IMAX', status: 'ACTIVE',
    }];
    form.readinessIssues = [];
    form.isReady = true;
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByRole('button', { name: 'Bỏ chọn Phim A IMAX' })).toBeInTheDocument();
    expect(screen.getByText('Cấu hình hợp lệ và sẵn sàng tạo bản xem trước.')).toBeInTheDocument();
    expect(screen.getByText('22/08/2099 – 28/08/2099')).toBeInTheDocument();
    expect(screen.queryByText('2099-08-22 → 2099-08-28')).not.toBeInTheDocument();
    const generate = screen.getByRole('button', { name: /Tạo bản xem trước/i });
    expect(generate).toBeEnabled();
    fireEvent.click(generate);
    expect(form.handleSubmit).toHaveBeenCalledTimes(1);
  });

  it('starts with the scope step open and exposes the room/movie steps through the progress nav', () => {
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    const scopeToggle = screen.getAllByRole('button', { name: 'Thu gọn' })[0];
    expect(scopeToggle).toHaveAttribute('aria-expanded', 'true');
    fireEvent.click(screen.getAllByRole('button', { name: /2Phòng chiếu/ })[0]);
    expect(scopeToggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByText('4. Thiết lập nâng cao').closest('details')).not.toHaveAttribute('open');
  });

  it('automatically opens advanced settings when an advanced validation error exists', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      errors: { previewTtlMinutes: 'Giá trị từ 5 đến 120' },
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('4. Thiết lập nâng cao').closest('details')).toHaveAttribute('open');
    expect(screen.getByText('Giá trị từ 5 đến 120')).toBeInTheDocument();
  });

  it('distinguishes search, selected-only, initial-empty, and load-failure states with retry', () => {
    const movie = {
      publicId: 'movie-1',
      title: 'Phim A',
      eligible: true,
      reasons: [],
      releaseDate: '2099-09-01',
      durationMinutes: 110,
    };
    const form = {
      ...baseForm(),
      movies: [movie],
      versionsByMovie: {
        'movie-1': [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE', format: '2D' }],
      },
    };
    useAutoScheduleForm.mockReturnValue(form);
    const { unmount } = render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm phim' }), { target: { value: 'không tồn tại' } });
    expect(screen.getByText(/Không tìm thấy phim khớp từ khóa/)).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm phim' }), { target: { value: '' } });
    fireEvent.click(screen.getByRole('checkbox', { name: 'Chỉ xem đã chọn' }));
    expect(screen.getByText('Chưa có định dạng nào được chọn để hiển thị.')).toBeInTheDocument();
    unmount();

    useAutoScheduleForm.mockReturnValue({ ...baseForm(), movies: [] });
    const initialEmpty = render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    expect(screen.getByText(/Chưa có phim đủ điều kiện trong khoảng ngày đã chọn/)).toBeInTheDocument();
    initialEmpty.unmount();

    const retryMovies = vi.fn();
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      movieLoadError: 'Không thể xác minh điều kiện phim cho khoảng ngày đã chọn.',
      retryMovies,
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: 'Thử tải lại' }));
    expect(retryMovies).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Không thể tải danh sách phim/)).toBeInTheDocument();
  });
});
