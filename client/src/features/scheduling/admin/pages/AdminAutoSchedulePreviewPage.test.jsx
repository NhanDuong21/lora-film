import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { derivePreviewCapabilities } from '../utils/autoSchedulePreviewLifecycle';
import useAutoSchedulePreview from '../hooks/useAutoSchedulePreview';
import AdminAutoSchedulePreviewPage from './AdminAutoSchedulePreviewPage';

vi.mock('../hooks/useAutoSchedulePreview');

const candidate = (index = 1, overrides = {}) => ({
  itemPublicId: `item-${index}`,
  moviePublicId: `movie-${index}`,
  movieTitle: `Phim ${index}`,
  movieVersionPublicId: `version-${index}`,
  versionName: '2D',
  format: '2D',
  audioLanguage: 'vi',
  auditoriumPublicId: 'aud-1',
  auditoriumName: 'Phòng 1',
  serviceDate: '2026-07-24',
  startTime: '2026-07-24T10:00:00Z',
  endTime: '2026-07-24T11:00:00Z',
  occupancyEndTime: '2026-07-24T11:15:00Z',
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  selected: true,
  score: 90 - index,
  rankingPosition: index,
  ...overrides,
});

const preview = (status = 'PREVIEWED', overrides = {}) => ({
  previewPublicId: 'preview-1',
  version: 3,
  status,
  timezoneSnapshot: 'UTC',
  cinemaName: 'Lora Cinema',
  scheduleFrom: '2026-07-24',
  scheduleTo: '2026-07-26',
  generatedAt: '2026-07-23T10:00:00Z',
  expiresAt: '2099-07-24T12:00:00Z',
  applyMode: 'ALL_OR_NOTHING',
  totalCandidateCount: 1,
  validCandidateCount: 1,
  rejectedCandidateCount: 0,
  ...overrides,
});

