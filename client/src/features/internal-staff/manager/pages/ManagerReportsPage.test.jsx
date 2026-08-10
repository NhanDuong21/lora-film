import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerReportsPage from './ManagerReportsPage';
import managerCinemaService from '../services/managerCinemaService';

const cinemaId = 'b1575c2d-9081-11f1-bf65-0ebab02bf6f5';
const selectedCinema = { publicId: cinemaId, name: 'LoraFilm Landmark 81' };

vi.mock('react-router-dom', async importOriginal => {
  const actual = await importOriginal();
  return {
    ...actual,
    useOutletContext: () => ({
      selectedCinema,
      selectedCinemaId: cinemaId,
      cinemaState: { loading: false, error: '' },
    }),
  };
});

vi.mock('../services/managerCinemaService', () => ({
  default: {
    getCinemaReport: vi.fn(),
    acknowledgeAlert: vi.fn(),
    updateRecommendation: vi.fn(),
  },
}));

const currentReport = {
  period: { startDate: '2026-07-11', endDate: '2026-08-09' },
  scope: { type: 'CINEMA', cinemaKey: cinemaId, cinemaName: selectedCinema.name },
  summary: {
    netRevenue: 1125000,
    grossRevenue: 1200000,
    bookingCount: 14,
    ticketCount: 25,
    occupancyRate: 0.32,
    refundRate: 0.02,
    refundBookingCount: 1,
  },
  daily: [{
    statDate: '2026-08-05',
    netRevenue: 1125000,
    ticketCount: 25,
    occupancyRate: 0.32,
  }],
  topMovies: [{
    movieKey: 'movie-1',
    movieTitle: 'Mưa Đỏ',
    ticketCount: 25,
    occupancyRate: 0.32,
    netRevenue: 1125000,
  }],
  promotions: [{
    promotionKey: 'promo-1',
    promotionName: 'Ưu đãi thành viên',
    usageCount: 3,
    discountCost: 75000,
    generatedRevenue: 450000,
    roi: 6,
  }],
  insights: [{
    id: 3,
    statDate: '2026-08-05',
    severity: 'WARNING',
    confidenceScore: 0.85,
    title: 'Occupancy tại rạp cần chú ý',
    summary: 'Occupancy thấp hơn mức thông thường.',
    rootCause: 'CINEMA_LOW_OCCUPANCY',
    rootCauses: [{ rank: 1, causeType: 'CINEMA_LOW_OCCUPANCY', dimensionType: 'CINEMA', dimensionKey: cinemaId, contributionScore: 0.7 }],
  }],
  recommendations: [{
    id: 9,
    insightId: 3,
    priority: 'HIGH',
    targetService: 'movie-service',
    title: 'Rà soát lịch chiếu',
    description: 'Điều chỉnh khung giờ có công suất thấp.',
    expectedImpact: 'Tăng tỷ lệ lấp đầy.',
    status: 'PENDING',
  }],
  alerts: [{
    id: 12,
    insightId: 3,
    severity: 'WARNING',
    title: 'Công suất ghế giảm',
    message: 'Một số khung giờ có lượng khách thấp.',
    acknowledged: false,
    createdAt: '2026-08-08T10:00:00Z',
  }],
  dataQuality: {
    freshnessStatus: 'FRESH',
    lastPipelineStatus: 'SUCCESS',
    lastCalculatedDate: '2026-08-08',
  },
};

const previousReport = {
  ...currentReport,
  summary: {
    ...currentReport.summary,
    netRevenue: 900000,
    bookingCount: 10,
    occupancyRate: 0.25,
    refundRate: 0.04,
  },
};

describe('ManagerReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerCinemaService.getCinemaReport
      .mockResolvedValueOnce(currentReport)
      .mockResolvedValueOnce(previousReport)
      .mockResolvedValue(currentReport);
    managerCinemaService.acknowledgeAlert.mockResolvedValue({ status: 'ACKNOWLEDGED' });
    managerCinemaService.updateRecommendation.mockResolvedValue({ status: 'ACCEPTED' });
  });

  it('hiển thị báo cáo theo quyết định, đúng phạm vi rạp và đúng tỷ lệ phần trăm', async () => {
    render(<ManagerReportsPage />);

    expect(await screen.findByRole('heading', { name: 'Trung tâm điều hành tại rạp' })).toBeInTheDocument();
    expect(screen.getAllByText('LoraFilm Landmark 81').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /Đã xảy ra gì/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Vì sao/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Nên làm gì/ })).toBeInTheDocument();
    expect(screen.getAllByText(/32[,.]0%/).length).toBeGreaterThan(0);
    expect(screen.getByText('Mưa Đỏ')).toBeInTheDocument();
    expect(screen.getByText(/Ưu đãi thành viên/)).toBeInTheDocument();

    expect(managerCinemaService.getCinemaReport).toHaveBeenCalledTimes(2);
    expect(managerCinemaService.getCinemaReport).toHaveBeenCalledWith(expect.objectContaining({
      cinemaKey: cinemaId,
      startDate: expect.any(String),
      endDate: expect.any(String),
    }));
  });

  it('cho phép quản lý nhận việc và ghi nhận cảnh báo của rạp', async () => {
    render(<ManagerReportsPage />);
    await screen.findByRole('heading', { name: 'Trung tâm điều hành tại rạp' });

    fireEvent.click(screen.getByRole('button', { name: /Nên làm gì/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận cảnh báo' }));
    await waitFor(() => expect(managerCinemaService.acknowledgeAlert).toHaveBeenCalledWith(12));

    fireEvent.click(screen.getByRole('button', { name: 'Nhận xử lý' }));
    await waitFor(() => expect(managerCinemaService.updateRecommendation).toHaveBeenCalledWith(9, 'ACCEPTED'));
  });
});
