import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminPricingService from '../services/adminPricingService';
import AdminPricingPolicyDetailPage from './AdminPricingPolicyDetailPage';

vi.mock('../services/adminPricingService', () => ({
  default: {
    getPolicy: vi.fn(),
    getUsage: vi.fn(),
    activatePolicy: vi.fn(),
    deactivatePolicy: vi.fn(),
    copyPolicy: vi.fn(),
  },
}));

describe('AdminPricingPolicyDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminPricingService.getPolicy.mockResolvedValue({
      data: {
        publicId: 'policy-1',
        name: 'Giá chuẩn',
        storedStatus: 'ACTIVE',
        displayStatus: 'ACTIVE',
        version: 3,
        cinemaName: 'Lora Quận 1',
        effectiveFrom: '2026-01-01',
        currency: 'VND',
        priority: 0,
        rules: [],
        conflicts: [],
      },
    });
    adminPricingService.getUsage.mockResolvedValue({
      data: { snapshotShowtimeCount: 0, futureDraftShowtimeCount: 0, affectedFutureShowtimes: [] },
    });
  });

  it('explains immutable ACTIVE policies and offers a new version instead of editing', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/pricing/policy-1']}>
        <Routes>
          <Route path="/admin/pricing/:id" element={<AdminPricingPolicyDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(adminPricingService.getPolicy).toHaveBeenCalled());
    expect(screen.getByText('Tạo phiên bản mới')).toBeInTheDocument();
    expect(screen.getByText('Chính sách đang hoạt động không thể sửa trực tiếp để bảo toàn lịch sử giá. Hãy tạo một phiên bản mới để thay đổi.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Sửa' })).not.toBeInTheDocument();
  });
});
