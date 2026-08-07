import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MovieTmdbQueuePanel from './MovieTmdbQueuePanel';

describe('MovieTmdbQueuePanel', () => {
  it('shows future, ready-to-show, needs-schedule and undated counts', () => {
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 17, future: 8, readyToShow: 4, needsSchedule: 2, undated: 3 }}
        onApprove={vi.fn()}
      />,
    );

    expect(screen.getByText('Sắp tới thời gian khai thác')).toBeInTheDocument();
    expect(screen.getByText('Đủ lịch để đang chiếu')).toBeInTheDocument();
    expect(screen.getByText('Cần lập lịch chiếu')).toBeInTheDocument();
    expect(screen.getByText('Chưa có ngày bắt đầu khai thác')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('runs one approval flow for all currently eligible movies', () => {
    const onApprove = vi.fn();
    render(
      <MovieTmdbQueuePanel
        breakdown={{ total: 3, future: 1, readyToShow: 1, needsSchedule: 1, undated: 0 }}
        onApprove={onApprove}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Duyệt tối đa 2 phim đủ điều kiện' }));
    expect(onApprove).toHaveBeenCalledTimes(1);
  });
});
