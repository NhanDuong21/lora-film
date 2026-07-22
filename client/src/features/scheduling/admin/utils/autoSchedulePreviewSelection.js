export const SELECTION_BLOCK_TYPES = Object.freeze({
  ITEM_NOT_FOUND: 'ITEM_NOT_FOUND',
  REJECTED: 'REJECTED',
  APPLIED: 'APPLIED',
  MALFORMED_ITEM: 'MALFORMED_ITEM',
  MALFORMED_SELECTED_ITEM: 'MALFORMED_SELECTED_ITEM',
  OCCUPANCY_OVERLAP: 'OCCUPANCY_OVERLAP',
});

const toEpoch = (value) => {
  if (!value) return null;
  const epoch = new Date(value).getTime();
  return Number.isFinite(epoch) ? epoch : null;
};

export const getAuditoriumKey = (item) => {
  const key = item?.auditoriumPublicId;
  return typeof key === 'string' && key.trim() ? key.trim() : null;
};

export const validatePreviewItemInterval = (item) => {
  const startMs = toEpoch(item?.startTime);
  const endMs = toEpoch(item?.endTime);
  const occupancyEndMs = toEpoch(item?.occupancyEndTime);
  const auditoriumKey = getAuditoriumKey(item);

  if (startMs === null || endMs === null || occupancyEndMs === null || !auditoriumKey) {
    return { valid: false, reason: 'MISSING_OR_INVALID_INTERVAL' };
  }
  if (endMs <= startMs || occupancyEndMs < endMs) {
    return { valid: false, reason: 'INVALID_INTERVAL_ORDER' };
  }

  return {
    valid: true,
    interval: {
      startMs,
      endMs,
      occupancyEndMs,
      auditoriumKey,
      item,
    },
  };
};

export const occupancyIntervalsOverlap = (firstInterval, secondInterval) =>
  firstInterval.startMs < secondInterval.occupancyEndMs
  && firstInterval.occupancyEndMs > secondInterval.startMs;

export const buildSelectedItemsIndex = (items, selectedItemIds) => {
  const selectedIds = selectedItemIds instanceof Set ? selectedItemIds : new Set(selectedItemIds || []);
  const byAuditorium = new Map();

  for (const item of items || []) {
    if (!selectedIds.has(item.itemPublicId)) continue;
    const validation = validatePreviewItemInterval(item);
    const auditoriumKey = getAuditoriumKey(item);
    if (!auditoriumKey) continue;

    if (!byAuditorium.has(auditoriumKey)) {
      byAuditorium.set(auditoriumKey, { intervals: [], malformedItems: [] });
    }
    const bucket = byAuditorium.get(auditoriumKey);
    if (validation.valid) bucket.intervals.push(validation.interval);
    else bucket.malformedItems.push(item);
  }

  for (const bucket of byAuditorium.values()) {
    bucket.intervals.sort((a, b) => a.startMs - b.startMs || a.occupancyEndMs - b.occupancyEndMs);
  }

  return { byAuditorium, selectedIds };
};

export const findSelectionBlock = (item, selectedItemsIndex) => {
  const validation = validatePreviewItemInterval(item);
  if (!validation.valid) {
    return { blocked: true, type: SELECTION_BLOCK_TYPES.MALFORMED_ITEM, item };
  }

  const bucket = selectedItemsIndex?.byAuditorium?.get(validation.interval.auditoriumKey);
  if (!bucket) return null;

  const malformedSelectedItem = bucket.malformedItems.find(
    (selectedItem) => selectedItem.itemPublicId !== item.itemPublicId,
  );
  if (malformedSelectedItem) {
    return {
      blocked: true,
      type: SELECTION_BLOCK_TYPES.MALFORMED_SELECTED_ITEM,
      item: malformedSelectedItem,
    };
  }

  const conflictingInterval = bucket.intervals.find(
    (selectedInterval) => selectedInterval.item.itemPublicId !== item.itemPublicId
      && occupancyIntervalsOverlap(validation.interval, selectedInterval),
  );

  return conflictingInterval
    ? {
        blocked: true,
        type: SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP,
        item: conflictingInterval.item,
      }
    : null;
};

