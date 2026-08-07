import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoScheduleForm from '../hooks/useAutoScheduleForm';
import AdminAutoScheduleCreatePage from './AdminAutoScheduleCreatePage';

vi.mock('../hooks/useAutoScheduleForm');

const readyPreflight = {
  canGenerate: true,
  planningFrom: '2099-08-22',
  planningTo: '2099-08-24',
  timezone: 'Asia/Ho_Chi_Minh',
  eligibleMovieCount: 2,
  eligibleVersionCount: 3,
  eligibleAuditoriumCount: 2,
  compatiblePairCount: 5,
  blockers: [],
};

const baseForm = () => ({
  cinemas: [{ publicId: 'cinema-1', name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh' }],
  selectedCinemaId: 'cinema-1',
  setSelectedCinemaId: vi.fn(),
  selectedCinema: { publicId: 'cinema-1', name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh' },
  planningDays: 3,
  setPlanningPreset: vi.fn(),
  slotGranularityMinutes: 15,
  setSlotGranularityMinutes: vi.fn(),
  previewTtlMinutes: 60,
  setPreviewTtlMinutes: vi.fn(),
  auditoriums: [{ publicId: 'aud-1', name: 'Phòng 1', status: 'ACTIVE', screenType: '2D' }],
  movies: [{
    publicId: 'movie-1', title: 'Phim A', primaryPoster: 'https://cdn.lorafilm.test/phim-a.jpg', durationMinutes: 120,
    versions: [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE' }],
  }],
  includeAuditoriumIds: [],
  excludeAuditoriumIds: [],
  includeMovieVersionIds: [],
  excludeMovieVersionIds: [],
  setScopeChoice: vi.fn(),
  preflight: readyPreflight,
  preflightError: '',
  isLoadingCinemas: false,
  isLoadingScope: false,
  isCheckingPreflight: false,
  isSubmitting: false,
  errors: {},
  isReady: true,
  runPreflight: vi.fn(),
  handleSubmit: vi.fn(),
});

describe('AdminAutoScheduleCreatePage Quick Mode', () => {
  beforeEach(() => useAutoScheduleForm.mockReturnValue(baseForm()));
  afterEach(() => vi.useRealTimers());

  it('requires only cinema and a planning preset in the primary flow', () => {
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    expect(screen.getByRole('heading', { name: 'Quick Mode' })).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toHaveValue('cinema-1');
    expect(screen.getByRole('button', { name: /3 ngàyNgắn hạn/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByText('Chọn phòng chiếu')).not.toBeInTheDocument();
    expect(screen.queryByText('Chọn phim muốn chiếu')).not.toBeInTheDocument();
  });

  it('changes the 1/3/7-day preset without exposing date inputs', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    fireEvent.click(screen.getByRole('button', { name: /7 ngàyCả tuần/ }));
    expect(form.setPlanningPreset).toHaveBeenCalledWith(7);
    expect(screen.queryByDisplayValue('2099-08-22')).not.toBeInTheDocument();
  });

  it('shows preflight counts and blocker repair links', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      isReady: false,
      preflight: {
        ...readyPreflight,
        canGenerate: false,
        blockers: [{ code: 'PRICING_INCOMPLETE', message: 'Backend message', actionPath: '/admin/pricing' }],
      },
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    expect(screen.getByText('Cần xử lý trước khi tạo lịch')).toBeInTheDocument();
    expect(screen.getByText('Bảng giá hiện tại chưa bao phủ tất cả phòng chiếu và khung giờ có thể xếp lịch.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mở nơi xử lý' })).toHaveAttribute('href', '/admin/pricing');
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('translates the fully blocked planning-range blocker into Vietnamese', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      isReady: false,
      preflight: {
        ...readyPreflight,
        canGenerate: false,
        blockers: [{
          code: 'PLANNING_RANGE_FULLY_BLOCKED',
          message: 'Closures, maintenance, or existing showtimes block every feasible slot',
          actionPath: '/admin/showtimes?cinemaId=cinema-1',
        }],
      },
    });

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('Tất cả khung giờ khả dụng đều đang bị chặn bởi lịch chiếu hiện có, thời gian rạp đóng cửa hoặc lịch bảo trì.')).toBeInTheDocument();
    expect(screen.queryByText(/Closures, maintenance/)).not.toBeInTheDocument();
  });

  it('shows concrete room/date diagnostics with cause-specific repair links', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      isReady: false,
      preflight: {
        ...readyPreflight,
        canGenerate: false,
        blockers: [{
          code: 'PLANNING_RANGE_FULLY_BLOCKED',
          message: 'Không còn phương án xếp lịch khả thi. Xem chi tiết bên dưới.',
          actionPath: '/admin/showtimes?date=2026-08-08&cinemaSlug=lora-cinema',
          details: [
            {
              code: 'EXISTING_SHOWTIME_CONFLICT',
              serviceDate: '2026-08-08',
              auditoriumPublicId: 'aud-1',
              auditoriumName: 'Phòng 1',
              affectedCandidateCount: 12,
              message: 'Phòng 1: 12/12 phương án trùng lịch chiếu hiện có.',
              actionPath: '/admin/showtimes?date=2026-08-08&cinemaSlug=lora-cinema',
            },
            {
              code: 'MAINTENANCE_CONFLICT',
              serviceDate: '2026-08-08',
              auditoriumPublicId: 'aud-2',
              auditoriumName: 'Phòng 2',
              affectedCandidateCount: 8,
              message: 'Phòng 2: 8/8 phương án trùng thời gian bảo trì.',
              actionPath: '/admin/rooms/edit/aud-2?tab=maintenance',
            },
          ],
        }],
      },
    });

    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('Không còn phương án xếp lịch khả thi. Xem chi tiết bên dưới.')).toBeInTheDocument();
    expect(screen.getByText('Thứ bảy, 08/08/2026')).toBeInTheDocument();
    expect(screen.getByText('Lịch chiếu hiện có')).toBeInTheDocument();
    expect(screen.getByText('Đóng phòng hoặc bảo trì')).toBeInTheDocument();
    expect(screen.getByText('Phòng 1: 12/12 phương án trùng lịch chiếu hiện có.')).toBeInTheDocument();
    expect(screen.getByText('Phòng 2: 8/8 phương án trùng thời gian bảo trì.')).toBeInTheDocument();
    const repairLinks = screen.getAllByRole('link', { name: 'Xử lý nguyên nhân này' });
    expect(repairLinks[0]).toHaveAttribute(
      'href',
      '/admin/showtimes?date=2026-08-08&cinemaSlug=lora-cinema',
    );
    expect(repairLinks[1]).toHaveAttribute('href', '/admin/rooms/edit/aud-2?tab=maintenance');
  });

  it('keeps include/exclude controls inside the optional advanced section', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    const advanced = screen.getByText('Nâng cao').closest('details');
    expect(advanced).not.toHaveAttribute('open');
    fireEvent.click(screen.getByText('Nâng cao'));
    const onlyUse = screen.getAllByRole('button', { name: 'Chỉ dùng' })[0];
    expect(onlyUse).toHaveAttribute('aria-pressed', 'false');
    expect(screen.queryByRole('checkbox', { name: 'Chỉ dùng' })).not.toBeInTheDocument();
    fireEvent.click(onlyUse);
    expect(form.setScopeChoice).toHaveBeenCalledWith('include', 'auditorium:aud-1', true);
  });

  it('keeps Advanced open and the previous preflight visible while filters refresh', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    const { rerender } = render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    const advanced = screen.getByText('Nâng cao').closest('details');

    fireEvent.click(screen.getByText('Nâng cao'));
    expect(advanced).toHaveAttribute('open');

    useAutoScheduleForm.mockReturnValue({ ...form, isCheckingPreflight: true, isReady: false });
    rerender(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(advanced).toHaveAttribute('open');
    expect(screen.getByText('Đang cập nhật kết quả kiểm tra…')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('renders eligible movie versions as selectable poster cards', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByText('Nâng cao'));

    expect(screen.getByRole('img', { name: 'Poster Phim A' })).toHaveAttribute(
      'src',
      'https://cdn.lorafilm.test/phim-a.jpg',
    );
    expect(screen.getByRole('heading', { name: 'Phim A' })).toBeInTheDocument();
    expect(screen.getByText('120 phút')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Chỉ dùng Phim A - 2D' }));
    expect(form.setScopeChoice).toHaveBeenCalledWith('include', 'version:version-1', true);
  });

  it('renders movie posters in batches and preserves choices while loading more or searching', () => {
    const movies = Array.from({ length: 13 }, (_, index) => ({
      publicId: `movie-${index + 1}`,
      title: `Phim ${String(index + 1).padStart(2, '0')}`,
      primaryPoster: `https://cdn.lorafilm.test/phim-${index + 1}.jpg`,
      versions: [{ publicId: `version-${index + 1}`, versionName: '2D', status: 'ACTIVE' }],
    }));
    const form = {
      ...baseForm(),
      movies,
      includeMovieVersionIds: ['version-1'],
    };
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByText('Nâng cao'));

    expect(screen.getAllByRole('img', { name: /Poster Phim/ })).toHaveLength(10);
    expect(screen.getByText('10/13 phiên bản')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chỉ dùng Phim 01 - 2D' })).toHaveAttribute('aria-pressed', 'true');

    fireEvent.click(screen.getByRole('button', { name: 'Xem thêm 3 phiên bản' }));
    expect(screen.getAllByRole('img', { name: /Poster Phim/ })).toHaveLength(13);
    expect(screen.getByText('13 phiên bản')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chỉ dùng Phim 01 - 2D' })).toHaveAttribute('aria-pressed', 'true');

    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm phim hoặc phiên bản' }), {
      target: { value: 'phim 13' },
    });
    expect(screen.getAllByRole('img', { name: /Poster Phim/ })).toHaveLength(1);
    expect(screen.getByText('1 kết quả')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Xem thêm/ })).not.toBeInTheDocument();
  });

  it('submits only after preflight is ready', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);
    const generate = screen.getByRole('button', { name: 'Tạo lịch tối ưu' });
    expect(generate).toBeEnabled();
    fireEvent.click(generate);
    expect(form.handleSubmit).toHaveBeenCalledTimes(1);
  });

  it('explains recreation semantics for authoritative tomorrow scheduling', () => {
    const draft = { cinemaPublicId: 'cinema-1', auditoriumPublicIds: ['aud-1'] };
    render(
      <MemoryRouter initialEntries={[{
        pathname: '/admin/showtime-schedules/create',
        state: { autoScheduleRecreate: { draft, sourceShortCode: 'ABC123' } },
      }]}
      >
        <AdminAutoScheduleCreatePage />
      </MemoryRouter>,
    );
    expect(useAutoScheduleForm).toHaveBeenCalledWith(expect.objectContaining({ initialDraft: draft }));
    expect(screen.getByRole('status')).toHaveTextContent('Đang tạo lại từ lịch ABC123');
    expect(screen.getByRole('status')).toHaveTextContent('từ ngày mai');
  });

  it('allows an explicit preflight retry from the one-screen flow', () => {
    const form = baseForm();
    useAutoScheduleForm.mockReturnValue(form);
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra lại' }));

    expect(form.runPreflight).toHaveBeenCalledTimes(1);
  });

  it('opens Advanced automatically when an advanced validation error exists', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(),
      errors: { previewTtlMinutes: 'Giá trị từ 5 đến 120' },
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByText('Nâng cao').closest('details')).toHaveAttribute('open');
    expect(screen.getByText('Giá trị từ 5 đến 120')).toBeInTheDocument();
  });

  it('disables retry and generation while preflight is running', () => {
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(), isCheckingPreflight: true, isReady: false,
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByRole('button', { name: 'Kiểm tra lại' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Tạo lịch tối ưu' })).toBeDisabled();
  });

  it('shows an honest animated waiting progress while the optimizer is running', () => {
    vi.useFakeTimers();
    useAutoScheduleForm.mockReturnValue({
      ...baseForm(), isSubmitting: true, isReady: false, planningDays: 7,
    });
    render(<MemoryRouter><AdminAutoScheduleCreatePage /></MemoryRouter>);

    expect(screen.getByRole('progressbar', { name: 'Tiến trình tạo lịch tối ưu' }))
      .toHaveAttribute('aria-valuetext', 'Đang gửi phạm vi và chuẩn bị dữ liệu tối ưu.');
    expect(screen.getByText('Hệ thống vẫn đang xử lý')).toBeInTheDocument();
    expect(screen.getByLabelText('Đã chờ 00:00')).toBeInTheDocument();
    expect(screen.getByText(/Phạm vi 7 ngày có thể mất vài phút/)).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(46_000));
    expect(screen.getByRole('progressbar', { name: 'Tiến trình tạo lịch tối ưu' }))
      .toHaveAttribute('aria-valuetext', 'Bộ tối ưu vẫn đang tính toán lịch phù hợp nhất.');
    expect(screen.getByLabelText('Đã chờ 00:46')).toBeInTheDocument();
  });
});
