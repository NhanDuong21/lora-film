import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { derivePreviewCapabilities } from '../utils/autoSchedulePreviewLifecycle';
import useAutoSchedulePreview from '../hooks/useAutoSchedulePreview';
import AdminAutoSchedulePreviewPage from './AdminAutoSchedulePreviewPage';

vi.mock('../hooks/useAutoSchedulePreview');

const candidate = (index = 1, overrides = {}) => ({
  itemPublicId: `item-${index}`,
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
  ...overrides,
});

const preview = (status = 'PREVIEWED', overrides = {}) => ({
  previewPublicId: 'preview-1',
  version: 3,
  status,
  timezoneSnapshot: 'UTC',
  cinemaName: 'Lora Cinema',
  scheduleFrom: '2026-07-24',
  scheduleTo: '2026-07-24',
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
    isUpdatingSelection: false,
    handleToggleSelection: vi.fn(),
    handleBulkSelection: vi.fn(),
    handleApply: vi.fn(),
    fetchPreview: vi.fn(),
  };
};

const renderPage = () => render(
  <MemoryRouter initialEntries={['/admin/showtime-schedules/preview-1']}>
    <Routes>
      <Route path="/admin/showtime-schedules/:id" element={<AdminAutoSchedulePreviewPage />} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminAutoSchedulePreviewPage Milestone B', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAutoSchedulePreview.mockReturnValue(hookValue());
  });

  it.each([
    'GENERATING',
    'PREVIEWED',
    'APPLYING',
    'APPLIED',
    'FAILED',
    'EXPIRED',
    'CANCELLED',
  ])('presents lifecycle %s truthfully', status => {
    const candidateState = status === 'APPLIED' ? 'CREATED' : 'PENDING';
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status,
      items: [candidate(1, { applyStatus: candidateState })],
    }));

    renderPage();

    expect(screen.getAllByText(status).length).toBeGreaterThan(0);
    if (status === 'PREVIEWED') {
      expect(screen.getByRole('button', { name: /Áp dụng \(1\)/i })).toBeEnabled();
      expect(screen.getByRole('tab', { name: /Đề xuất \(1\)/i })).toHaveAttribute('aria-selected', 'true');
    } else {
      expect(screen.queryByRole('button', { name: /Áp dụng \(/i })).not.toBeInTheDocument();
    }
    if (status === 'APPLIED') {
      expect(screen.getByRole('tab', { name: /Suất chiếu đã tạo \(1\)/i }))
        .toHaveAttribute('aria-selected', 'true');
    }
  });

  it('locks selection and apply while a replacement snapshot is incomplete', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({ isRefreshing: true }));
    renderPage();

    expect(screen.getByText('Đang làm mới ảnh chụp ứng viên')).toBeInTheDocument();
    expect(screen.getByText(/2\/4 trang · 200\/361 ứng viên/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Áp dụng \(1\)/i })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /Danh sách/i }));
    expect(screen.getByRole('checkbox', { name: /Chọn Phim 1/i })).toBeDisabled();
  });

  it('shows stale-snapshot recovery without replacing the previous candidates', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      snapshotError: {
        code: 'VERSION_MISMATCH',
        message: 'Dữ liệu bản xem trước đã thay đổi trong lúc tải.',
        blocksMutations: true,
      },
    }));
    renderPage();

    expect(screen.getByRole('alert')).toHaveTextContent('Không thể công bố ảnh chụp mới');
    expect(screen.getByText('Dữ liệu bản xem trước đã thay đổi trong lúc tải.')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Đề xuất \(1\)/i })).toBeInTheDocument();
  });

  it('maps all five exact candidate apply states and does not expose selection for outcomes', () => {
    const states = ['PENDING', 'CREATED', 'SKIPPED', 'CONFLICT', 'FAILED'];
    const items = states.map((applyStatus, index) => candidate(index + 1, {
      applyStatus,
      selected: applyStatus === 'PENDING',
    }));
    useAutoSchedulePreview.mockReturnValue(hookValue({ items, selectedIds: new Set(['item-1']) }));
    renderPage();

    fireEvent.click(screen.getByRole('tab', { name: /Tất cả ứng viên/i }));
    fireEvent.click(screen.getByRole('button', { name: /Danh sách/i }));

    expect(screen.getByText('Đang chờ')).toBeInTheDocument();
    expect(screen.getByText('Đã tạo suất chiếu')).toBeInTheDocument();
    expect(screen.getByText('Đã bỏ qua')).toBeInTheDocument();
    expect(screen.getByText('Xung đột khi áp dụng')).toBeInTheDocument();
    expect(screen.getByText('Áp dụng thất bại')).toBeInTheDocument();
    expect(screen.getAllByRole('checkbox')).toHaveLength(1);
  });

  it('renders no more than 100 candidate rows from a 3,615-item complete dataset', () => {
    const items = Array.from({ length: 3615 }, (_, index) => candidate(index, {
      selected: false,
      auditoriumName: `Phòng ${index % 10}`,
    }));
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status: 'CANCELLED',
      items,
      selectedIds: new Set(),
    }));
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Danh sách/i }));
    fireEvent.change(screen.getByRole('combobox', { name: 'Số ứng viên mỗi trang' }), {
      target: { value: '100' },
    });

    expect(screen.getAllByTestId('candidate-row')).toHaveLength(100);
    expect(screen.getByText('Trang 1/37 · 3615 ứng viên')).toBeInTheDocument();
  });

  it('resets and clamps the client page when a filter changes', () => {
    const items = Array.from({ length: 120 }, (_, index) => candidate(index, {
      selected: false,
      auditoriumName: index === 119 ? 'Phòng đặc biệt' : 'Phòng 1',
    }));
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status: 'CANCELLED',
      items,
      selectedIds: new Set(),
    }));
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Sau' }));
    expect(screen.getByText('Trang 2/3 · 120 ứng viên')).toBeInTheDocument();
    fireEvent.change(screen.getByRole('combobox', { name: 'Lọc phòng chiếu' }), {
      target: { value: 'Phòng đặc biệt' },
    });
    expect(screen.getByText('Trang 1/1 · 1 ứng viên')).toBeInTheDocument();
  });

  it('uses the complete candidate array for the manual non-overlap helper', () => {
    const first = candidate(1, { selected: true });
    const overlapping = candidate(2, {
      selected: false,
      startTime: '2026-07-24T11:05:00Z',
      endTime: '2026-07-24T12:05:00Z',
      occupancyEndTime: '2026-07-24T12:20:00Z',
    });
    const value = hookValue({ items: [first, overlapping], selectedIds: new Set(['item-1']) });
    useAutoSchedulePreview.mockReturnValue(value);
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /Chọn nhanh không trùng/i }));
    expect(value.handleBulkSelection).toHaveBeenCalledWith(['item-1']);
  });

  it('uses authoritative serviceDate for after-midnight filtering', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      items: [candidate(1, {
        serviceDate: '2026-07-24',
        startTime: '2026-07-25T00:30:00Z',
        endTime: '2026-07-25T01:30:00Z',
        occupancyEndTime: '2026-07-25T01:45:00Z',
      })],
    }));
    renderPage();

    expect(screen.getByRole('option', { name: '24/07/2026' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '25/07/2026' })).not.toBeInTheDocument();
  });

  it('shows the APPLIED empty state when no Showtime was created', () => {
    useAutoSchedulePreview.mockReturnValue(hookValue({
      status: 'APPLIED',
      items: [candidate(1, { applyStatus: 'SKIPPED', selected: false })],
      selectedIds: new Set(),
    }));
    renderPage();

    expect(screen.getByRole('tab', { name: /Suất chiếu đã tạo \(0\)/i }))
      .toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Không có suất chiếu nào được tạo từ bản xem trước này.')).toBeInTheDocument();
  });
});
