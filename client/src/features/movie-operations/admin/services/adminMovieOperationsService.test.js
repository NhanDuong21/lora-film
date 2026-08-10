import { beforeEach, describe, expect, it, vi } from 'vitest';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminPricingService from '@/features/pricing/admin/services/adminPricingService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';
import adminMovieOperationsService from './adminMovieOperationsService';

vi.mock('@/features/catalog/admin/services/adminMovieService', () => ({
  default: { getMovieSummary: vi.fn() },
}));

vi.mock('@/features/facilities/admin/services/adminCinemaService', () => ({
  default: { getCinemas: vi.fn() },
}));

vi.mock('@/features/pricing/admin/services/adminPricingService', () => ({
  default: { searchPolicies: vi.fn() },
}));

vi.mock('@/features/scheduling/admin/services/adminShowtimeService', () => ({
  default: { getShowtimes: vi.fn() },
}));

const pageEnvelope = totalElements => ({
  data: {
    data: [],
    totalElements,
  },
});

describe('adminMovieOperationsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminMovieService.getMovieSummary.mockResolvedValue({
      data: { total: 10, ready: 7, warning: 2, blocked: 1 },
    });
    adminCinemaService.getCinemas.mockResolvedValue(pageEnvelope(3));
    adminShowtimeService.getShowtimes
      .mockResolvedValueOnce(pageEnvelope(4))
      .mockResolvedValueOnce(pageEnvelope(8));
    adminPricingService.searchPolicies.mockResolvedValue(pageEnvelope(2));
  });

  it('combines authoritative counts from existing Movie administration APIs', async () => {
    await expect(adminMovieOperationsService.getOverview()).resolves.toEqual({
      movies: { total: 10, ready: 7, warning: 2, blocked: 1 },
      activeCinemas: 3,
      draftShowtimes: 4,
      openShowtimes: 8,
      activePricePolicies: 2,
      unavailableSections: [],
    });

    expect(adminCinemaService.getCinemas).toHaveBeenCalledWith({
      page: 0,
      size: 1,
      status: 'ACTIVE',
    });
    expect(adminShowtimeService.getShowtimes).toHaveBeenNthCalledWith(1, {
      page: 0,
      size: 1,
      status: 'DRAFT',
    });
    expect(adminShowtimeService.getShowtimes).toHaveBeenNthCalledWith(2, {
      page: 0,
      size: 1,
      status: 'OPEN_FOR_BOOKING',
    });
  });

  it('keeps the dashboard usable when one independent section is unavailable', async () => {
    adminPricingService.searchPolicies.mockRejectedValueOnce(new Error('pricing unavailable'));

    const result = await adminMovieOperationsService.getOverview();

    expect(result.activePricePolicies).toBe(0);
    expect(result.activeCinemas).toBe(3);
    expect(result.unavailableSections).toEqual(['pricing']);
  });
});
