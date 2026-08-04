const uniqueIds = values => Array.from(new Set(values.filter(value => (
  typeof value === 'string' && value.trim().length > 0
))));

export const buildAutoScheduleRecreateDraft = (preview, items = []) => ({
  cinemaPublicId: preview?.cinemaPublicId || '',
  scheduleFrom: preview?.scheduleFrom || '',
  scheduleTo: preview?.scheduleTo || '',
  slotGranularityMinutes: preview?.slotGranularityMinutes || 15,
  auditoriumPublicIds: uniqueIds(items.map(item => item.auditoriumPublicId)),
  movieVersionPublicIds: uniqueIds(items.map(item => item.movieVersionPublicId)),
});

export default buildAutoScheduleRecreateDraft;
