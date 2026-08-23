import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ManagerPromotionCenterPage from './ManagerPromotionCenterPage';
import managerPromotionService from '../services/managerPromotionService';

vi.mock('react-router-dom', () => ({
  useOutletContext: () => ({
    selectedCinemaId: 'cinema-a',
    selectedCinema: { publicId: 'cinema-a', name: 'LoraFilm Landmark 81' },
    cinemaState: { loading: false, error: '' },
  }),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      role: 'MANAGER',
      permissions: ['PROMOTION_VIEW', 'PROMOTION_AUDIT_VIEW'],
      cinemaPublicIds: ['cinema-a'],
    },
  }),
}));

vi.mock('../services/managerPromotionService', () => ({
  default: {
    getWorkspace: vi.fn(),
    getCampaigns: vi.fn(),
    getAutomations: vi.fn(),
    getDistributionOptions: vi.fn(),
    getIncidents: vi.fn(),
    issueBenefit: vi.fn(),
  },
}));

const workspace = {
  cinemaPublicId: 'cinema-a',
  activeCampaignCount: 1,
  upcomingCampaignCount: 0,
  lowQuotaBenefitCount: 0,
  openIncidentCount: 0,
  tasks: [],
  capabilities: {
    canViewCinemaPromotions: true,
    canLaunchApprovedTemplate: false,
    canDistributeLocalBenefit: false,
    canViewLocalIncidents: true,
    canProposeCampaign: false,
  },
};

describe('ManagerPromotionCenterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    managerPromotionService.getWorkspace.mockResolvedValue(workspace);
    managerPromotionService.getCampaigns.mockResolvedValue([]);
    managerPromotionService.getAutomations.mockResolvedValue([]);
    managerPromotionService.getDistributionOptions.mockResolvedValue([]);
    managerPromotionService.getIncidents.mockResolvedValue([]);
  });

  it('uses a cinema-specific information architecture without Admin or UAT controls', async () => {
    managerPromotionService.getCampaigns.mockResolvedValue([{
      publicId: 'central-1',
      code: 'CENTRAL-1',
      name: 'Ưu đãi toàn chuỗi cuối tuần',
      description: 'Áp dụng cho Landmark 81.',
      status: 'ACTIVE',
      source: 'CENTRAL',
      readOnly: true,
      startAt: '2026-08-24T00:00:00Z',
      endAt: '2026-08-31T00:00:00Z',
      benefits: [],
    }]);
    render(<ManagerPromotionCenterPage />);

    expect(await screen.findByText('Không có việc khẩn cấp cần xử lý')).toBeInTheDocument();
    expect(screen.queryByText(/UAT\/kiểm thử/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Tạo|Duyệt|Override/i })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Chương trình tại rạp/ }));
    expect(await screen.findByText('Ưu đãi toàn chuỗi cuối tuần')).toBeInTheDocument();
    expect(screen.getByText('Chương trình toàn chuỗi')).toBeInTheDocument();
    expect(managerPromotionService.getCampaigns).toHaveBeenCalledWith('cinema-a');
  });

  it('renders access denied without a false empty success when a request returns 403', async () => {
    const forbidden = new Error('ERR_403_FORBIDDEN');
    forbidden.response = { status: 403 };
    managerPromotionService.getCampaigns.mockRejectedValue(forbidden);
    render(<ManagerPromotionCenterPage />);

    await screen.findByText('Không có việc khẩn cấp cần xử lý');
    fireEvent.click(screen.getByRole('button', { name: /Chương trình tại rạp/ }));

    expect(await screen.findByText('Chưa được cấp quyền')).toBeInTheDocument();
    expect(screen.getByText(/Liên hệ quản trị viên hệ thống/)).toBeInTheDocument();
    expect(screen.queryByText(/ERR_403_FORBIDDEN/)).not.toBeInTheDocument();
    expect(screen.queryByText('Chưa có chương trình áp dụng tại rạp')).not.toBeInTheDocument();

    await new Promise(resolve => setTimeout(resolve, 100));
    expect(managerPromotionService.getCampaigns).toHaveBeenCalledTimes(1);
  });

  it('shows an empty incident state only after the scoped request succeeds', async () => {
    render(<ManagerPromotionCenterPage />);
    await screen.findByText('Không có việc khẩn cấp cần xử lý');
    fireEvent.click(screen.getByRole('button', { name: /Sự cố tại rạp/ }));

    expect(await screen.findByText('Không có sự cố tại rạp cần xử lý')).toBeInTheDocument();
    expect(managerPromotionService.getIncidents).toHaveBeenCalledWith('cinema-a');
  });

  it('keeps the incident tab hidden when the backend capability denies it', async () => {
    managerPromotionService.getWorkspace.mockResolvedValue({
      ...workspace,
      capabilities: { ...workspace.capabilities, canViewLocalIncidents: false },
    });
    render(<ManagerPromotionCenterPage />);

    await waitFor(() => expect(managerPromotionService.getWorkspace).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByRole('button', { name: /Sự cố tại rạp/ })).not.toBeInTheDocument());
    expect(managerPromotionService.getIncidents).not.toHaveBeenCalled();
  });
});
