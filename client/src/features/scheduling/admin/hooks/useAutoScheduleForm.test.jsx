import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminAutoScheduleService from '../services/adminAutoScheduleService';
import useAutoScheduleForm from './useAutoScheduleForm';

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn(), getAdminCinemaDetail: vi.fn() },
}));
vi.mock('../services/adminAutoScheduleService', () => ({
  default: { preflight: vi.fn(), getEligibleMovies: vi.fn(), generatePreview: vi.fn() },
}));

const cinema = { publicId: 'cinema-1', name: 'Lora Cinema', timezone: 'Asia/Ho_Chi_Minh' };
const auditorium = { publicId: 'aud-1', name: 'Phòng 1', status: 'ACTIVE', screenType: '2D' };
const movie = {
  moviePublicId: 'movie-1', title: 'Phim A', eligible: true, status: 'NOW_SHOWING', reasons: [],
  versions: [{ publicId: 'version-1', versionName: '2D', status: 'ACTIVE' }],
};
const readyPreflight = {
  canGenerate: true,
  planningFrom: '2099-08-22',
  planningTo: '2099-08-22',
  timezone: 'Asia/Ho_Chi_Minh',
  eligibleMovieCount: 1,
  eligibleVersionCount: 1,
  eligibleAuditoriumCount: 1,
  compatiblePairCount: 1,
  blockers: [],
};