const hookValue = ({
  status = 'PREVIEWED',
  previewOverrides = {},
  items = [candidate()],
  selectedIds = new Set(items.filter(item => item.selected).map(item => item.itemPublicId)),
  isRefreshing = false,
  isUpdatingSelection = false,
  snapshotError = null,
} = {}) => {
  const previewValue = preview(status, {
    totalCandidateCount: items.length,
    validCandidateCount: items.filter(item => item.validationStatus === 'VALID').length,
    rejectedCandidateCount: items.filter(item => item.validationStatus !== 'VALID').length,
    ...previewOverrides,
  });
  const capabilities = derivePreviewCapabilities(previewValue, {
    selectedCount: selectedIds.size,
    isSnapshotUpdating: isRefreshing,
    isUpdatingSelection,
    hasUnsafeSnapshot: Boolean(snapshotError?.blocksMutations),
  });

  return {
    preview: previewValue,
    items,
    selectedItemIds: selectedIds,
    expectedVersion: 3,
    isLoading: false,
    isRefreshing,
    isSnapshotUpdating: isRefreshing,
    loadingProgress: isRefreshing
      ? { loadedPages: 2, totalPages: 4, loadedItems: 200, totalItems: 361 }
      : { loadedPages: 1, totalPages: 1, loadedItems: items.length, totalItems: items.length },
    snapshotError,
    capabilities,
    isApplying: false,
    isUpdatingSelection,
    handleToggleSelection: vi.fn(),
    handleBulkSelection: vi.fn(),
    handleApply: vi.fn(),
    fetchPreview: vi.fn(),
  };
};

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="location">{location.pathname}{location.search}</span>;
}

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/showtime-schedules/preview-1']}>
    <Routes>
      <Route path="/admin/showtime-schedules/:id" element={<><AdminAutoSchedulePreviewPage /><LocationProbe /></>} />
      <Route path="*" element={<LocationProbe />} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminAutoSchedulePreviewPage Milestone C', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAutoSchedulePreview.mockReturnValue(hookValue());
  });

  it('defaults to the earliest service date containing a selected recommendation', () => {
    const items = [
      candidate(1, { serviceDate: '2026-07-26', startTime: '2026-07-26T10:00:00Z', endTime: '2026-07-26T11:00:00Z', occupancyEndTime: '2026-07-26T11:15:00Z', selected: false }),
      candidate(2, { serviceDate: '2026-07-25', startTime: '2026-07-25T10:00:00Z', endTime: '2026-07-25T11:00:00Z', occupancyEndTime: '2026-07-25T11:15:00Z' }),
      candidate(3, { serviceDate: '2026-07-24', startTime: '2026-07-24T10:00:00Z', endTime: '2026-07-24T11:00:00Z', occupancyEndTime: '2026-07-24T11:15:00Z', selected: false }),
    ];
    useAutoSchedulePreview.mockReturnValue(hookValue({ items, selectedIds: new Set(['item-2']) }));
    renderPage();

    expect(screen.getByRole('button', { name: '25/07/2026' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('timeline-candidate-item-2')).toBeInTheDocument();
    expect(screen.queryByTestId('timeline-candidate-item-1')).not.toBeInTheDocument();
    expect(screen.getByText('Tạo lúc 10:00 23/07/2026')).toBeInTheDocument();
    expect(screen.getByText(/Mã rút gọn:/)).toHaveTextContent('PREVIEW1');
    expect(screen.getByText('previewPublicId: preview-1')).toBeInTheDocument();
  });

  it('uses authoritative serviceDate for after-midnight candidates and extends the axis beyond 24:00', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      items: [candidate(1, {
        serviceDate: '2026-07-24',
        startTime: '2026-07-25T00:30:00Z',
        endTime: '2026-07-25T01:30:00Z',
        occupancyEndTime: '2026-07-25T01:45:00Z',
      })],
    }));
    renderPage();

    expect(screen.getByRole('button', { name: '24/07/2026' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByRole('button', { name: '25/07/2026' })).not.toBeInTheDocument();
    expect(screen.getByText('25:00')).toBeInTheDocument();
    expect(screen.getByText('26:00')).toBeInTheDocument();
  });

  it('shows exactly one diagnostic overlay, switches to its service date, and does not mutate selection', () => {
    const items = [
      candidate(1),
      candidate(2, {
        serviceDate: '2026-07-25',
        startTime: '2026-07-25T10:00:00Z',
        endTime: '2026-07-25T11:00:00Z',
        occupancyEndTime: '2026-07-25T11:15:00Z',
        selected: false,
        validationStatus: 'REJECTED',
        rejectionCode: 'AUDITORIUM_UNAVAILABLE',
      }),
      candidate(3, {
        serviceDate: '2026-07-26',
        startTime: '2026-07-26T10:00:00Z',
        endTime: '2026-07-26T11:00:00Z',
        occupancyEndTime: '2026-07-26T11:15:00Z',
        selected: false,
        validationStatus: 'REJECTED',
      }),
    ];
    const value = hookValue({ items, selectedIds: new Set(['item-1']) });
    useAutoSchedulePreview.mockReturnValue(value);
    renderPage();

    fireEvent.click(screen.getByRole('tab', { name: /Không hợp lệ \/ xung đột \(2\)/i }));
    const overlayActions = screen.getAllByRole('button', { name: 'Xem trên timeline' });
    fireEvent.click(overlayActions[0]);
    expect(screen.getByRole('button', { name: '25/07/2026' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('timeline-candidate-item-2')).toHaveAttribute('data-diagnostic', 'true');
    expect(screen.getByTestId('timeline-boundary-evidence')).toHaveTextContent('0 đề xuất đã chọn + 1 phủ chẩn đoán');

    fireEvent.click(overlayActions[1]);
    expect(screen.getByRole('button', { name: '26/07/2026' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByTestId('timeline-candidate-item-2')).not.toBeInTheDocument();
    expect(screen.getByTestId('timeline-candidate-item-3')).toHaveAttribute('data-diagnostic', 'true');
    expect(value.handleToggleSelection).not.toHaveBeenCalled();
    expect(value.handleBulkSelection).not.toHaveBeenCalled();
  });

  it('detects a diagnostic occupancy conflict from the complete snapshot', () => {
    const items = [
      candidate(1, { occupancyEndTime: '2026-07-24T11:30:00Z' }),
      candidate(2, {
        selected: false,
        validationStatus: 'REJECTED',
        startTime: '2026-07-24T11:10:00Z',
        endTime: '2026-07-24T12:10:00Z',
        occupancyEndTime: '2026-07-24T12:25:00Z',
      }),
    ];
    useAutoSchedulePreview.mockReturnValue(hookValue({ items, selectedIds: new Set(['item-1']) }));
    renderPage();

    fireEvent.click(screen.getByRole('tab', { name: /Không hợp lệ \/ xung đột/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Xem trên timeline' }));
    expect(screen.getByRole('status')).toHaveTextContent('xung đột khoảng chiếm phòng');
    expect(screen.getByTestId('timeline-boundary-evidence')).toHaveTextContent('dữ liệu đầy đủ 2 ứng viên');
  });

  it('sorts concise rows by selected state before date, room, local start, rank, and stable ID', () => {
    const items = [
      candidate(3, { selected: false, serviceDate: '2026-07-24', startTime: '2026-07-24T09:00:00Z', endTime: '2026-07-24T10:00:00Z', occupancyEndTime: '2026-07-24T10:15:00Z', rankingPosition: 1 }),
      candidate(2, { selected: true, serviceDate: '2026-07-25', startTime: '2026-07-25T10:00:00Z', endTime: '2026-07-25T11:00:00Z', occupancyEndTime: '2026-07-25T11:15:00Z', rankingPosition: 3 }),
      candidate(1, { selected: true, serviceDate: '2026-07-24', startTime: '2026-07-24T10:00:00Z', endTime: '2026-07-24T11:00:00Z', occupancyEndTime: '2026-07-24T11:15:00Z', rankingPosition: 2 }),
    ];
    useAutoSchedulePreview.mockReturnValue(hookValue({ status: 'CANCELLED', items, selectedIds: new Set(['item-1', 'item-2']) }));
    renderPage();

    const rows = screen.getAllByTestId('candidate-row');
    expect(within(rows[0]).getByText('Phim 1')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Phim 2')).toBeInTheDocument();
    expect(within(rows[2]).getByText('Phim 3')).toBeInTheDocument();
    expect(within(rows[0]).getByText((_, element) => element?.tagName === 'TD' && element.textContent === '89 / 2')).toBeInTheDocument();
    expect(within(rows[0]).getByText('Dữ liệu mở rộng')).toBeInTheDocument();
    expect(screen.getByText(/Điểm ưu tiên là tổng các thành phần/)).toHaveTextContent('điểm cao hơn tốt hơn');
    expect(screen.getByText(/Điểm ưu tiên là tổng các thành phần/)).toHaveTextContent('Hạng chỉ là thứ tự hiển thị, không phải thứ tự chọn');
    expect(screen.getByText(/Điểm ưu tiên là tổng các thành phần/)).toHaveTextContent('hạng thấp hơn vẫn có thể được chọn');
  });

  it('renders no more than 100 candidate rows from a 3,615-item complete dataset', () => {
    const items = Array.from({ length: 3615 }, (_, index) => candidate(index, {
      selected: false,
      auditoriumPublicId: `aud-${index % 10}`,
      auditoriumName: `Phòng ${index % 10}`,
    }));
    useAutoSchedulePreview.mockReturnValue(hookValue({ status: 'CANCELLED', items, selectedIds: new Set() }));
    renderPage();

    fireEvent.change(screen.getByRole('combobox', { name: 'Số ứng viên mỗi trang' }), { target: { value: '100' } });
    expect(screen.getAllByTestId('candidate-row')).toHaveLength(100);
    expect(screen.getByText('Trang 1/37 · 3615 ứng viên')).toBeInTheDocument();
    expect(screen.getByTestId('timeline-boundary-evidence')).toHaveTextContent('Timeline: 0 đề xuất đã chọn + 0 phủ chẩn đoán');
  });

  it('opens an accessible drawer from a semantic timeline button and restores focus on close', async () => {
    renderPage();
    const timelineButton = screen.getByRole('button', { name: /Phim 1.*Mở chi tiết/i });
    timelineButton.focus();
    fireEvent.click(timelineButton);

    expect(screen.getByRole('dialog', { name: 'Phim 1' })).toBeInTheDocument();
    const closeButton = screen.getByRole('button', { name: 'Đóng chi tiết ứng viên' });
    await waitFor(() => expect(closeButton).toHaveFocus());
    fireEvent.click(closeButton);
    await waitFor(() => expect(timelineButton).toHaveFocus());
  });

  it('retains Milestone B lifecycle defaults and mutation locking during snapshot refresh', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({ isRefreshing: true }));
    renderPage();

    expect(screen.getByText('Đang làm mới ảnh chụp ứng viên')).toBeInTheDocument();
    expect(screen.getByText(/2\/4 trang · 200\/361 ứng viên/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Áp dụng \(1\)/i })).toBeDisabled();
    expect(screen.getByRole('tab', { name: /Đề xuất \(1\)/i })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('checkbox', { name: /Chọn Phim 1/i })).toBeDisabled();
  });

  it('uses the non-conflicting auto-selection wording for its action and loading state', () => {
    const value = hookValue();
    useAutoSchedulePreview.mockReturnValue(value);
    const { unmount } = renderPage();

    const action = screen.getByRole('button', { name: 'Tự chọn lịch không xung đột' });
    fireEvent.click(action);
    expect(value.handleBulkSelection).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Chọn nhanh không trùng')).not.toBeInTheDocument();
    unmount();

    useAutoSchedulePreview.mockReturnValue(hookValue({ isUpdatingSelection: true }));
    renderPage();
    expect(screen.getByRole('button', { name: 'Đang tự chọn lịch không xung đột' }))
      .toHaveTextContent('Đang tự chọn lịch không xung đột…');
  });

  it('defaults an applied preview to created Showtimes and remains read-only', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status: 'APPLIED',
      items: [candidate(1, { applyStatus: 'CREATED', createdShowtimePublicId: 'showtime-1' })],
    }));
    renderPage();

    expect(screen.getByRole('tab', { name: /Suất chiếu đã tạo \(1\)/i })).toHaveAttribute('aria-selected', 'true');
    expect(screen.queryByRole('button', { name: /Áp dụng \(/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('emphasizes APPLIED metadata and links only CREATED items to operational Showtimes', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status: 'APPLIED',
      previewOverrides: { appliedAt: '2026-07-24T18:30:00Z' },
      items: [
        candidate(1, { applyStatus: 'CREATED', createdShowtimePublicId: 'showtime-1' }),
        candidate(2, { applyStatus: 'SKIPPED', createdShowtimePublicId: 'should-not-link', selected: false }),
      ],
      selectedIds: new Set(['item-1']),
    }));
    renderPage();

    expect(screen.getByText('Đã áp dụng lúc')).toBeInTheDocument();
    expect(screen.getAllByText('1 suất chiếu đã tạo').length).toBeGreaterThan(0);
    const batchLink = screen.getByRole('link', { name: 'Xem các suất chiếu đã tạo' });
    expect(batchLink).toHaveAttribute('href', '/admin/showtimes?source=AUTO&batchId=preview-1');
    expect(batchLink).not.toHaveAttribute('href', expect.stringContaining('status=DRAFT'));

    fireEvent.click(screen.getByRole('tab', { name: /Tất cả ứng viên \(2\)/i }));
    expect(screen.getByRole('link', { name: 'Mở suất chiếu showtime-1' }))
      .toHaveAttribute('href', '/admin/showtimes/showtime-1');
    expect(screen.queryByText('should-not-link')).not.toBeInTheDocument();
  });

  it('navigates after apply without forcing the DRAFT status filter', () => {
    let hookOptions;
    useAutoSchedulePreview.mockImplementation((_, options) => {
      hookOptions = options;
      return hookValue();
    });
    renderPage();

    act(() => hookOptions.onSuccess({ createdShowtimeCount: 1, skippedItemCount: 0 }));
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/showtimes?source=AUTO&batchId=preview-1',
    );
    expect(screen.getByTestId('location')).not.toHaveTextContent('status=DRAFT');
  });

  it('shows the complete apply confirmation and keeps the dialog open while the request fails', async () => {
    const value = hookValue({
      items: [
        candidate(1),
        candidate(2, {
          selected: false,
          auditoriumPublicId: 'aud-2',
          auditoriumName: 'Phòng 2',
          validationStatus: 'REJECTED',
        }),
      ],
      selectedIds: new Set(['item-1']),
    });
    value.handleApply.mockResolvedValue(null);
    useAutoSchedulePreview.mockReturnValue(value);
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Áp dụng \(1\)/i }));
    const dialog = screen.getByRole('dialog', { name: 'Xác nhận áp dụng lịch chiếu' });
    expect(dialog).toHaveTextContent('Lora Cinema');
    expect(dialog).toHaveTextContent('24/07/2026 – 26/07/2026');
    expect(dialog).toHaveTextContent('Bản nháp');
    expect(dialog).toHaveTextContent('Tất cả hoặc không tạo');
    expect(dialog).toHaveTextContent('Không hợp lệ / xung đột toàn bản');
    expect(dialog).toHaveTextContent('lần thử lại dùng cùng khóa an toàn');

    await act(async () => fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận' })));
    expect(value.handleApply).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('dialog', { name: 'Xác nhận áp dụng lịch chiếu' })).toBeInTheDocument();
  });
});
