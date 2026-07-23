import { AlertTriangle } from 'lucide-react';
import {
  formatCinemaTime,
  formatCinemaTimeRange,
  compareServiceDateKeys,
  formatServiceDateKey,
  getTimelineRange,
  TIMELINE_END_HOUR,
  TIMELINE_START_HOUR,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewDateTime';
import {
  findSelectionBlock,
  SELECTION_BLOCK_TYPES,
  validatePreviewItemInterval,
} from '@/features/scheduling/admin/utils/autoSchedulePreviewSelection';

const AutoScheduleTimeline = ({
  groupedItems,
  selectedItemIds,
  selectedItemsIndex,
  handleToggleSelection,
  isSelectionBusy,
  canSelect,
  timezone,
}) => {
  const hours = Array.from(
    { length: TIMELINE_END_HOUR - TIMELINE_START_HOUR + 1 },
    (_, index) => index + TIMELINE_START_HOUR,
  );

  return (
    <div className="space-y-12 w-full max-w-full overflow-x-auto">
      {Object.keys(groupedItems).sort(compareServiceDateKeys).map(dateKey => {
        const dateItems = Object.values(groupedItems[dateKey]).flat();
        const rangesByItemId = new Map(
          dateItems.map(item => [
            item.itemPublicId,
            getTimelineRange(item.startTime, item.endTime, timezone),
          ]),
        );
        const outsideRangeCount = dateItems.filter(
          item => rangesByItemId.get(item.itemPublicId)?.isOutsideRange,
        ).length;

        return (
          <div key={dateKey} className="space-y-6 min-w-[800px]">
            <div className="border-b border-zinc-800 pb-2 sticky left-0">
              <h2 className="text-lg font-black text-white flex items-center gap-3">
                {formatServiceDateKey(dateKey, { weekday: true })}
              </h2>
              {outsideRangeCount > 0 && (
                <p className="mt-2 text-xs text-amber-400 flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  {outsideRangeCount} suất nằm ngoài khung 08:00–24:00. Xem đầy đủ trong chế độ Danh sách.
                </p>
              )}
            </div>

            <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-2xl p-4">
              <div className="flex relative border-b border-zinc-800 pb-2 mb-4 ml-28">
                {hours.map(hour => (
                  <div key={hour} className="flex-1 text-left text-[10px] font-bold text-zinc-500 border-l border-zinc-800/50 pl-1">
                    {String(hour).padStart(2, '0')}:00
                  </div>
                ))}
              </div>

              <div className="space-y-4">
                {Object.keys(groupedItems[dateKey]).sort().map(audKey => {
                  const audItems = groupedItems[dateKey][audKey];

                  return (
                    <div key={audKey} className="flex items-stretch gap-4 relative group">
                      <div className="w-24 flex-shrink-0 flex items-center border-r border-zinc-800 pr-2">
                        <span className="text-xs font-bold text-zinc-300 truncate" title={audKey}>{audKey}</span>
                      </div>

                      <div className="flex-1 relative h-14 bg-zinc-950/50 rounded-lg overflow-hidden border border-zinc-800/50">
                        <div className="absolute inset-0 flex pointer-events-none">
                          {hours.map(hour => (
                            <div key={hour} className="flex-1 border-r border-zinc-800/30 h-full" />
                          ))}
                        </div>

                        {audItems.map(item => {
                          const range = rangesByItemId.get(item.itemPublicId);
                          if (!range?.isVisible) return null;

                          const isValid = item.validationStatus === 'VALID';
                          const isSelected = selectedItemIds.has(item.itemPublicId);
                          const isPending = item.applyStatus === 'PENDING';
                          const isCreated = item.applyStatus === 'CREATED';
                          const isApplyFailure = item.applyStatus === 'CONFLICT'
                            || item.applyStatus === 'FAILED';
                          const isSkipped = item.applyStatus === 'SKIPPED';
                          const selectionBlock = canSelect && isValid && isPending && !isSelected
                            ? findSelectionBlock(item, selectedItemsIndex)
                            : null;
                          const isConflicting = selectionBlock?.type === SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP;
                          const hasMalformedData = !validatePreviewItemInterval(item).valid
                            || (selectionBlock
                              && selectionBlock.type !== SELECTION_BLOCK_TYPES.OCCUPANCY_OVERLAP);
                          const isDisabled = !canSelect
                            || !isPending
                            || isSelectionBusy
                            || Boolean(selectionBlock);

                          let bgColor;
                          if (isCreated) bgColor = 'bg-green-500/20 border-green-500/50 text-green-300';
                          else if (isApplyFailure) bgColor = 'bg-red-500/20 border-red-500/50 text-red-300';
                          else if (isSkipped) bgColor = 'bg-zinc-800 border-zinc-700 text-zinc-400';
                          else if (!isValid) bgColor = 'bg-red-500/20 border-red-500/50 text-red-300';
                          else if (hasMalformedData) bgColor = 'bg-amber-500/10 border-amber-500/30 text-amber-400 opacity-70';
                          else if (isConflicting) bgColor = 'bg-red-500/10 border-red-500/30 text-red-500/50 grayscale opacity-50';
                          else if (isSelected) bgColor = 'bg-brand-orange text-zinc-950 border-brand-orange shadow-[0_0_10px_rgba(255,165,0,0.3)]';
                          else bgColor = 'bg-blue-500/20 border-blue-500/40 text-blue-300 cursor-pointer hover:bg-blue-500/40 hover:border-blue-400';

                          const occupancyLabel = item.occupancyEndTime
                            ? `\nChiếm phòng đến ${formatCinemaTime(item.occupancyEndTime, timezone)}`
                            : '\nThiếu dữ liệu chiếm phòng';
                          const statusLabel = !isValid
                            ? 'Từ chối'
                            : hasMalformedData
                              ? 'Thiếu dữ liệu chiếm phòng'
                              : isConflicting
                                ? 'Xung đột khoảng chiếm phòng'
                                : '';

                          return (
                            <div
                              key={item.itemPublicId}
                              onClick={() => {
                                if (isValid && isPending && !isDisabled) {
                                  handleToggleSelection(item.itemPublicId, isSelected);
                                }
                              }}
                              className={`absolute top-1 bottom-1 rounded border overflow-hidden flex flex-col justify-center px-1.5 transition-all ${bgColor} ${isDisabled && !isSelected && isValid ? 'cursor-not-allowed' : ''}`}
                              style={{ left: range.left, width: range.width }}
                              title={`${item.movieTitle}\n${formatCinemaTimeRange(item.startTime, item.endTime, timezone)}${occupancyLabel}\n${statusLabel}`}
                            >
                              <span className="text-[9px] font-bold truncate leading-tight whitespace-nowrap">{item.movieTitle}</span>
                              <span className="text-[8px] opacity-80 truncate leading-tight">
                                {formatCinemaTime(item.startTime, timezone)}-{formatCinemaTime(item.endTime, timezone)}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default AutoScheduleTimeline;