describe('useAutoScheduleForm Quick Mode', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminCinemaService.getCinemas.mockResolvedValue({ success: true, data: { data: [cinema] } });
    adminCinemaService.getAdminCinemaDetail.mockResolvedValue({
      success: true, data: { ...cinema, activeAuditoriums: [auditorium] },
    });
    adminAutoScheduleService.preflight.mockResolvedValue({ success: true, data: readyPreflight });
    adminAutoScheduleService.getEligibleMovies.mockResolvedValue({ success: true, data: [movie] });
  });

  const selectCinema = async result => {
    await waitFor(() => expect(result.current.cinemas).toHaveLength(1));
    act(() => result.current.setSelectedCinemaId('cinema-1'));
    await waitFor(() => expect(result.current.preflight?.canGenerate).toBe(true));
  };

  it('automatically preflights tomorrow without requiring rooms or movie versions', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);

    expect(adminAutoScheduleService.preflight).toHaveBeenCalledWith(expect.objectContaining({
      cinemaPublicId: 'cinema-1', planningDays: 1,
    }));
    expect(result.current.includeAuditoriumIds).toEqual([]);
    expect(result.current.includeMovieVersionIds).toEqual([]);
    expect(result.current.isReady).toBe(true);
  });

  it('supports only the 1/3/7-day presets and re-runs preflight', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);
    act(() => result.current.setPlanningPreset(3));
    await waitFor(() => expect(adminAutoScheduleService.preflight).toHaveBeenLastCalledWith(
      expect.objectContaining({ planningDays: 3 }),
    ));
    act(() => result.current.setPlanningPreset(2));
    expect(result.current.planningDays).toBe(3);
  });

  it('keeps include and exclude choices mutually exclusive', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);
    act(() => result.current.setScopeChoice('include', 'auditorium:aud-1', true));
    expect(result.current.includeAuditoriumIds).toEqual(['aud-1']);
    act(() => result.current.setScopeChoice('exclude', 'auditorium:aud-1', true));
    expect(result.current.includeAuditoriumIds).toEqual([]);
    expect(result.current.excludeAuditoriumIds).toEqual(['aud-1']);
  });

  it('surfaces blockers and prevents generation', async () => {
    adminAutoScheduleService.preflight.mockResolvedValue({
      success: true,
      data: { ...readyPreflight, canGenerate: false, blockers: [{ code: 'PRICING_INCOMPLETE', message: 'Thiếu bảng giá' }] },
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await waitFor(() => expect(result.current.cinemas).toHaveLength(1));
    act(() => result.current.setSelectedCinemaId('cinema-1'));
    await waitFor(() => expect(result.current.preflight?.canGenerate).toBe(false));
    expect(result.current.readinessIssues).toContain('Thiếu bảng giá');
    expect(result.current.isReady).toBe(false);
    await act(async () => result.current.handleSubmit());
    expect(adminAutoScheduleService.generatePreview).not.toHaveBeenCalled();
  });

  it('submits the authoritative planning preset and no manual dates', async () => {
    adminAutoScheduleService.generatePreview.mockResolvedValue({
      success: true, data: { previewPublicId: 'preview-1' },
    });
    const onSuccess = vi.fn();
    const { result } = renderHook(() => useAutoScheduleForm({ onSuccess }));
    await selectCinema(result);
    act(() => result.current.setPlanningPreset(7));
    await waitFor(() => expect(result.current.preflight?.canGenerate).toBe(true));
    await act(async () => result.current.handleSubmit());

    expect(adminAutoScheduleService.generatePreview).toHaveBeenCalledWith(expect.objectContaining({
      cinemaPublicId: 'cinema-1', planningDays: 7, slotGranularityMinutes: 15,
    }));
    const payload = adminAutoScheduleService.generatePreview.mock.calls[0][0];
    expect(payload).not.toHaveProperty('scheduleFrom');
    expect(payload).not.toHaveProperty('scheduleTo');
    expect(payload).not.toHaveProperty('auditoriumPublicIds');
    expect(payload).not.toHaveProperty('movieVersionPublicIds');
    expect(onSuccess).toHaveBeenCalledWith('preview-1');
  });

  it('reuses an idempotency key after an unchanged failed retry', async () => {
    adminAutoScheduleService.generatePreview.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_SOLVER_TIMEOUT', message: 'timeout',
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);
    await act(async () => result.current.handleSubmit());
    await act(async () => result.current.handleSubmit());
    const [first, second] = adminAutoScheduleService.generatePreview.mock.calls;
    expect(first[0].idempotencyKey).toBeTruthy();
    expect(second[0].idempotencyKey).toBe(first[0].idempotencyKey);
  });

  it('restores a legacy draft as advanced constraints and infers a supported preset', async () => {
    const { result } = renderHook(() => useAutoScheduleForm({
      initialDraft: {
        cinemaPublicId: 'cinema-1', scheduleFrom: '2099-08-22', scheduleTo: '2099-08-24',
        auditoriumPublicIds: ['aud-1'], movieVersionPublicIds: ['version-1'],
      },
    }));
    await waitFor(() => expect(result.current.preflight?.canGenerate).toBe(true));
    expect(result.current.planningDays).toBe(3);
    expect(result.current.includeAuditoriumIds).toEqual(['aud-1']);
    expect(result.current.includeMovieVersionIds).toEqual(['version-1']);
  });

  it('submits only non-empty advanced include and exclude filters', async () => {
    adminAutoScheduleService.generatePreview.mockResolvedValue({
      success: true, data: { previewPublicId: 'preview-advanced' },
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);
    act(() => {
      result.current.setScopeChoice('include', 'auditorium:aud-1', true);
      result.current.setScopeChoice('exclude', 'version:version-1', true);
    });
    await waitFor(() => expect(result.current.preflight?.canGenerate).toBe(true));

    await act(async () => result.current.handleSubmit());

    expect(adminAutoScheduleService.generatePreview).toHaveBeenCalledWith(expect.objectContaining({
      auditoriumPublicIds: ['aud-1'],
      excludeMovieVersionPublicIds: ['version-1'],
    }));
    const payload = adminAutoScheduleService.generatePreview.mock.calls[0][0];
    expect(payload).not.toHaveProperty('movieVersionPublicIds');
    expect(payload).not.toHaveProperty('excludeAuditoriumPublicIds');
  });

  it('rotates the idempotency key after a planning-preset edit', async () => {
    adminAutoScheduleService.generatePreview.mockRejectedValue({
      errorCode: 'AUTO_SCHEDULE_SOLVER_TIMEOUT', message: 'timeout',
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);
    await act(async () => result.current.handleSubmit());
    act(() => result.current.setPlanningPreset(3));
    await waitFor(() => expect(adminAutoScheduleService.preflight).toHaveBeenLastCalledWith(
      expect.objectContaining({ planningDays: 3 }),
    ));
    await act(async () => result.current.handleSubmit());

    const [first, second] = adminAutoScheduleService.generatePreview.mock.calls;
    expect(second[0].idempotencyKey).not.toBe(first[0].idempotencyKey);
  });

  it('rotates the idempotency key after each successful preview', async () => {
    adminAutoScheduleService.generatePreview.mockResolvedValue({
      success: true, data: { previewPublicId: 'preview-1' },
    });
    const { result } = renderHook(() => useAutoScheduleForm({}));
    await selectCinema(result);

    await act(async () => result.current.handleSubmit());
    await act(async () => result.current.handleSubmit());

    const [first, second] = adminAutoScheduleService.generatePreview.mock.calls;
    expect(second[0].idempotencyKey).not.toBe(first[0].idempotencyKey);
  });

  it('exposes a preflight transport failure and allows an explicit retry', async () => {
    adminAutoScheduleService.preflight
      .mockRejectedValueOnce({ message: 'analytics offline' })
      .mockResolvedValueOnce({ success: true, data: readyPreflight });
    const triggerToast = vi.fn();
    const { result } = renderHook(() => useAutoScheduleForm({ triggerToast }));
    await waitFor(() => expect(result.current.cinemas).toHaveLength(1));
    act(() => result.current.setSelectedCinemaId('cinema-1'));
    await waitFor(() => expect(result.current.preflightError).toBeTruthy());

    await act(async () => result.current.runPreflight());

    expect(result.current.preflight?.canGenerate).toBe(true);
    expect(result.current.preflightError).toBe('');
    expect(adminAutoScheduleService.preflight).toHaveBeenCalledTimes(2);
  });
});
