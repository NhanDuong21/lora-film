import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAdminShowtimes from '../hooks/useAdminShowtimes';
import AdminShowtimePage from './AdminShowtimePage';

vi.mock('../hooks/useAdminShowtimes');
vi.mock('../services/adminShowtimeService', () => ({ default: {} }));
vi.mock('../components/ShowtimeTable', () => ({
  default: props => (
    <div>
      <button type="button" onClick={props.onClearFilters}>Clear all</button>
      <button type="button" onClick={props.onClearBatch}>Clear batch</button>
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
});
