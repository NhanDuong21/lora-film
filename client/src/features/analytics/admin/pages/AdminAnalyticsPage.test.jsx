import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminAnalyticsPage from './AdminAnalyticsPage';
import {
  acknowledgeAnalyticsAlert,
  getAnalyticsDashboard,
  updateAnalyticsRecommendation
} from '../services/analyticsAdminService';

vi.mock('../services/analyticsAdminService', () => ({
  acknowledgeAnalyticsAlert: vi.fn(),
  getAnalyticsDashboard: vi.fn(),
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
    getAnalyticsDashboard.mockResolvedValue(dashboard);
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
});
