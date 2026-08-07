import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieTmdbQueuePanel from './MovieTmdbQueuePanel';

describe('MovieTmdbQueuePanel', () => {
  it('hiển thị số phim đủ điều kiện, quá ngày và chưa có ngày', () => {
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 17, eligibleUpcoming: 8, releaseDateExpired: 6, undated: 3 }}
        onApprove={vi.fn()}
      />,
    );

    expect(screen.getByText('Đủ điều kiện về ngày')).toBeInTheDocument();
    expect(screen.getByText('Ngày khai thác không còn hợp lệ')).toBeInTheDocument();
    expect(screen.getByText('Chưa có ngày bắt đầu khai thác')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('chỉ duyệt các phim có ngày khai thác sau hôm nay', () => {
    const onApprove = vi.fn();
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 3, eligibleUpcoming: 1, releaseDateExpired: 2, undated: 0 }}
        onApprove={onApprove}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Duyệt tối đa 1 phim đủ điều kiện' }));
    expect(onApprove).toHaveBeenCalledTimes(1);
  });
});
