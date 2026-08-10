import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminAnalyticsPage from './AdminAnalyticsPage';
import {
  acknowledgeAnalyticsAlert,
  getAnalyticsDashboard,
  getCinemaDirectory,
  getCinemaKpis,
  updateAnalyticsRecommendation
} from '../services/analyticsAdminService';

const authContext = vi.hoisted(() => ({ role: 'MANAGER', permissions: [] }));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    userRole: authContext.role,
    user: { role: authContext.role, permissions: authContext.permissions },
  }),
}));

vi.mock('../services/analyticsAdminService', () => ({
  acknowledgeAnalyticsAlert: vi.fn(),
  getAnalyticsDashboard: vi.fn(),
  getCinemaDirectory: vi.fn(),
  getCinemaKpis: vi.fn(),
  updateAnalyticsRecommendation: vi.fn()
}));

const dashboard = {
  period: { startDate: '2026-07-01', endDate: '2026-07-29' },
  summary: {
    netRevenue: 1000000,
    grossRevenue: 1200000,
    bookingCount: 10,
    averageBookingValue: 100000,
    occupancyRate: 0.5,
    ticketCount: 20,
    refundRate: 0.1,
    refundBookingCount: 1
  },
  daily: [],
  topMovies: [],
  topCinemas: [],
  promotions: [],
  customerSegments: [],
  forecasts: [],
  healthScore: {
    overallScore: 76,
    revenueScore: 82,
    demandScore: 75,
    occupancyScore: 71,
    customerScore: 68,
    operationalScore: 79,
    healthStatus: 'STABLE',
    confidenceScore: 0.9,
    algorithmVersion: 'HEALTH_SCORE_V1'
  },
  anomalies: [],
  forecastQuality: [],
  insights: [],
  recommendations: [{
    id: 9,
    priority: 'HIGH',
    targetService: 'promotion-service',
    title: 'Rà soát doanh thu',
    description: 'Kiểm tra rạp đóng góp lớn nhất.',
    expectedImpact: 'Khôi phục doanh thu.',
    status: 'PENDING'
  }],
  alerts: [{
    id: 12,
    severity: 'WARNING',
    title: 'Doanh thu giảm',
    message: 'Thấp hơn đường cơ sở.',
    acknowledged: false
  }],
  dataQuality: {
    latestCompleteness: 0.75,
    paymentFacts: 10,
    cancellationFacts: 2,
    refundFacts: 1,
    freshnessStatus: 'DEGRADED',
    lastPipelineStatus: 'SUCCESS',
    lastCalculatedDate: '2026-07-28'
  }
};

