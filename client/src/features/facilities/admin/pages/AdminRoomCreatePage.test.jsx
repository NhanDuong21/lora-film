import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import AdminRoomCreatePage from './AdminRoomCreatePage';

const outlet = vi.hoisted(() => ({
  triggerToast: vi.fn(),
  triggerConfirm: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useOutletContext: () => outlet,
  };
});

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getAdminCinemaDetail: vi.fn() },
}));

vi.mock('@/features/facilities/admin/services/adminRoomService', () => ({
  default: {
    getAdminSeatLayout: vi.fn(),
    getLayoutTemplates: vi.fn(),
    getClonePreview: vi.fn(),
    getSeatTypes: vi.fn(),
    createAuditoriumFromTemplate: vi.fn(),
    cloneAuditoriumAsNew: vi.fn(),
    createAuditoriumWithLayout: vi.fn(),
  },
}));

const matrix = [
  ['EXIT', 'STANDARD', 'EXIT'],
  ['DISABLED', 'VIP', 'STANDARD'],
  ['COUPLE', 'COUPLE', 'AISLE'],
];

const validation = [
  { code: 'CAPACITY_MATCH', label: 'Sức chứa khớp nguồn sơ đồ', passed: true, severity: 'SUCCESS' },
  { code: 'ACCESSIBLE_POSITION', label: 'Có vị trí tiếp cận', passed: true, severity: 'SUCCESS' },
];

const template = {
  sourceType: 'TEMPLATE',
  sourcePublicId: 'system-standard-6-v1',
  name: 'Tiêu chuẩn 6 — Cân bằng',
  scope: 'SYSTEM',
  layoutVersion: 1,
  recommendedScreenType: 'STANDARD',
  recommendedSoundType: 'STANDARD',
  rows: 3,
  columns: 3,
  capacity: 6,
  standardSeats: 2,
  vipSeats: 1,
  coupleModules: 1,
  coupleSeats: 2,
  accessiblePositions: 1,
  aisleCount: 1,
  doorCount: 2,
  matrix,
  valid: true,
  validation,
};

const clonePreview = {
  ...template,
  sourceType: 'AUDITORIUM',
  sourcePublicId: 'room-1',
  name: 'Phòng 01',
  scope: 'CINEMA',
};

describe('AdminRoomCreatePage quick flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getAdminCinemaDetail.mockResolvedValue({
      success: true,
      data: {
        publicId: 'cinema-1',
        name: 'LoraFilm Sencity Hà Nội',
        activeAuditoriums: [
          {
            publicId: 'room-1',
            name: 'Phòng 01',
            status: 'ACTIVE',
            capacity: 120,
            screenType: 'STANDARD',
            soundType: 'STANDARD',
            cleaningBufferMinutes: 15,
          },
          {
            publicId: 'room-2',
            name: 'Phòng 02',
            status: 'ACTIVE',
            capacity: 96,
            screenType: 'STANDARD',
            soundType: 'DOLBY_ATMOS',
            cleaningBufferMinutes: 15,
          },
        ],
      },
    });
    adminRoomService.getLayoutTemplates.mockResolvedValue({ success: true, data: [template] });
    adminRoomService.getClonePreview.mockResolvedValue({ success: true, data: clonePreview });
    adminRoomService.getSeatTypes.mockResolvedValue({ success: true, data: [] });
    adminRoomService.createAuditoriumFromTemplate.mockResolvedValue({
      success: true,
      data: { publicId: 'new-room' },
    });
  });

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/admin/rooms/create?cinemaId=cinema-1']}>
        <AdminRoomCreatePage />
      </MemoryRouter>,
    );
  }

  it('starts from a recommended existing room with a read-only preview', async () => {
    renderPage();

    expect(await screen.findByText('Preview chỉ đọc')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Phòng 03')).toBeInTheDocument();
    expect(screen.getByText('Sao chép phòng hiện có')).toBeInTheDocument();
    expect(screen.getByText('Kế thừa từ cụm rạp: 15 phút')).toBeInTheDocument();
    expect(screen.getByText('6 người')).toBeInTheDocument();
    expect(screen.queryByText('Cọ vẽ:')).not.toBeInTheDocument();
    expect(adminRoomService.getClonePreview).toHaveBeenCalledWith('room-1');
  });

  it('creates directly from a complete template without opening the manual wizard', async () => {
    renderPage();
    await screen.findByText('Preview chỉ đọc');

    fireEvent.click(screen.getByRole('button', { name: /Dùng mẫu có sẵn/i }));
    expect(await screen.findByText('Tiêu chuẩn 6 — Cân bằng')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^Tạo phòng$/i }));

    await waitFor(() => expect(adminRoomService.createAuditoriumFromTemplate).toHaveBeenCalledWith({
      cinemaPublicId: 'cinema-1',
      templatePublicId: 'system-standard-6-v1',
      name: 'Phòng 03',
      screenType: 'STANDARD',
      soundType: 'STANDARD',
      cleaningBufferMinutes: 15,
    }));
  });

  it('auto-fits a wide layout and gives the scaled preview a centered footprint', async () => {
    const wideTemplate = {
      ...template,
      sourcePublicId: 'system-large-180-v1',
      name: 'Large 180',
      rows: 12,
      columns: 19,
      capacity: 180,
      matrix: Array.from({ length: 12 }, () => Array.from({ length: 19 }, () => 'STANDARD')),
    };
    adminRoomService.getLayoutTemplates.mockResolvedValue({ success: true, data: [template, wideTemplate] });

    const offsetWidthSpy = vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get')
      .mockImplementation(function getOffsetWidth() {
        return this.dataset.testid === 'seat-preview-content' ? 932 : 0;
      });
    const offsetHeightSpy = vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get')
      .mockImplementation(function getOffsetHeight() {
        return this.dataset.testid === 'seat-preview-content' ? 760 : 0;
      });
    const clientWidthSpy = vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get')
      .mockImplementation(function getClientWidth() {
        return this.dataset.testid === 'seat-preview-viewport' ? 751 : 0;
      });

    try {
      renderPage();
      await screen.findByText('Preview chỉ đọc');
      fireEvent.click(screen.getByRole('button', { name: /Dùng mẫu có sẵn/i }));
      fireEvent.click(screen.getByRole('button', { name: /Large 180/i }));

      await waitFor(() => expect(screen.getByText('75%')).toBeInTheDocument());
      expect(screen.getByTestId('seat-preview-content')).toHaveStyle({ transform: 'scale(0.75)' });
      expect(screen.getByTestId('seat-preview-footprint')).toHaveStyle({
        width: '699px',
        height: '570px',
      });
    } finally {
      offsetWidthSpy.mockRestore();
      offsetHeightSpy.mockRestore();
      clientWidthSpy.mockRestore();
    }
  });
});
