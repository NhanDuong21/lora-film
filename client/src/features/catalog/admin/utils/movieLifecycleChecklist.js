export function evaluatePublishChecklist(movieDetail) {
  if (!movieDetail) {
    return {
      hasGenre: 'UNKNOWN',
      hasActiveVersion: 'UNKNOWN',
      hasPrimaryPoster: 'UNKNOWN',
      isReady: false
    };
  }

  const { genres = [], versions = [], media = [] } = movieDetail;

  const hasGenre = genres.length > 0;
  const hasActiveVersion = versions.some(v => v.status === 'ACTIVE');
  const hasPrimaryPoster = media.some(
    m => m.mediaType === 'POSTER' && m.isPrimary && m.status === 'ACTIVE'
  );

  return {
    hasGenre: hasGenre ? 'PASS' : 'MISSING',
    hasActiveVersion: hasActiveVersion ? 'PASS' : 'MISSING',
    hasPrimaryPoster: hasPrimaryPoster ? 'PASS' : 'MISSING',
    isReady: hasGenre && hasActiveVersion && hasPrimaryPoster
  };
}
