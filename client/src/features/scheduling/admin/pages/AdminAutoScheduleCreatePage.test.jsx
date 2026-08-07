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
    publicId: 'movie-1', title: 'Phim A',
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
    fireEvent.click(screen.getAllByText('Ghim dùng')[0]);
    expect(form.setScopeChoice).toHaveBeenCalledWith('include', 'auditorium:aud-1', true);
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
