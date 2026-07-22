import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoSchedulePreview from '../hooks/useAutoSchedulePreview';
import AdminAutoSchedulePreviewPage from './AdminAutoSchedulePreviewPage';

vi.mock('../hooks/useAutoSchedulePreview');

const candidate = {
  itemPublicId: 'item-1',
  movieTitle: 'Phim thử nghiệm',
  movieVersionPublicId: 'version-1',
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
};

describe('AdminAutoSchedulePreviewPage manual helper', () => {
  const handleBulkSelection = vi.fn();
  let hookValue;
  const renderPage = () => render(
    <MemoryRouter initialEntries={['/admin/showtime-schedules/preview-1']}>
      <Routes>
        <Route path="/admin/showtime-schedules/:id" element={<AdminAutoSchedulePreviewPage />} />
      </Routes>
    </MemoryRouter>,
  );

  beforeEach(() => {
    vi.clearAllMocks();
    hookValue = {
      preview: {
        version: 3,
        status: 'PREVIEWED',
        timezoneSnapshot: 'UTC',
        cinemaName: 'Lora Cinema',
        scheduleFrom: '2026-07-24',
        scheduleTo: '2026-07-24',
        expiresAt: '2099-07-24T12:00:00Z',
        totalCandidateCount: 1,
        validCandidateCount: 1,
        rejectedCandidateCount: 0,
      },
      items: [candidate],
      isLoading: false,
      isApplying: false,
      isUpdatingSelection: false,
      selectedItemIds: new Set(['item-1']),
      handleToggleSelection: vi.fn(),
      handleBulkSelection,
      handleApply: vi.fn(),
      fetchPreview: vi.fn(),
    };
    useAutoSchedulePreview.mockReturnValue(hookValue);
  });

  afterEach(() => cleanup());

  it('presents the greedy helper as an explicit non-optimizer action', () => {
    renderPage();

    expect(handleBulkSelection).not.toHaveBeenCalled();
    const button = screen.getByRole('button', { name: /CHỌN NHANH KHÔNG TRÙNG/i });
    expect(button).toHaveAttribute(
      'title',
      'Chọn lại theo giờ bắt đầu sớm nhất và khoảng chiếm phòng; thao tác này có thể thay thế đề xuất tối ưu ban đầu.',
    );

    fireEvent.click(button);
    expect(handleBulkSelection).toHaveBeenCalledWith(['item-1']);
  });

  it('shows deterministic fallback and malformed occupancy warnings without crashing', () => {
    useAutoSchedulePreview.mockReturnValue({
      ...hookValue,
      preview: {
        ...hookValue.preview,
        timezoneSnapshot: 'Not/A_Timezone',
      },
      items: [{ ...candidate, occupancyEndTime: null }],
    });

    renderPage();

    expect(screen.getByText('Múi giờ bản xem trước không hợp lệ')).toBeInTheDocument();
    expect(screen.getByText('Thiếu dữ liệu chiếm phòng')).toBeInTheDocument();
    expect(screen.getByText(/hiển thị tạm thời theo UTC/)).toBeInTheDocument();
  });

  it('groups and filters an after-midnight item by authoritative service date', () => {
    useAutoSchedulePreview.mockReturnValue({
      ...hookValue,
      items: [{
        ...candidate,
        serviceDate: '2026-07-24',
        startTime: '2026-07-25T00:30:00Z',
        endTime: '2026-07-25T01:30:00Z',
        occupancyEndTime: '2026-07-25T01:45:00Z',
      }],
    });

    renderPage();

    expect(screen.getByRole('option', { name: '24/07/2026' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: '25/07/2026' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /DANH SÁCH/i }));
    expect(screen.getByRole('heading', { name: /24\/07\/2026/ })).toBeInTheDocument();
    expect(screen.getByText('00:30')).toBeInTheDocument();
  });

  it('keeps legacy items visible and filterable without inventing a timeline date', () => {
    useAutoSchedulePreview.mockReturnValue({
      ...hookValue,
      items: [{ ...candidate, serviceDate: null }],
    });

    renderPage();

    expect(screen.getByRole('option', { name: 'Không xác định ngày vận hành' }))
      .toBeInTheDocument();
    expect(screen.queryByText(candidate.movieTitle)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Xem danh sách' }));
    expect(screen.getByText(candidate.movieTitle)).toBeInTheDocument();
    expect(screen.getByText('Dữ liệu lịch sử')).toBeInTheDocument();
  });
});
