import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAdminShowtimes from '../hooks/useAdminShowtimes';
import adminShowtimeService from '../services/adminShowtimeService';
import { writeBatchReadinessCache } from '../utils/batchReadinessCache';
import AdminShowtimePage from './AdminShowtimePage';

vi.mock('../hooks/useAdminShowtimes');
vi.mock('../services/adminShowtimeService', () => ({
  default: {
    previewBatchStatus: vi.fn(),
    transitionBatchStatus: vi.fn(),
  },
}));
vi.mock('../components/ShowtimeTable', () => ({
  default: props => (
    <div>
      <button type="button" onClick={props.onClearFilters}>Clear all</button>
      <button type="button" onClick={props.onClearBatch}>Clear batch</button>
      <button type="button" onClick={props.onOpenBatch}>Open batch</button>
      <span data-testid="cached-ready-count">{props.batchReadiness?.eligibleCount ?? 'none'}</span>
      <span data-testid="readiness-loading">{String(props.isBatchReadinessLoading)}</span>
    </div>
  ),
}));

const hookValue = () => ({
  showtimes: [],
  cinemas: [],
  movies: [],
  isLoading: false,
  isOptionsLoading: false,
  cinemaSlug: '',
  setCinemaSlug: vi.fn(),
  movieSlug: '',
  setMovieSlug: vi.fn(),
  date: '',
  setDate: vi.fn(),
  status: '',
  setStatus: vi.fn(),
  batchId: '',
  setBatchId: vi.fn(),
  source: '',
  setSource: vi.fn(),
  currentPage: 0,
  setCurrentPage: vi.fn(),
  pageSize: 25,
  setPageSize: vi.fn(),
  totalPages: 0,
  totalElements: 0,
  fetchShowtimes: vi.fn(),
});

function LocationControls() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <>
      <span data-testid="location">{location.pathname}{location.search}</span>
      <button type="button" onClick={() => navigate('/admin/showtimes?source=AUTO&batchId=preview-2')}>Next batch</button>
    </>
  );
}

const renderPage = initialEntry => render(
  <MemoryRouter initialEntries={[initialEntry]}>
    <Routes>
      <Route path="/admin/showtimes" element={<><AdminShowtimePage /><LocationControls /></>} />
    </Routes>
  </MemoryRouter>,
);

