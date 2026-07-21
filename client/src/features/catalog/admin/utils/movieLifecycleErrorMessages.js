export const MOVIE_LIFECYCLE_ERRORS = {
  MOVIE_NOT_FOUND: 'Không tìm thấy phim.',
  MOVIE_PUBLISH_VALIDATION_FAILED: 'Phim chưa có thể loại hoặc chưa đáp ứng điều kiện phát hành.',
  MOVIE_ACTIVE_VERSION_REQUIRED: 'Phim cần ít nhất một phiên bản đang hoạt động.',
  MOVIE_PRIMARY_POSTER_REQUIRED: 'Phim cần có một poster chính (hình ảnh đang hoạt động).',
  INVALID_MOVIE_STATUS_TRANSITION: 'Không thể chuyển phim sang trạng thái này.'
};

export function getLifecycleErrorMessage(errorCode, fallbackMessage) {
  if (MOVIE_LIFECYCLE_ERRORS[errorCode]) {
    return MOVIE_LIFECYCLE_ERRORS[errorCode];
  }
  return fallbackMessage || 'Không thể cập nhật trạng thái phim. Vui lòng thử lại.';
}
