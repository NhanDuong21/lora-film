/**
 * Pure utility to evaluate data quality warnings based solely on available list DTO fields.
 * Does not check for nested relations like versions or media, as they are not reliably in the list DTO.
 */
export const getMovieListWarnings = (movie) => {
  const warnings = [];
  
  if (!movie) return warnings;

  // Check title
  if (!movie.title || movie.title.trim() === '') {
    warnings.push({ code: 'MISSING_TITLE', label: 'Thiếu tên phim' });
  }

  // Check poster
  if (!movie.primaryPosterUrl && !movie.primaryPoster) {
    warnings.push({ code: 'MISSING_POSTER', label: 'Thiếu poster' });
  }

  // Check release date
  if (!movie.releaseDate) {
    warnings.push({ code: 'MISSING_RELEASE_DATE', label: 'Thiếu ngày phát hành' });
  }

  // Check duration (only if the field is provided in the DTO; assuming it could be 0 or null)
  if (movie.durationMinutes !== undefined && movie.durationMinutes !== null) {
    if (movie.durationMinutes <= 0) {
      warnings.push({ code: 'INVALID_DURATION', label: 'Thời lượng không hợp lệ' });
    } else if (movie.durationMinutes < 30) {
      warnings.push({ code: 'SUSPICIOUS_DURATION', label: `Cần kiểm tra thời lượng: ${movie.durationMinutes} phút` });
    }
  }

  // Check age rating
  if (!movie.ageRating) {
    warnings.push({ code: 'MISSING_AGE_RATING', label: 'Thiếu phân loại độ tuổi' });
  }

  return warnings;
};
