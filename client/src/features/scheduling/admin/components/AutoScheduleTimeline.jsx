import React from 'react';
import { Info, CheckCircle2, Clock } from 'lucide-react';

const formatTime = (isoString) => {
  if (!isoString) return '';
  const d = new Date(isoString);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

// Calculate left position and width based on time within 08:00 to 24:00 range
const getStylesForTimeRange = (startIso, endIso) => {
  const START_HOUR = 8;
  const END_HOUR = 24;
  const TOTAL_MINUTES = (END_HOUR - START_HOUR) * 60;

  const startDate = new Date(startIso);
  const endDate = new Date(endIso);

  const startMinutes = (startDate.getHours() * 60 + startDate.getMinutes()) - (START_HOUR * 60);
  const endMinutes = (endDate.getHours() * 60 + endDate.getMinutes()) - (START_HOUR * 60);

  const leftPercent = Math.max(0, (startMinutes / TOTAL_MINUTES) * 100);
  let widthPercent = ((endMinutes - startMinutes) / TOTAL_MINUTES) * 100;
  
  // Handle cross-midnight by capping at 100%
  if (endDate.getDate() > startDate.getDate() || endDate.getHours() < START_HOUR) {
    widthPercent = 100 - leftPercent;
  }

  return {
    left: `${leftPercent}%`,
    width: `${Math.max(2, widthPercent)}%` // min width 2%
  };
};

const AutoScheduleTimeline = ({
  groupedItems,
  selectedItemIds,
  handleToggleSelection,
  isCheckboxDisabledFunc,
  checkOverlapFunc
}) => {

  const hours = Array.from({ length: 17 }, (_, i) => i + 8); // 8 to 24

  return (
    <div className="space-y-12 w-full max-w-full overflow-x-auto">
      {Object.keys(groupedItems).sort().map(dateKey => (
        <div key={dateKey} className="space-y-6 min-w-[800px]">
          <h2 className="text-lg font-black text-white flex items-center gap-3 border-b border-zinc-800 pb-2 sticky left-0">
            {new Date(dateKey).toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
          </h2>

          <div className="bg-zinc-900/40 border border-zinc-800/80 rounded-2xl p-4">
            {/* Timeline Header (Hours) */}
            <div className="flex relative border-b border-zinc-800 pb-2 mb-4 ml-28">
              {hours.map(hour => (
                <div key={hour} className="flex-1 text-left text-[10px] font-bold text-zinc-500 border-l border-zinc-800/50 pl-1">
                  {String(hour).padStart(2, '0')}:00
                </div>
              ))}
            </div>

            {/* Auditorium Rows */}
            <div className="space-y-4">
              {Object.keys(groupedItems[dateKey]).sort().map(audKey => {
                const audItems = groupedItems[dateKey][audKey];
                
                return (
                  <div key={audKey} className="flex items-stretch gap-4 relative group">
                    <div className="w-24 flex-shrink-0 flex items-center border-r border-zinc-800 pr-2">
                      <span className="text-xs font-bold text-zinc-300 truncate" title={audKey}>{audKey}</span>
                    </div>

                    <div className="flex-1 relative h-14 bg-zinc-950/50 rounded-lg overflow-hidden border border-zinc-800/50">
                      {/* Grid Lines */}
                      <div className="absolute inset-0 flex pointer-events-none">
                        {hours.map(hour => (
                          <div key={hour} className="flex-1 border-r border-zinc-800/30 h-full"></div>
                        ))}
                      </div>

                      {/* Items */}
                      {audItems.map(item => {
                        const isValid = item.validationStatus === 'VALID';
                        const isSelected = selectedItemIds.has(item.itemPublicId);
                        const isItemApplied = item.applyStatus === 'APPLIED';
                        
                        let isConflicting = false;
                        if (isValid && !isSelected && !isItemApplied) {
                          isConflicting = checkOverlapFunc(item, audItems, selectedItemIds);
                        }

                        const isDisabled = isCheckboxDisabledFunc(isConflicting);
                        const { left, width } = getStylesForTimeRange(item.startTime, item.endTime);

                        let bgColor = 'bg-zinc-800 border-zinc-700 text-zinc-400';
                        if (isItemApplied) bgColor = 'bg-green-500/20 border-green-500/50 text-green-300';
                        else if (!isValid) bgColor = 'bg-red-500/20 border-red-500/50 text-red-300';
                        else if (isConflicting) bgColor = 'bg-red-500/10 border-red-500/30 text-red-500/50 grayscale opacity-50';
                        else if (isSelected) bgColor = 'bg-brand-orange text-zinc-950 border-brand-orange shadow-[0_0_10px_rgba(255,165,0,0.3)]';
                        else bgColor = 'bg-blue-500/20 border-blue-500/40 text-blue-300 cursor-pointer hover:bg-blue-500/40 hover:border-blue-400';

                        return (
                          <div
                            key={item.itemPublicId}
                            onClick={() => {
                              if (isValid && !isItemApplied && !isDisabled) {
                                handleToggleSelection(item.itemPublicId, isSelected);
                              }
                            }}
                            className={`absolute top-1 bottom-1 rounded border overflow-hidden flex flex-col justify-center px-1.5 transition-all ${bgColor} ${isDisabled && !isSelected && isValid ? 'cursor-not-allowed' : ''}`}
                            style={{ left, width }}
                            title={`${item.movieTitle}\n${formatTime(item.startTime)} - ${formatTime(item.endTime)}\n${!isValid ? 'Từ chối' : isConflicting ? 'Xung đột' : ''}`}
                          >
                            <span className="text-[9px] font-bold truncate leading-tight whitespace-nowrap">{item.movieTitle}</span>
                            <span className="text-[8px] opacity-80 truncate leading-tight">{formatTime(item.startTime)}-{formatTime(item.endTime)}</span>
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
      ))}
    </div>
  );
};

export default AutoScheduleTimeline;