export const validateSingleSelectionChange = (
  items,
  selectedItemIds,
  itemPublicId,
  newSelectedState,
) => {
  const item = (items || []).find((candidate) => candidate.itemPublicId === itemPublicId);
  if (!item) return { valid: false, type: SELECTION_BLOCK_TYPES.ITEM_NOT_FOUND };
  if (!newSelectedState) return { valid: true };
  if (item.validationStatus !== 'VALID') {
    return { valid: false, type: SELECTION_BLOCK_TYPES.REJECTED, item };
  }
  if (item.applyStatus === 'APPLIED') {
    return { valid: false, type: SELECTION_BLOCK_TYPES.APPLIED, item };
  }

  const selectedItemsIndex = buildSelectedItemsIndex(items, selectedItemIds);
  const block = findSelectionBlock(item, selectedItemsIndex);
  return block ? { valid: false, ...block } : { valid: true };
};

export const validateBulkSelection = (items, selectedItemIds) => {
  const ids = selectedItemIds instanceof Set ? selectedItemIds : new Set(selectedItemIds || []);
  const selectedItems = (items || []).filter((item) => ids.has(item.itemPublicId));
  if (selectedItems.length !== ids.size) {
    return { valid: false, type: SELECTION_BLOCK_TYPES.ITEM_NOT_FOUND };
  }

  const intervalsByAuditorium = new Map();
  for (const item of selectedItems) {
    if (item.validationStatus !== 'VALID') {
      return { valid: false, type: SELECTION_BLOCK_TYPES.REJECTED, item };
    }
    if (item.applyStatus === 'APPLIED') {
      return { valid: false, type: SELECTION_BLOCK_TYPES.APPLIED, item };
    }
    const validation = validatePreviewItemInterval(item);
    if (!validation.valid) {
      return { valid: false, type: SELECTION_BLOCK_TYPES.MALFORMED_ITEM, item };
    }
    const key = validation.interval.auditoriumKey;
    if (!intervalsByAuditorium.has(key)) intervalsByAuditorium.set(key, []);
    intervalsByAuditorium.get(key).push(validation.interval);
  }

  for (const intervals of intervalsByAuditorium.values()) {
    intervals.sort((a, b) => a.startMs - b.startMs || a.occupancyEndMs - b.occupancyEndMs);
    let maximumOccupancyEnd = null;
    let maximumInterval = null;
    for (const interval of intervals) {
      if (maximumOccupancyEnd !== null && interval.startMs < maximumOccupancyEnd) {
        return {
          valid: false,
          type: SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP,
          item: interval.item,
          conflictingItem: maximumInterval?.item,
        };
      }
      if (maximumOccupancyEnd === null || interval.occupancyEndMs > maximumOccupancyEnd) {
        maximumOccupancyEnd = interval.occupancyEndMs;
        maximumInterval = interval;
      }
    }
  }

  return { valid: true };
};

export const buildQuickNonOverlappingSelection = (items) => {
  const candidates = (items || [])
    .map((item, originalIndex) => ({ item, originalIndex, validation: validatePreviewItemInterval(item) }))
    .filter(({ item, validation }) =>
      item.validationStatus === 'VALID'
      && item.applyStatus !== 'APPLIED'
      && validation.valid,
    )
    .sort((a, b) =>
      a.validation.interval.startMs - b.validation.interval.startMs
      || a.originalIndex - b.originalIndex,
    );

  const lastSelectedByAuditorium = new Map();
  const selectedIds = [];

  for (const candidate of candidates) {
    const interval = candidate.validation.interval;
    const previous = lastSelectedByAuditorium.get(interval.auditoriumKey);
    if (previous && occupancyIntervalsOverlap(interval, previous)) continue;

    selectedIds.push(candidate.item.itemPublicId);
    lastSelectedByAuditorium.set(interval.auditoriumKey, interval);
  }

  return selectedIds;
};

export const getMalformedPreviewItems = (items) =>
  (items || []).filter((item) => !validatePreviewItemInterval(item).valid);
