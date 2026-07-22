import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import useAutoScheduleForm from '../hooks/useAutoScheduleForm';
import AdminAutoScheduleCreatePage from './AdminAutoScheduleCreatePage';

vi.mock('../hooks/useAutoScheduleForm');
vi.mock('@/components/common/SearchableSelect', () => ({
  default: () => <div data-testid="cinema-select" />,
}));

describe('AdminAutoScheduleCreatePage Phase 1 date contract', () => {
  beforeEach(() => {
    useAutoScheduleForm.mockReturnValue({
      cinemas: [],
      movies: [],
      auditoriums: [],
      versionsByMovie: {},
      selectedCinemaId: 'cinema-1',
      setSelectedCinemaId: vi.fn(),
      selectedCinema: { timezone: 'Asia/Ho_Chi_Minh' },
      scheduleFrom: '2099-08-22',
      setScheduleFrom: vi.fn(),
      scheduleTo: '2099-08-29',
      setScheduleTo: vi.fn(),
      slotGranularityMinutes: 15,
      setSlotGranularityMinutes: vi.fn(),
      previewTtlMinutes: 60,
      setPreviewTtlMinutes: vi.fn(),
      selectedAuditoriumIds: [],
      toggleAuditorium: vi.fn(),
      selectedMovieVersionIds: [],
      toggleVersion: vi.fn(),
      isLoadingCinemas: false,
      isLoadingAuditoriums: false,
      isLoadingMovies: false,
      isSubmitting: false,
      errors: {},
      dateRangeInfo: {
        dayCount: 8,
        cinemaToday: '2099-07-23',
        isTooLong: true,
        suggestedScheduleFrom: '2099-08-22',
        suggestedScheduleTo: '2099-08-28',
      },
      toggleMovieExpansion: vi.fn(),
      handleSubmit: vi.fn(),
    });
  });

  it('keeps the oversized range, explains it inline, and blocks submission', () => {
    render(
      <MemoryRouter>
        <AdminAutoScheduleCreatePage />
      </MemoryRouter>,
    );

    expect(screen.getByText(
      'Mỗi bản xem trước tối đa 7 ngày. Bạn có thể tạo nhiều bản liên tiếp để lập lịch trước cho cả tháng.',
    )).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Khoảng đã chọn gồm 8 ngày');
    expect(screen.getByRole('alert')).toHaveTextContent('2099-08-22 đến 2099-08-28');
    expect(screen.getByRole('alert')).toHaveTextContent('Ngày bạn đã nhập được giữ nguyên');
    expect(screen.getByDisplayValue('2099-08-29')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /TẠO BẢN XEM TRƯỚC/i })).toBeDisabled();
  });
});
