import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminPricingService from '@/features/pricing/admin/services/adminPricingService';
import adminShowtimeService from '@/features/scheduling/admin/services/adminShowtimeService';

const pageTotal = envelope => Number(
  envelope?.data?.totalElements
  ?? envelope?.data?.data?.length
  ?? 0,
);

const settledValue = (result, fallback) => (
  result.status === 'fulfilled' ? result.value : fallback
);

const adminMovieOperationsService = {
  getOverview: async () => {
    const results = await Promise.allSettled([
      adminMovieService.getMovieSummary(),
      adminCinemaService.getCinemas({ page: 0, size: 1, status: 'ACTIVE' }),
      adminShowtimeService.getShowtimes({ page: 0, size: 1, status: 'DRAFT' }),
      adminShowtimeService.getShowtimes({ page: 0, size: 1, status: 'OPEN_FOR_BOOKING' }),
      adminPricingService.searchPolicies({ page: 0, size: 1, status: 'ACTIVE' }),
    ]);

    const movieEnvelope = settledValue(results[0], null);
    const cinemaEnvelope = settledValue(results[1], null);
    const draftShowtimeEnvelope = settledValue(results[2], null);
    const openShowtimeEnvelope = settledValue(results[3], null);
    const pricingEnvelope = settledValue(results[4], null);

    return {
      movies: movieEnvelope?.data || null,
      activeCinemas: pageTotal(cinemaEnvelope),
      draftShowtimes: pageTotal(draftShowtimeEnvelope),
      openShowtimes: pageTotal(openShowtimeEnvelope),
      activePricePolicies: pageTotal(pricingEnvelope),
      unavailableSections: [
        !movieEnvelope && 'movies',
        !cinemaEnvelope && 'cinemas',
        !draftShowtimeEnvelope && 'draftShowtimes',
        !openShowtimeEnvelope && 'openShowtimes',
        !pricingEnvelope && 'pricing',
      ].filter(Boolean),
    };
  },
};

export default adminMovieOperationsService;
