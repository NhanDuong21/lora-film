import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import {
  MemoryRouter,
  Outlet,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminGenrePage from './AdminGenrePage';
import adminGenreService from '../services/adminGenreService';

vi.mock('../services/adminGenreService', () => ({
  default: {
    getAllGenres: vi.fn(),
    createGenre: vi.fn(),
    updateGenre: vi.fn(),
    deleteGenre: vi.fn(),
  },
}));

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="current-location">{location.pathname}{location.search}</span>;
}

const renderPage = (contextOverrides = {}) => {
  const context = {
    triggerToast: vi.fn(),
    triggerConfirm: vi.fn().mockResolvedValue(true),
    ...contextOverrides,
  };

  render(
    <MemoryRouter initialEntries={['/admin/genres']}>
      <Routes>
        <Route element={<Outlet context={context} />}>
          <Route
            path="*"
            element={(
              <>
                <AdminGenrePage />
                <LocationProbe />
              </>
            )}
          />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  return context;
};

describe('AdminGenrePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminGenreService.getAllGenres.mockResolvedValue({
      success: true,
      data: {
        content: [{
          publicId: 'genre-1',
          name: 'Hành động',
          status: 'ACTIVE',
          movieCount: 12,
        }],
      },
    });
    adminGenreService.updateGenre.mockResolvedValue({ success: true });
    adminGenreService.deleteGenre.mockResolvedValue({ success: true });
  });

  it('uses publicId when editing a genre', async () => {
    renderPage();

    await screen.findByTestId('genre-name-genre-1');
    fireEvent.click(screen.getByTestId('edit-genre-genre-1'));
    fireEvent.change(screen.getByTestId('genre-name-input'), { target: { value: 'Hành động mới' } });
    fireEvent.click(screen.getByTestId('genre-submit-btn'));

    await waitFor(() => {
      expect(adminGenreService.updateGenre).toHaveBeenCalledWith('genre-1', { name: 'Hành động mới' });
    });
  });

  it('opens a focused add form from the empty-state action', async () => {
    adminGenreService.getAllGenres.mockResolvedValue({ success: true, data: { content: [] } });
    renderPage();

    await screen.findByText('Chưa có thể loại nào.');
    fireEvent.click(screen.getByText('Thêm thể loại đầu tiên'));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByTestId('genre-name-input')).toHaveValue('');
  });

  it('shows usage and status so operators can understand the impact', async () => {
    renderPage();

    expect(await screen.findByText('12 phim')).toBeInTheDocument();
    expect(screen.getAllByText('Đang sử dụng').length).toBeGreaterThan(0);
    expect(screen.getByTestId('delete-genre-genre-1')).toBeDisabled();

    fireEvent.click(screen.getByRole('button', {
      name: 'Xem 12 phim đang dùng thể loại Hành động',
    }));

    await waitFor(() => {
      expect(screen.getByTestId('current-location')).toHaveTextContent(
        '/admin/movies?status=ALL&genrePublicId=genre-1',
      );
    });
  });

  it('confirms before stopping a genre and preserves its name in the update', async () => {
    const context = renderPage();

    await screen.findByTestId('genre-name-genre-1');
    fireEvent.click(screen.getByTestId('toggle-genre-genre-1'));

    await waitFor(() => {
      expect(context.triggerConfirm).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Ngừng sử dụng “Hành động”?',
        confirmLabel: 'Ngừng sử dụng',
      }));
      expect(adminGenreService.updateGenre).toHaveBeenCalledWith('genre-1', {
        name: 'Hành động',
        status: 'INACTIVE',
      });
    });
  });

  it('warns and blocks genre names that only differ by the Phim prefix', async () => {
    renderPage();

    await screen.findByTestId('genre-name-genre-1');
    fireEvent.click(screen.getByTestId('create-genre-btn'));
    fireEvent.change(screen.getByTestId('genre-name-input'), {
      target: { value: 'Phim Hành Động' },
    });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Tên này có thể trùng với thể loại “Hành động”',
    );
    expect(screen.getByTestId('genre-submit-btn')).toBeDisabled();
  });

  it('only deletes an unused genre after confirmation', async () => {
    adminGenreService.getAllGenres.mockResolvedValue({
      success: true,
      data: {
        content: [{
          publicId: 'genre-unused',
          name: 'Thể loại thử nghiệm',
          status: 'INACTIVE',
          movieCount: 0,
        }],
      },
    });
    const context = renderPage();

    await screen.findByTestId('genre-name-genre-unused');
    const deleteButton = screen.getByTestId('delete-genre-genre-unused');
    expect(deleteButton).toBeEnabled();
    fireEvent.click(deleteButton);

    await waitFor(() => {
      expect(context.triggerConfirm).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Xóa thể loại “Thể loại thử nghiệm”?',
      }));
      expect(adminGenreService.deleteGenre).toHaveBeenCalledWith('genre-unused');
    });
  });
});
