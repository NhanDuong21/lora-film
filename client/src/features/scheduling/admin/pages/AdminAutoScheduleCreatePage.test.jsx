import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoScheduleForm from '../hooks/useAutoScheduleForm';
import useExistingShowtimeSummary from '../hooks/useExistingShowtimeSummary';
import AdminAutoScheduleCreatePage from './AdminAutoScheduleCreatePage';

vi.mock('../hooks/useAutoScheduleForm');
vi.mock('../hooks/useExistingShowtimeSummary');
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
  beforeEach(() => {
    useAutoScheduleForm.mockReturnValue(baseForm());
    useExistingShowtimeSummary.mockReturnValue({
      countsByDate: {},
      totalExisting: 0,
      isLoading: false,
      error: null,
      retry: vi.fn(),
    });
  });

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

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Mỗi bản lịch tối đa 7 ngày. Bạn có thể tạo nhiều bản liên tiếp để chuẩn bị lịch cho cả tháng.',
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Khoảng đã chọn gồm 8 ngày');
    expect(screen.getByRole('alert')).toHaveTextContent('22/08/2099 – 28/08/2099');
    expect(screen.getByRole('alert')).not.toHaveTextContent('2099-08-22');
    expect(screen.getByDisplayValue('2099-08-29')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Phòng chiếu.*hoàn tất/i })).toBeDisabled();
    expect(screen.queryByRole('button', { name: /Tạo lịch để kiểm tra/i })).not.toBeInTheDocument();
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

    fireEvent.click(screen.getByRole('button', { name: /Phòng chiếu.*hoàn tất/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Chọn tất cả đang hoạt động' }));
    fireEvent.click(screen.getByRole('button', { name: 'Xóa chọn' }));
    expect(form.selectAllActiveAuditoriums).toHaveBeenCalledTimes(1);
    expect(form.clearAuditoriums).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
    expect(screen.queryByText('Phim chưa phát hành')).not.toBeInTheDocument();
    expect(screen.queryByText('Khoảng ngày tạo lịch nằm ngoài thời gian phát hành của phim.')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Bị loại (1)' }));
    expect(screen.getByText('Phim chưa phát hành')).toBeInTheDocument();
    expect(screen.getByText('Khoảng ngày tạo lịch nằm ngoài thời gian phát hành của phim.')).toBeInTheDocument();
    expect(screen.getByText('Chỉ dùng để kiểm tra lý do, không thể chọn định dạng.')).toBeInTheDocument();
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
    form.selectedAuditoriumIds = ['aud-1'];
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: '3 ngày' }));
    expect(form.setScheduleFrom).toHaveBeenCalledWith('2099-08-22');
    expect(form.setScheduleTo).toHaveBeenCalledWith('2099-08-24');
    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
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
    form.selectedAuditoriumIds = ['aud-1'];
    useAutoScheduleForm.mockReturnValue(form);

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
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
    useExistingShowtimeSummary.mockReturnValue({
      countsByDate: { '2099-08-22': 12 },
      totalExisting: 12,
      isLoading: false,
      error: null,
      retry: vi.fn(),
    });

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByRole('button', { name: 'Bỏ chọn Phim A IMAX' })).toBeInTheDocument();
    expect(screen.getByText('22/08/2099 – 28/08/2099')).toBeInTheDocument();
    expect(screen.queryByText('2099-08-22 → 2099-08-28')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Kiểm tra.*hoàn tất/i }));
    expect(screen.getByText(/Hệ thống chỉ bổ sung vào khung còn trống/)).toBeInTheDocument();
    expect(screen.getByText('Đã tìm thấy 12 suất hiện có trong khoảng ngày này.')).toBeInTheDocument();
    expect(screen.getByText(/Bạn chỉ chọn Phim A/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kiểm tra lịch hiện có' })).toBeInTheDocument();
    expect(screen.getByText('Thông tin đã hợp lệ. Bạn có thể tạo lịch để kiểm tra trước khi mở bán.')).toBeInTheDocument();
    const generate = screen.getByRole('button', { name: /Tạo lịch để kiểm tra/i });
    expect(generate).toBeEnabled();
    fireEvent.click(generate);
    expect(form.handleSubmit).toHaveBeenCalledTimes(1);
  });

  it('starts at scope and moves through the wizard progress navigation', () => {
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Chọn rạp và khoảng ngày' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Phòng chiếu.*hoàn tất/i }));
    expect(screen.queryByRole('heading', { name: 'Chọn rạp và khoảng ngày' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Chọn phòng chiếu' })).toBeInTheDocument();
    expect(screen.getByText('Tùy chọn nâng cao').closest('details')).not.toHaveAttribute('open');
  });

  it('automatically opens advanced settings when an advanced validation error exists', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      errors: { previewTtlMinutes: 'Giá trị từ 5 đến 120' },
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('Tùy chọn nâng cao').closest('details')).toHaveAttribute('open');
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
      selectedAuditoriumIds: ['aud-1'],
      movies: [movie],
      versionsByMovie: {
        'movie-1': [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE', format: '2D' }],
      },
    };
    useAutoScheduleForm.mockReturnValue(form);
    const { unmount } = render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm phim' }), { target: { value: 'không tồn tại' } });
    expect(screen.getByText(/Không tìm thấy phim khớp từ khóa/)).toBeInTheDocument();
    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm phim' }), { target: { value: '' } });
    fireEvent.click(screen.getByRole('checkbox', { name: 'Chỉ xem đã chọn' }));
    expect(screen.getByText('Chưa có định dạng nào được chọn để hiển thị.')).toBeInTheDocument();
    unmount();

    useAutoScheduleForm.mockReturnValue({ ...baseForm(), selectedAuditoriumIds: ['aud-1'], movies: [] });
    const initialEmpty = render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
    expect(screen.getByText(/Chưa có phim đủ điều kiện trong khoảng ngày đã chọn/)).toBeInTheDocument();
    initialEmpty.unmount();

    const retryMovies = vi.fn();
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      selectedAuditoriumIds: ['aud-1'],
      movieLoadError: 'Không thể xác minh điều kiện phim cho khoảng ngày đã chọn.',
      retryMovies,
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: /Phim.*hoàn tất/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Thử tải lại' }));
    expect(retryMovies).toHaveBeenCalledTimes(1);
    expect(screen.getByText(/Không thể tải danh sách phim/)).toBeInTheDocument();
  });
});
