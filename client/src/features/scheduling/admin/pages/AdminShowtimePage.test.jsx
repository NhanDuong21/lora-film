import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAdminShowtimes from '../hooks/useAdminShowtimes';
import adminShowtimeService from '../services/adminShowtimeService';
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
      <button type="button" onClick={() => props.onTransitionBatch('OPEN_FOR_BOOKING')}>Open batch</button>
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
  pageSize: 10,
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
  beforeEach(() => vi.clearAllMocks());

  it('synchronizes source/batch on initial load and later URL changes', async () => {
    const value = hookValue();
    useAdminShowtimes.mockReturnValue(value);
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    await waitFor(() => expect(value.setBatchId).toHaveBeenCalledWith('preview-1'));
    expect(value.setSource).toHaveBeenCalledWith('AUTO');

    fireEvent.click(screen.getByRole('button', { name: 'Next batch' }));
    await waitFor(() => expect(value.setBatchId).toHaveBeenCalledWith('preview-2'));
    expect(value.setSource).toHaveBeenLastCalledWith('AUTO');
  });

  it('removes source, batch, and URL status through clear-filter behavior', async () => {
    const value = hookValue();
    useAdminShowtimes.mockReturnValue(value);
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1&status=DRAFT');
    await waitFor(() => expect(value.setStatus).toHaveBeenCalledWith('DRAFT'));

    fireEvent.click(screen.getByRole('button', { name: 'Clear all' }));
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/admin/showtimes'));
    expect(screen.getByTestId('location').textContent).not.toContain('?');
    expect(value.setBatchId).toHaveBeenLastCalledWith('');
    expect(value.setSource).toHaveBeenLastCalledWith('');
    expect(value.setStatus).toHaveBeenLastCalledWith('');
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

    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));
    expect(await screen.findByRole('dialog', { name: 'Xác nhận mở bán toàn bộ' })).toHaveTextContent('25');
    expect(screen.getByRole('dialog')).toHaveTextContent('20');
    fireEvent.click(screen.getByRole('button', { name: 'Mở bán toàn bộ' }));

    await waitFor(() => expect(adminShowtimeService.transitionBatchStatus).toHaveBeenCalledWith(
      'preview-1',
      { status: 'OPEN_FOR_BOOKING' },
    ));
    expect(await screen.findByRole('dialog', { name: 'Kết quả mở bán toàn bộ' })).toHaveTextContent('20');
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(adminShowtimeService.deleteBatch).toBeUndefined();
    confirmSpy.mockRestore();
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
        reasonGroups: [{ reasonCode: 'SHOWTIME_PRICE_MISSING', reason: 'Thiếu giá', count: 1 }],
      },
    });
    renderPage('/admin/showtimes?source=AUTO&batchId=preview-1');

    fireEvent.click(screen.getByRole('button', { name: 'Open batch' }));
    expect(await screen.findByText('Không thể mở bán một phần. Không có suất chiếu nào được thay đổi.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mở bán toàn bộ' })).toBeDisabled();
    expect(adminShowtimeService.transitionBatchStatus).not.toHaveBeenCalled();
  });
});
