export const SCHEDULABLE_MOVIE_STATUSES = Object.freeze(['UPCOMING', 'NOW_SHOWING']);

const schedulableMovieStatuses = new Set(SCHEDULABLE_MOVIE_STATUSES);

export const isSchedulableMovieStatus = status => schedulableMovieStatuses.has(status);
