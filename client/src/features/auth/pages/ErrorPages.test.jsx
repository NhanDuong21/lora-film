import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import {
  ForbiddenPage,
  NotFoundPage,
  ServerErrorPage,
  UnauthorizedPage
} from './ErrorPages';

describe('authentication and routing error pages', () => {
  it.each([
    [UnauthorizedPage, '401', 'Cần đăng nhập'],
    [ForbiddenPage, '403', 'Không có quyền truy cập'],
    [NotFoundPage, '404', 'Không tìm thấy trang']
  ])('renders its status and recovery action', (Page, status, title) => {
    render(<Page />);

    expect(screen.getByText(status)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('href');
  });

  it('offers an explicit retry for an unexpected application error', () => {
    const onRetry = vi.fn();
    render(<ServerErrorPage onRetry={onRetry} />);

    fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }));
    expect(onRetry).toHaveBeenCalledOnce();
  });
});