describe('AdminAnalyticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authContext.role = 'MANAGER';
    authContext.permissions = [];
    getAnalyticsDashboard.mockResolvedValue(dashboard);
    getCinemaKpis.mockResolvedValue([
      { cinemaKey: 'cinema-1', cinemaName: 'LoraFilm Quận 1' }
    ]);
    getCinemaDirectory.mockResolvedValue([
      { publicId: 'cinema-1', name: 'LoraFilm Quận 1' },
      { publicId: 'cinema-2', name: 'KingHouse Q10' }
    ]);
    acknowledgeAnalyticsAlert.mockResolvedValue({ status: 'ACKNOWLEDGED' });
    updateAnalyticsRecommendation.mockResolvedValue({ status: 'ACCEPTED' });
  });

  it('renders a decision-oriented overview and health score', async () => {
    render(<AdminAnalyticsPage />);

    expect(await screen.findByText('Trung tâm điều hành kinh doanh')).toBeInTheDocument();
    expect(screen.getByText('Sức khỏe hoạt động toàn chuỗi')).toBeInTheDocument();
    expect(screen.getByText('76')).toBeInTheDocument();
    expect(screen.getByText('50.0%')).toBeInTheDocument();
  });

  it('places the revenue chart before the business health overview', async () => {
    render(<AdminAnalyticsPage />);

    const revenueChart = await screen.findByText('Doanh thu thuần theo ngày');
    const healthOverview = screen.getByText('Sức khỏe hoạt động toàn chuỗi');

    expect(
      revenueChart.compareDocumentPosition(healthOverview)
      & Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy();
  });

  it('exposes the four BI questions as simple navigation', async () => {
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');

    expect(screen.getByRole('button', { name: /Đã xảy ra gì/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Vì sao/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sắp xảy ra gì/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Nên làm gì/ })).toBeInTheDocument();
  });

  it('loads only the current date when choosing Hôm nay', async () => {
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');
    getAnalyticsDashboard.mockClear();

    const currentDate = new Date();
    const today = [
      currentDate.getFullYear(),
      String(currentDate.getMonth() + 1).padStart(2, '0'),
      String(currentDate.getDate()).padStart(2, '0')
    ].join('-');

    fireEvent.click(screen.getByRole('button', { name: 'Hôm nay' }));

    await waitFor(() => {
      expect(getAnalyticsDashboard).toHaveBeenCalledWith({
        startDate: today,
        endDate: today
      });
    });
  });

  it('loads a dashboard scoped to one selected cinema', async () => {
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');
    getAnalyticsDashboard.mockClear();

    fireEvent.change(screen.getByLabelText('Chọn rạp phân tích'), {
      target: { value: 'cinema-1' }
    });

    await waitFor(() => {
      expect(getAnalyticsDashboard).toHaveBeenCalledWith(
        expect.objectContaining({ cinemaKey: 'cinema-1' })
      );
    });
  });

  it('allows selecting a cinema that has no analytics data yet', async () => {
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');
    getAnalyticsDashboard.mockClear();

    expect(screen.getByRole('option', { name: 'KingHouse Q10' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Chọn rạp phân tích'), {
      target: { value: 'cinema-2' }
    });

    await waitFor(() => {
      expect(getAnalyticsDashboard).toHaveBeenCalledWith(
        expect.objectContaining({ cinemaKey: 'cinema-2' })
      );
    });
  });

  it('explains zero values when the selected cinema has no transactions', async () => {
    getAnalyticsDashboard.mockResolvedValue({
      ...dashboard,
      scope: {
        type: 'CINEMA',
        cinemaKey: 'cinema-2',
        cinemaName: 'cinema-2'
      },
      summary: {
        ...dashboard.summary,
        netRevenue: 0,
        grossRevenue: 0,
        bookingCount: 0,
        ticketCount: 0
      },
      daily: []
    });

    render(<AdminAnalyticsPage />);

    expect(await screen.findByText(/chưa phát sinh/)).toBeInTheDocument();
    expect(screen.getByText(/không phải lỗi hệ thống/)).toBeInTheDocument();
  });

  it('shows the cinema name from the directory instead of a UUID', async () => {
    const cinemaId = '479a04f1-32a1-4b9d-94cd-3e1842cb33f6';
    getAnalyticsDashboard.mockResolvedValue({
      ...dashboard,
      scope: { type: 'CINEMA', cinemaKey: cinemaId, cinemaName: cinemaId },
      topCinemas: [{
        cinemaKey: cinemaId,
        cinemaName: cinemaId,
        ticketCount: 20,
        occupancyRate: 0.5,
        netRevenue: 1000000
      }]
    });
    getCinemaKpis.mockResolvedValue([
      { cinemaKey: cinemaId, cinemaName: cinemaId }
    ]);
    getCinemaDirectory.mockResolvedValue([
      { publicId: cinemaId, name: 'LoraFilm Gò Vấp' }
    ]);

    render(<AdminAnalyticsPage />);

    expect(await screen.findByText('Hiệu suất riêng · LoraFilm Gò Vấp')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'LoraFilm Gò Vấp' })).toBeInTheDocument();
    expect(screen.queryByText(cinemaId)).not.toBeInTheDocument();
  });

  it('lets managers acknowledge alerts and accept recommendations', async () => {
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');

    fireEvent.click(screen.getByRole('button', { name: /Nên làm gì/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận cảnh báo' }));
    await waitFor(() => expect(acknowledgeAnalyticsAlert).toHaveBeenCalledWith(12));

    fireEvent.click(screen.getByRole('button', { name: 'Nhận xử lý' }));
    await waitFor(() => {
      expect(updateAnalyticsRecommendation).toHaveBeenCalledWith(9, 'ACCEPTED');
    });
  });

  it('keeps accounting analytics read-only', async () => {
    authContext.role = 'EMPLOYEE';
    authContext.permissions = ['ANALYTICS_VIEW'];
    render(<AdminAnalyticsPage />);
    await screen.findByText('Trung tâm điều hành kinh doanh');

    fireEvent.click(screen.getByRole('button', { name: /Nên làm gì/ }));

    expect(screen.getByText(/Kế toán dùng phần này để đọc nguyên nhân/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ghi nhận cảnh báo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Nhận xử lý' })).not.toBeInTheDocument();
  });
});
