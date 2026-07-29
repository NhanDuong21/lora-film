import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AdminSidebar from './AdminSidebar';

describe('AdminSidebar scheduling terminology', () => {
  it('distinguishes operational Showtimes from preview history', () => {
    render(
      <MemoryRouter>
        <AdminSidebar
          activeTab="auto-schedule-history"
          setActiveTab={vi.fn()}
          user={{ role: 'ADMIN', permissions: [] }}
          onBackHome={vi.fn()}
          handleLogout={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText('Lịch chiếu')).toBeInTheDocument();
    expect(screen.getByText('Lịch sử bản xem trước')).toBeInTheDocument();
    expect(screen.queryByText('Lịch sử bản xem trước xếp lịch')).not.toBeInTheDocument();
    expect(screen.queryByText('Lịch sử xếp lịch')).not.toBeInTheDocument();
  });

  it('shows the new revenue report once and removes the legacy sidebar entry', () => {
    render(
      <MemoryRouter>
        <AdminSidebar
          activeTab="analytics"
          setActiveTab={vi.fn()}
          user={{ role: 'ADMIN', permissions: [] }}
          onBackHome={vi.fn()}
          handleLogout={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(screen.getAllByText('Báo cáo doanh thu')).toHaveLength(1);
    expect(screen.queryByText('Analytics & KPI')).not.toBeInTheDocument();
  });
});
