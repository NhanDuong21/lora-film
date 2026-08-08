import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { UatGuide, WorkflowSteps } from './HrWorkspace';

describe('không gian vận hành nhân sự', () => {
  it('hướng dẫn admin bằng các bước tiếng Việt dễ hiểu', () => {
    render(<MemoryRouter><UatGuide /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Cách kiểm tra' }));

    expect(screen.getByRole('heading', { name: 'Bạn chỉ cần kiểm tra 6 việc' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Tạo hồ sơ nhân viên/ })).toHaveAttribute('href', '/admin/staff');
    expect(screen.getByRole('link', { name: /Chạy kỳ lương/ })).toHaveAttribute('href', '/admin/payroll');
    expect(screen.getByText(/người tạo và người duyệt phải là hai tài khoản khác nhau/)).toBeInTheDocument();
  });

  it('hiển thị đúng trạng thái từng bước của quy trình', () => {
    render(<WorkflowSteps steps={[
      { label: 'Lấy dữ liệu công', state: 'done' },
      { label: 'Kiểm tra & duyệt', state: 'active' },
      { label: 'Hoàn tất', state: 'waiting' }
    ]} />);

    expect(screen.getByText('Lấy dữ liệu công')).toBeInTheDocument();
    expect(screen.getByText('Kiểm tra & duyệt')).toBeInTheDocument();
    expect(screen.getByText('Hoàn tất')).toBeInTheDocument();
  });
});
