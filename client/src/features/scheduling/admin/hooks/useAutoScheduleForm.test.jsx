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
    await waitFor(() => expect(result.current.selectedAuditoriumIds).toEqual(['auditorium-1']));
    await waitFor(() => expect(result.current.movies).toHaveLength(1));
    act(() => {
      result.current.setScheduleFrom('2099-08-22');
      result.current.setScheduleTo(scheduleTo);
      result.current.toggleVersion('version-1');
    });
  };

  it('blocks an eight-day request before calling the backend', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await configure(result, '2099-08-29');
    await act(async () => result.current.handleSubmit());
    expect(result.current.dateRangeInfo.isTooLong).toBe(true);
    expect(result.current.scheduleTo).toBe('2099-08-29');
    expect(result.current.dateRangeInfo.suggestedScheduleTo).toBe('2099-08-28');
    expect(adminAutoScheduleService.generatePreview).not.toHaveBeenCalled();
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
    expect(triggerToast).toHaveBeenLastCalledWith(expect.stringContaining('quá nhiều ứng viên'), 'error');
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
