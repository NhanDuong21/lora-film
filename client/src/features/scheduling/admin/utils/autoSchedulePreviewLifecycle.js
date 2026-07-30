import { CANDIDATE_APPLY_PRESENTATION } from './schedulingPresentation';

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

export const CANDIDATE_APPLY_STATE_META = CANDIDATE_APPLY_PRESENTATION;

const LIFECYCLE_MESSAGES = Object.freeze({
  [PREVIEW_LIFECYCLE_STATUS.GENERATING]: 'Hệ thống đang xếp giờ chiếu. Danh sách đề xuất sẽ xuất hiện sau ít phút.',
  [PREVIEW_LIFECYCLE_STATUS.PREVIEWED]: 'Lịch đề xuất đã sẵn sàng để bạn kiểm tra trước khi tạo suất chiếu.',
  [PREVIEW_LIFECYCLE_STATUS.APPLYING]: 'Hệ thống đang tạo các suất chiếu ở trạng thái đang soạn. Các thao tác chỉnh sửa tạm thời bị khóa.',
  [PREVIEW_LIFECYCLE_STATUS.APPLIED]: 'Các suất chiếu đã được tạo ở trạng thái đang soạn và lịch này hiện chỉ còn để xem.',
  [PREVIEW_LIFECYCLE_STATUS.FAILED]: 'Không thể tạo bản lịch. Hãy làm mới hoặc lập một bản lịch khác.',
  [PREVIEW_LIFECYCLE_STATUS.EXPIRED]: 'Lịch đang soạn đã hết hạn. Hãy tạo lịch mới để tiếp tục.',
  [PREVIEW_LIFECYCLE_STATUS.CANCELLED]: 'Lịch này đã bị hủy và chỉ còn để tra cứu lịch sử.',
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
      || 'Trạng thái bản lịch chưa được hỗ trợ. Dữ liệu đang được hiển thị ở chế độ chỉ xem.',
    failureReasonSafe: getSafePreviewFailureReason(preview),
  };
};

export const getCandidateApplyStateMeta = applyStatus => (
  CANDIDATE_APPLY_STATE_META[applyStatus] || {
    label: 'Không xác định',
    description: 'Trạng thái của suất đề xuất này chưa xác định.',
    tone: 'zinc',
  }
);

export const isCandidateSelectable = (item, capabilities) => (
  Boolean(capabilities?.canSelect)
  && item?.validationStatus === 'VALID'
  && item?.applyStatus === CANDIDATE_APPLY_STATE.PENDING
);
