import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';
import TmdbSyncStatusPanel from './TmdbSyncStatusPanel';

vi.mock('@/features/catalog/admin/services/adminTmdbService', () => ({
  default: {
    getSyncState: vi.fn(),
    startBulkSync: vi.fn(),
    resetBulkSync: vi.fn(),
    stopBulkSync: vi.fn(),
    syncMovieById: vi.fn(),
  },
}));

describe('TmdbSyncStatusPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminTmdbService.getSyncState.mockResolvedValue({
      displayStatus: 'RUNNING',
      automaticSyncEnabled: false,
      processedMovies: 0,
      importedMovies: 0,
      skippedMovies: 0,
    });
  });

  it('shows the stopping state immediately after the admin requests a stop', async () => {
    adminTmdbService.stopBulkSync.mockResolvedValue({ data: 'Đã gửi yêu cầu dừng.' });

    render(
      <MemoryRouter>
        <TmdbSyncStatusPanel />
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Dừng nhập phim' }));

    await waitFor(() => {
      expect(adminTmdbService.stopBulkSync).toHaveBeenCalledOnce();
      expect(screen.getByRole('heading', { name: 'Đang dừng tiến trình' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Đang dừng tiến trình' })).toBeDisabled();
  });
});
