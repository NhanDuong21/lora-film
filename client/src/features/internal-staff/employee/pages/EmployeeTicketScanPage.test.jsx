import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EmployeeTicketScanPage from './EmployeeTicketScanPage';

const mocks = vi.hoisted(() => ({
  decodeFromImageUrl: vi.fn(),
  getContext: vi.fn(),
  getHistory: vi.fn(),
  scanTicket: vi.fn(),
}));

vi.mock('@zxing/browser', () => ({
  BrowserQRCodeReader: vi.fn(function BrowserQRCodeReader() {
    this.decodeFromImageUrl = mocks.decodeFromImageUrl;
  }),
}));

vi.mock('../services/employeeBoxOfficeService', () => ({
  getMyEmployeeCinemaContext: mocks.getContext,
}));

vi.mock('../services/employeeTicketCheckerService', () => ({
  getTicketScanHistory: mocks.getHistory,
  scanTicket: mocks.scanTicket,
}));

describe('EmployeeTicketScanPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getContext.mockResolvedValue({ cinemaName: 'LoraFilm Landmark 81' });
    mocks.getHistory.mockResolvedValue([]);
    mocks.scanTicket.mockResolvedValue({
      result: 'TOO_EARLY',
      message: 'Chưa đến giờ mở cửa.',
      ticketCode: 'TK-LORAFILM-0001',
    });
    mocks.decodeFromImageUrl.mockResolvedValue({ getText: () => 'TK-LORAFILM-0001' });
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:ticket-qr'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('hiển thị đủ cách quét phù hợp cho điện thoại', async () => {
    render(<EmployeeTicketScanPage />);

    expect(await screen.findByText('LoraFilm Landmark 81')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mở camera trực tiếp' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chụp / chọn ảnh QR' })).toBeInTheDocument();
    expect(screen.getByLabelText('Chụp hoặc chọn ảnh QR')).toHaveAttribute('capture', 'environment');
  });

  it('đọc mã QR từ ảnh và gửi mã vé sang backend', async () => {
    render(<EmployeeTicketScanPage />);
    await screen.findByText('LoraFilm Landmark 81');

    const image = new File(['qr'], 'ticket.png', { type: 'image/png' });
    fireEvent.change(screen.getByLabelText('Chụp hoặc chọn ảnh QR'), {
      target: { files: [image] },
    });

    await waitFor(() => expect(mocks.scanTicket).toHaveBeenCalledWith({
      code: 'TK-LORAFILM-0001',
      gateLabel: 'Cửa phòng 01',
    }));
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:ticket-qr');
  });
});
