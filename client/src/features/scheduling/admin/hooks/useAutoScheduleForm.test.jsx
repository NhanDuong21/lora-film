import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '../services/adminAutoScheduleService';
import useAutoScheduleForm from './useAutoScheduleForm';

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn(), getAdminCinemaDetail: vi.fn() },
}));
vi.mock('../services/adminAutoScheduleService', () => ({
  default: { getEligibleMovies: vi.fn(), generatePreview: vi.fn() },
}));

const cinema = {
  publicId: 'cinema-1', name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh',
};
const auditorium = {
  publicId: 'auditorium-1', name: 'Phòng 1', status: 'ACTIVE',
};
const eligibleMovie = {
  moviePublicId: 'movie-1', title: 'Phim thử nghiệm', eligible: true, reasons: [],
  status: 'NOW_SHOWING',
  versions: [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE' }],
};

describe('useAutoScheduleForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getCinemas.mockResolvedValue({
      success: true, data: { data: [cinema] },
    });
    adminCinemaService.getAdminCinemaDetail.mockResolvedValue({
      success: true, data: { ...cinema, activeAuditoriums: [auditorium] },
    });
    adminAutoScheduleService.getEligibleMovies.mockResolvedValue({
      success: true, data: [eligibleMovie],
    });
  });

  const configure = async (result, scheduleTo = '2099-08-28') => {
    await waitFor(() => expect(result.current.cinemas).toHaveLength(1));
    act(() => result.current.setSelectedCinemaId('cinema-1'));
    await waitFor(() => expect(result.current.auditoriums).toHaveLength(1));
    expect(result.current.selectedAuditoriumIds).toEqual([]);
    act(() => result.current.selectAllActiveAuditoriums());
    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    act(() => {
      result.current.setScheduleFrom('2099-08-22');
      result.current.setScheduleTo(scheduleTo);
      result.current.toggleVersion('version-1');
    });
    await waitFor(() => expect(result.current.selectedAuditoriumIds).toEqual(['auditorium-1']));
  };

  it('starts rooms empty and supports select-all and clear actions', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await waitFor(() => expect(result.current.cinemas).toHaveLength(1));
    act(() => result.current.setSelectedCinemaId('cinema-1'));
    await waitFor(() => expect(result.current.auditoriums).toHaveLength(1));

    expect(result.current.selectedAuditoriumIds).toEqual([]);
    act(() => result.current.selectAllActiveAuditoriums());
    expect(result.current.selectedAuditoriumIds).toEqual(['auditorium-1']);
    act(() => result.current.clearAuditoriums());
    expect(result.current.selectedAuditoriumIds).toEqual([]);
  });

  it('restores an eligible schedule draft for recreation', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({
      initialDraft: {
        cinemaPublicId: 'cinema-1',
        scheduleFrom: '2099-08-22',
        scheduleTo: '2099-08-28',
        slotGranularityMinutes: 30,
        auditoriumPublicIds: ['auditorium-1'],
        movieVersionPublicIds: ['version-1'],
      },
    }));

    await waitFor(() => expect(result.current.auditoriums).toHaveLength(1));
    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    expect(result.current.selectedCinemaId).toBe('cinema-1');
    expect(result.current.scheduleFrom).toBe('2099-08-22');
    expect(result.current.scheduleTo).toBe('2099-08-28');
    expect(result.current.slotGranularityMinutes).toBe(30);
    expect(result.current.selectedAuditoriumIds).toEqual(['auditorium-1']);
    expect(result.current.selectedMovieVersionIds).toEqual(['version-1']);
  });

  it('retains ineligible movies and removes stale selected versions after date changes', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    act(() => result.current.toggleVersion('version-1'));
    expect(result.current.selectedMovieVersionIds).toEqual(['version-1']);

    adminAutoScheduleService.getEligibleMovies.mockResolvedValue({
      success: true,
      data: [{
        ...eligibleMovie,
        eligible: false,
        reasons: [{ code: 'OUTSIDE_RELEASE_WINDOW', message: 'Ngoài thời gian phát hành' }],
      }],
    });
    act(() => result.current.setScheduleFrom('2099-08-23'));

    await waitFor(() => expect(result.current.movies[0].eligible).toBe(false));
    expect(result.current.movies[0].reasons[0].message).toBe('Ngoài thời gian phát hành');
    expect(result.current.selectedMovieVersionIds).toEqual([]);
    expect(result.current.selectionNotice).toContain('Đã bỏ 1 định dạng');
  });

  it('does not expose draft movies even if an older API response includes them', async () => {
    adminAutoScheduleService.getEligibleMovies.mockResolvedValue({
      success: true,
      data: [
        eligibleMovie,
        {
          moviePublicId: 'draft-movie',
          title: 'Phim nháp',
          status: 'DRAFT',
          eligible: true,
          reasons: [],
          versions: [{ publicId: 'draft-version', versionName: '2D', status: 'ACTIVE' }],
        },
      ],
    });

    const { result } = renderHook(() => useAutoScheduleForm({}));

    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    expect(result.current.movies[0].publicId).toBe('movie-1');
    expect(result.current.versionsByMovie).not.toHaveProperty('draft-movie');
  });

  it('exposes an explicit retry for a failed movie eligibility load', async () => {
    adminAutoScheduleService.getEligibleMovies
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce({ success: true, data: [eligibleMovie] });
    const { result } = renderHook(() => useAutoScheduleForm({}));

    await waitFor(() => expect(result.current.movieLoadError).toBeTruthy());
    act(() => result.current.retryMovies());

    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    expect(result.current.movieLoadError).toBe('');
    expect(adminAutoScheduleService.getEligibleMovies).toHaveBeenCalledTimes(2);
  });

  it('blocks an eight-day request before calling the backend', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await configure(result, '2099-08-29');
    await act(async () => result.current.handleSubmit());
    expect(result.current.dateRangeInfo.isTooLong).toBe(true);
    expect(result.current.scheduleTo).toBe('2099-08-29');
    expect(result.current.dateRangeInfo.suggestedScheduleTo).toBe('2099-08-28');
    expect(adminAutoScheduleService.generatePreview).not.toHaveBeenCalled();
  });

  it('exposes a complete readiness summary for a valid configuration', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await configure(result);
    await waitFor(() => expect(result.current.isLoadingMovies).toBe(false));

    expect(result.current.readinessIssues).toEqual([]);
    expect(result.current.isReady).toBe(true);
    expect(result.current.selectedVersions).toEqual([
      expect.objectContaining({ publicId: 'version-1', movieTitle: 'Phim thử nghiệm' }),
    ]);
  });

  it('reuses the idempotency key for an unchanged failed retry', async () => {
    adminAutoScheduleService.generatePreview.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_CANDIDATE_LIMIT_EXCEEDED', message: 'too many',
    });
    const triggerToast = vi.fn();
    const { result } = renderHook(() => useAutoScheduleForm({ triggerToast }));
    await configure(result);

    await act(async () => result.current.handleSubmit());
    await act(async () => result.current.handleSubmit());

    expect(adminAutoScheduleService.generatePreview).toHaveBeenCalledTimes(2);
    const firstKey = adminAutoScheduleService.generatePreview.mock.calls[0][0].idempotencyKey;
    const secondKey = adminAutoScheduleService.generatePreview.mock.calls[1][0].idempotencyKey;
    expect(firstKey).toBeTruthy();
    expect(secondKey).toBe(firstKey);
    expect(triggerToast).toHaveBeenLastCalledWith(expect.stringContaining('quá nhiều suất đề xuất'), 'error');
  });

  it('rotates the idempotency key after a request-affecting edit', async () => {
    adminAutoScheduleService.generatePreview.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_TOO_MANY_CANDIDATES', message: 'too many',
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await configure(result);

    await act(async () => result.current.handleSubmit());
    act(() => result.current.setScheduleTo('2099-08-27'));
    await act(async () => result.current.handleSubmit());

    const [first, second] = adminAutoScheduleService.generatePreview.mock.calls;
    expect(second[0].idempotencyKey).not.toBe(first[0].idempotencyKey);
  });

  it('rotates the idempotency key after successful generation', async () => {
    adminAutoScheduleService.generatePreview.mockResolvedValue({
      success: true, data: { previewPublicId: 'preview-1' },
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await configure(result);

    await act(async () => result.current.handleSubmit());
    await act(async () => result.current.handleSubmit());

    const [first, second] = adminAutoScheduleService.generatePreview.mock.calls;
    expect(second[0].idempotencyKey).not.toBe(first[0].idempotencyKey);
  });
});
