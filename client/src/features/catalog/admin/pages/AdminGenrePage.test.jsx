import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
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

const renderPage = () => render(
  <MemoryRouter>
    <AdminGenrePage />
  </MemoryRouter>,
);

describe('AdminGenrePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminGenreService.getAllGenres.mockResolvedValue({
      success: true,
      data: { content: [{ publicId: 'genre-1', name: 'Hành động' }] },
    });
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
});
