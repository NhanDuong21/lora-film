import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AdminWorkforcePage from './AdminWorkforcePage';
import {
  getAttendance,
  getEmployees,
  getLeaveRequests,
  getPiiGovernanceSummary,
  getWorkShifts,
} from '../services/userAdminService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

const access = vi.hoisted(() => () => true);

vi.mock('../hooks/useAdminAccess', () => ({ default: () => access }));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => ({ accountId: 1 }) }));
vi.mock('react-router-dom', async importOriginal => {
  const actual = await importOriginal();
  return { ...actual, useOutletContext: () => ({ triggerToast: vi.fn() }) };
});
vi.mock('../services/userAdminService', () => ({
  applyLeaveRequestAction: vi.fn(),
  correctAttendance: vi.fn(),
  createWorkShiftBatch: vi.fn(),
  getAttendance: vi.fn(),
  getEmployees: vi.fn(),
  getLeaveRequests: vi.fn(),
  getPiiGovernanceSummary: vi.fn(),
  getWorkShifts: vi.fn(),
}));
vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn() },
}));

const admin = {
  accountId: 1,
  employeeCode: 'EMP-0001',
  fullName: 'Admin Tối Cao',
  positionName: 'Chuyên viên tài chính',
  status: 'ACTIVE',
};
const employee = {
  accountId: 3,
  employeeCode: 'EMP-0003',
  fullName: 'Nguyễn Hoàng Nhân',
  positionName: 'Nhân viên quầy vé',
  status: 'ACTIVE',
};

describe('AdminWorkforcePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getEmployees.mockResolvedValue({ content: [admin, employee], totalElements: 2 });
    getWorkShifts.mockResolvedValue({
      content: [{
        id: 10,
        employeeId: 3,
        scheduledStart: new Date().toISOString(),
        scheduledEnd: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
        location: 'LoraFilm Landmark 81',
        status: 'SCHEDULED',
      }],
      totalElements: 1,
    });
    getAttendance.mockResolvedValue({ content: [], totalElements: 0 });
    getLeaveRequests.mockResolvedValue({ content: [], totalElements: 0 });
    getPiiGovernanceSummary.mockResolvedValue({ protectedProfiles: 2, totalProfiles: 2 });
    adminCinemaService.getCinemas.mockResolvedValue({ data: { data: [] } });
  });

  it('excludes the current system administrator from shift scheduling', async () => {
    render(<MemoryRouter><AdminWorkforcePage /></MemoryRouter>);

    expect(await screen.findByText('Nguyễn Hoàng Nhân')).toBeInTheDocument();
    expect(screen.queryByText('Admin Tối Cao')).not.toBeInTheDocument();
    await waitFor(() => expect(getEmployees).toHaveBeenCalledWith(expect.objectContaining({
      status: 'ACTIVE',
      excludeCurrentAccount: true,
    })));
  });
});