describe('AdminShowtimePage URL-backed batch context', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.sessionStorage.clear();
  });

  it('synchronizes source/batch on initial load and later URL changes', async () => {
    const value = hookValue();
    useAdminShowtimes.mockReturnValue(value);
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1&cinemaSlug=lora-cinema&date=2026-08-04');

    await waitFor(() => expect(value.setBatchId).toHaveBeenCalledWith('preview-1'));
    expect(value.setSource).toHaveBeenCalledWith('AUTO');
    expect(value.setCinemaSlug).toHaveBeenCalledWith('lora-cinema');
    expect(value.setDate).toHaveBeenCalledWith('2026-08-04');
    expect(value.fetchShowtimes).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole('button', { name: 'Next batch' }));
    await waitFor(() => expect(value.setBatchId).toHaveBeenCalledWith('preview-2'));
    expect(value.setSource).toHaveBeenLastCalledWith('AUTO');
  });

  it('removes source, batch, and URL status through clear-filter behavior', async () => {
    const value = hookValue();
    useAdminShowtimes.mockReturnValue(value);
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1&status=DRAFT&cinemaSlug=lora-cinema&date=2026-08-04');
    await waitFor(() => expect(value.setStatus).toHaveBeenCalledWith('DRAFT'));

    fireEvent.click(screen.getByRole('button', { name: 'Clear all' }));
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtimes'));
    expect(screen.getByTestId('location').textContent).not.toContain('?');
    expect(value.setBatchId).toHaveBeenLastCalledWith('');
    expect(value.setSource).toHaveBeenLastCalledWith('');
    expect(value.setStatus).toHaveBeenLastCalledWith('');
    expect(value.setCinemaSlug).toHaveBeenLastCalledWith('');
    expect(value.setDate).toHaveBeenLastCalledWith('');
  });

  it('uses server preflight and result counts without window.confirm or batch deletion', async () => {
    const value = { ...hookValue(), batchId: 'preview-1', source: 'AUTO' };
    useAdminShowtimes.mockReturnValue(value);
    const preflight = {
      batchId: 'preview-1',
      targetStatus: 'OPEN_FOR_BOOKING',
      totalCount: 25,
      eligibleCount: 20,
      alreadyTargetCount: 5,
      skippedCount: 0,
      failedCount: 0,
      affectedCount: 0,
      atomic: true,
      actionAllowed: true,
      reasonGroups: [],
    };
    adminShowtimeService.previewBatchStatus.mockResolvedValue({ success: true, data: preflight });
    adminShowtimeService.transitionBatchStatus.mockResolvedValue({
      success: true,
      data: { ...preflight, affectedCount: 20 },
    });
    const confirmSpy = vi.spyOn(window, 'confirm');
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    await waitFor(() => expect(adminShowtimeService.previewBatchStatus).toHaveBeenCalledWith(
      'preview-1',
      'OPEN_FOR_BOOKING',
    ));
    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));
    expect(await screen.findByRole('dialog', { name: 'Bạn sắp mở bán 20 suất chiếu' })).toHaveTextContent('25');
    expect(screen.getByRole('dialog')).toHaveTextContent('20');
    fireEvent.click(screen.getByRole('button', { name: 'Mở bán 20 suất' }));

    await waitFor(() => expect(adminShowtimeService.transitionBatchStatus).toHaveBeenCalledWith(
      'preview-1',
      { status: 'OPEN_FOR_BOOKING' },
    ));
    expect(await screen.findByRole('dialog', { name: 'Kết quả mở bán lịch chiếu' })).toHaveTextContent('20');
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminShowtimeService.deleteBatch).toBeUndefined();
    confirmSpy.mockRestore();
  });

  it('restores the latest successful readiness immediately and refreshes it in the background', async () => {
    const value = { ...hookValue(), batchId: 'preview-1', source: 'AUTO' };
    useAdminShowtimes.mockReturnValue(value);
    const cached = {
      batchId: 'preview-1',
      targetStatus: 'OPEN_FOR_BOOKING',
      totalCount: 84,
      eligibleCount: 84,
      alreadyTargetCount: 0,
      skippedCount: 0,
      atomic: true,
      actionAllowed: true,
      reasonGroups: [],
    };
    writeBatchReadinessCache('preview-1', cached);
    adminShowtimeService.previewBatchStatus.mockImplementation(() => new Promise(() => {}));

    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    expect(screen.getByTestId('cached-ready-count')).toHaveTextContent('84');
    await waitFor(() => expect(screen.getByTestId('readiness-loading')).toHaveTextContent('true'));
    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));
    expect(screen.getByRole('dialog', { name: 'Bạn sắp mở bán 84 suất chiếu' })).toBeInTheDocument();
  });

  it('blocks partial batch opening when preflight reports skipped items', async () => {
    useAdminShowtimes.mockReturnValue({ ...hookValue(), batchId: 'preview-1', source: 'AUTO' });
    adminShowtimeService.previewBatchStatus.mockResolvedValue({
      success: true,
      data: {
        batchId: 'preview-1',
        targetStatus: 'OPEN_FOR_BOOKING',
        totalCount: 2,
        eligibleCount: 1,
        alreadyTargetCount: 0,
        skippedCount: 1,
        failedCount: 0,
        affectedCount: 0,
        atomic: true,
        actionAllowed: false,
        reasonGroups: [{
          reasonCode: 'PRICING_INCOMPLETE',
          reason: 'Showtime pricing is incomplete',
          count: 1,
        }],
      },
    });
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));
    expect(await screen.findByText('1 suất · Chưa có bảng giá đầy đủ')).toBeInTheDocument();
    expect(await screen.findByText('Không thể mở bán một phần. Không có suất chiếu nào được thay đổi.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mở bán 1 suất' })).toBeDisabled();
    expect(adminShowtimeService.transitionBatchStatus).not.toHaveBeenCalled();
  });

  it('uses a Vietnamese fallback when no known blocker code is returned', async () => {
    useAdminShowtimes.mockReturnValue({ ...hookValue(), batchId: 'preview-1', source: 'AUTO' });
    adminShowtimeService.previewBatchStatus.mockResolvedValue({
      success: true,
      data: {
        batchId: 'preview-1',
        targetStatus: 'OPEN_FOR_BOOKING',
        totalCount: 1,
        eligibleCount: 0,
        alreadyTargetCount: 0,
        skippedCount: 1,
        failedCount: 0,
        affectedCount: 0,
        atomic: true,
        actionAllowed: false,
        reasonGroups: [{ reasonCode: null, reason: null, count: 1 }],
      },
    });
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));

    expect(await screen.findByText(
      '1 suất · Chưa xác định nguyên nhân — vui lòng kiểm tra danh sách suất',
    )).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mở bán 0 suất' })).toBeDisabled();
  });

  it('disables confirmation when no showtime is eligible even if actionAllowed is inconsistent', async () => {
    useAdminShowtimes.mockReturnValue({ ...hookValue(), batchId: 'preview-1', source: 'AUTO' });
    adminShowtimeService.previewBatchStatus.mockResolvedValue({
      success: true,
      data: {
        batchId: 'preview-1',
        targetStatus: 'OPEN_FOR_BOOKING',
        totalCount: 0,
        eligibleCount: 0,
        alreadyTargetCount: 0,
        skippedCount: 0,
        failedCount: 0,
        affectedCount: 0,
        atomic: true,
        actionAllowed: true,
        reasonGroups: [],
      },
    });
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));

    expect(await screen.findByRole('button', { name: 'Mở bán 0 suất' })).toBeDisabled();
    expect(adminShowtimeService.transitionBatchStatus).not.toHaveBeenCalled();
  });
});
