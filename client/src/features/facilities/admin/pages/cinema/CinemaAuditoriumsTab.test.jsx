import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CinemaAuditoriumsTab from './CinemaAuditoriumsTab';
import adminRoomService from '../../services/adminRoomService';

vi.mock('../../services/adminRoomService', () => ({
  default: {
    createAuditorium: vi.fn(),
    cloneAuditoriumLayout: vi.fn(),
  },
}));

const sourceRoom = {
  publicId: 'auditorium-source',
  name: 'Phòng 01',
  screenType: 'STANDARD',
  soundType: 'DOLBY_ATMOS',
  capacity: 48,
  cleaningBufferMinutes: 20,
  status: 'ACTIVE',
};

function renderTab() {
  const context = {
    triggerConfirm: vi.fn().mockResolvedValue(true),
  };
  const props = {
    cinema: {
      publicId: 'cinema-01',
      activeAuditoriums: [sourceRoom],
    },
    triggerToast: vi.fn(),
    onRefresh: vi.fn(),
  };

  render(
    <MemoryRouter initialEntries={['/admin/cinemas/cinema-01/auditoriums']}>
      <Routes>
        <Route element={<Outlet context={context} />}>
          <Route path="*" element={<CinemaAuditoriumsTab {...props} />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );

  return { context, props };
}

describe('CinemaAuditoriumsTab', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminRoomService.createAuditorium.mockResolvedValue({
      success: true,
      data: { publicId: 'auditorium-copy' },
    });
    adminRoomService.cloneAuditoriumLayout.mockResolvedValue({ success: true });
  });

  it('uses the source capacity when creating a room before cloning its seat layout', async () => {
    const { props } = renderTab();

    fireEvent.click(screen.getByRole('button', { name: 'Nhân bản' }));

    await waitFor(() => {
      expect(adminRoomService.createAuditorium).toHaveBeenCalledWith(
        'cinema-01',
        expect.objectContaining({
          capacity: 48,
          screenType: 'STANDARD',
          soundType: 'DOLBY_ATMOS',
          cleaningBufferMinutes: 20,
        }),
      );
    });
    expect(adminRoomService.cloneAuditoriumLayout).toHaveBeenCalledWith(
      'cinema-01',
      'auditorium-copy',
      'auditorium-source',
    );
    expect(props.onRefresh).toHaveBeenCalledOnce();
  });
});
