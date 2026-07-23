export const PREVIEW_LIFECYCLE_STATUS = Object.freeze({
  GENERATING: 'GENERATING',
  PREVIEWED: 'PREVIEWED',
  APPLYING: 'APPLYING',
  APPLIED: 'APPLIED',
  FAILED: 'FAILED',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED',
});

export const CANDIDATE_APPLY_STATE = Object.freeze({
  PENDING: 'PENDING',
  CREATED: 'CREATED',
  SKIPPED: 'SKIPPED',
  CONFLICT: 'CONFLICT',
  FAILED: 'FAILED',
});

export const CANDIDATE_APPLY_STATE_META = Object.freeze({
  [CANDIDATE_APPLY_STATE.PENDING]: {
    label: 'Đang chờ',
    description: 'Ứng viên vẫn đang chờ lựa chọn hoặc áp dụng.',
    tone: 'blue',
  },
  [CANDIDATE_APPLY_STATE.CREATED]: {
    label: 'Đã tạo suất chiếu',
    description: 'Một suất chiếu chính thức đã được tạo từ ứng viên này.',
    tone: 'green',
  },
  [CANDIDATE_APPLY_STATE.SKIPPED]: {
    label: 'Đã bỏ qua',
    description: 'Ứng viên không được chọn khi bản xem trước được áp dụng.',
    tone: 'zinc',
  },
  [CANDIDATE_APPLY_STATE.CONFLICT]: {
    label: 'Xung đột khi áp dụng',
    description: 'Ứng viên gặp xung đột trong quá trình áp dụng.',
    tone: 'red',
  },
  [CANDIDATE_APPLY_STATE.FAILED]: {
    label: 'Áp dụng thất bại',
    description: 'Không thể tạo suất chiếu từ ứng viên này.',
    tone: 'red',
  },
});

const LIFECYCLE_MESSAGES = Object.freeze({
  [PREVIEW_LIFECYCLE_STATUS.GENERATING]: 'Bản xem trước đang được tạo. Dữ liệu lựa chọn chưa sẵn sàng.',
  [PREVIEW_LIFECYCLE_STATUS.PREVIEWED]: 'Bản xem trước đã sẵn sàng để rà soát và áp dụng.',
  [PREVIEW_LIFECYCLE_STATUS.APPLYING]: 'Hệ thống đang áp dụng bản xem trước. Các thao tác chỉnh sửa đã bị khóa.',
  [PREVIEW_LIFECYCLE_STATUS.APPLIED]: 'Bản xem trước đã được áp dụng và hiện ở chế độ chỉ đọc.',
  [PREVIEW_LIFECYCLE_STATUS.FAILED]: 'Không thể tạo bản xem trước. Hãy làm mới hoặc tạo một bản xem trước mới.',
  [PREVIEW_LIFECYCLE_STATUS.EXPIRED]: 'Bản xem trước đã hết hạn và không thể chỉnh sửa hoặc áp dụng.',
  [PREVIEW_LIFECYCLE_STATUS.CANCELLED]: 'Bản xem trước đã bị hủy và chỉ còn để tra cứu lịch sử.',
});

const toEpoch = value => {
  const epoch = value ? new Date(value).getTime() : Number.NaN;
  return Number.isFinite(epoch) ? epoch : null;
};

export const getEffectivePreviewStatus = (preview, now = Date.now()) => {
  const persistedStatus = preview?.status || null;
  if (persistedStatus !== PREVIEW_LIFECYCLE_STATUS.PREVIEWED) return persistedStatus;

  const expiresAt = toEpoch(preview?.expiresAt);
  return expiresAt !== null && expiresAt <= now
    ? PREVIEW_LIFECYCLE_STATUS.EXPIRED
    : PREVIEW_LIFECYCLE_STATUS.PREVIEWED;
};

export const getSafePreviewFailureReason = preview => (
  preview?.status === PREVIEW_LIFECYCLE_STATUS.FAILED
    ? LIFECYCLE_MESSAGES[PREVIEW_LIFECYCLE_STATUS.FAILED]
    : null
);

export const derivePreviewCapabilities = (
  preview,
  {
    selectedCount = 0,
    isSnapshotUpdating = false,
    isApplying = false,
    isUpdatingSelection = false,
    hasUnsafeSnapshot = false,
    now = Date.now(),
  } = {},
) => {
  const effectiveStatus = getEffectivePreviewStatus(preview, now);
  const isEditable = effectiveStatus === PREVIEW_LIFECYCLE_STATUS.PREVIEWED;
  const isApplicable = isEditable
    && preview?.applyMode === 'ALL_OR_NOTHING'
    && selectedCount > 0;
  const mutationLocked = isSnapshotUpdating
    || isApplying
    || isUpdatingSelection
    || hasUnsafeSnapshot;

  return {
    effectiveStatus,
    isReadOnly: !isEditable,
    isEditable,
    isApplicable,
    canRefresh: !isSnapshotUpdating && !isApplying && !isUpdatingSelection,
    canSelect: isEditable && !mutationLocked,
    canApply: isApplicable && !mutationLocked,
    lifecycleMessage: LIFECYCLE_MESSAGES[effectiveStatus]
      || 'Trạng thái bản xem trước chưa được hỗ trợ. Dữ liệu được hiển thị ở chế độ chỉ đọc.',
    failureReasonSafe: getSafePreviewFailureReason(preview),
  };
};

export const getCandidateApplyStateMeta = applyStatus => (
  CANDIDATE_APPLY_STATE_META[applyStatus] || {
    label: 'Không xác định',
    description: 'Trạng thái áp dụng của ứng viên không xác định.',
    tone: 'zinc',
  }
);

export const isCandidateSelectable = (item, capabilities) => (
  Boolean(capabilities?.canSelect)
  && item?.validationStatus === 'VALID'
  && item?.applyStatus === CANDIDATE_APPLY_STATE.PENDING
);
