// eslint-disable-next-line no-unused-vars
import React from 'react';

export default function SeatGridDesigner({
  matrix,
  // eslint-disable-next-line no-unused-vars
  rows,
  cols,
  isLayoutEditable = true,
  skipIO = false,
  onCellMouseDown,
  onCellMouseEnter
}) {
  const getRowLabel = (index, skip) => {
    let current = 0;
    let label = '';
    while (current <= index) {
      label = String.fromCharCode(65 + (current % 26)) + label;
      if (skip && (label === 'I' || label === 'O')) {
        index++; // push the index further to skip this letter
      }
      current++;
    }
    // Actually this loop logic for multi-character is complex. Since max rows is 20, we can just map it directly.
    return '';
  };

  const calculateRowLabel = (rIdx, skip) => {
    let letterCode = 65; // 'A'
    for (let i = 0; i < rIdx; i++) {
      letterCode++;
      if (skip && (letterCode === 73 || letterCode === 79)) { // 'I' is 73, 'O' is 79
        letterCode++;
      }
    }
    // Handle skipping if the first letter itself is skipped when rIdx = 0
    if (skip && (letterCode === 73 || letterCode === 79)) {
        letterCode++;
    }
    return String.fromCharCode(letterCode);
  };
  return (
    <div className="flex flex-col items-center select-none">
      
      {/* Projector Screen Visual Indicator */}
      <div className="w-full max-w-lg mx-auto mb-16 text-center">
        <div className="h-1.5 bg-gradient-to-r from-transparent via-[#ff7a1a] to-transparent shadow-[0_0_20px_rgba(255,122,26,0.9)] rounded-full mb-3"></div>
        <span className="text-zinc-500 text-[10px] tracking-[0.44em] font-black uppercase">MÀN HÌNH CHIẾU / PROJECTOR SCREEN</span>
      </div>

      {/* Visual Interactive Seating Grid Box */}
      <div className="bg-zinc-900/30 border border-zinc-900/80 p-8 rounded-3xl max-w-full shadow-2xl relative">
        
        {/* Container cho lưới ghế để có thể căn giữa */}
        <div className="flex flex-col items-center overflow-x-auto pb-4">
          {/* Column Headers Numerical Indexes */}
          <div className="flex mb-3 w-max">
            <div className="w-8 shrink-0"></div>
            <div 
              className="grid gap-2 text-center text-[10px] font-black text-zinc-500"
              style={{ 
                gridTemplateColumns: `repeat(${cols}, minmax(36px, 1fr))`,
                width: `${cols * 44}px`
              }}
            >
              {Array.from({ length: cols }).map((_, idx) => (
                <div key={idx} className="w-9">{idx + 1}</div>
              ))}
            </div>
          </div>

          {/* Rows & Cells */}
          <div className="space-y-2 w-max">
            {matrix.map((row, rIdx) => {
              const rowLetter = calculateRowLabel(rIdx, skipIO);
              let seatNumber = 1;
            return (
              <div key={rIdx} className="flex items-center">
                
                {/* Row letter left anchor */}
                <div className="w-8 text-[11px] font-black text-zinc-400 uppercase">
                  {rowLetter}
                </div>

                {/* Interactive seat buttons row container */}
                <div 
                  className="grid gap-2"
                  style={{ 
                    gridTemplateColumns: `repeat(${cols}, minmax(36px, 1fr))`,
                    width: `${cols * 44}px`
                  }}
                >
                  {row.map((cell, cIdx) => {
                    let cellBg = '';
                    let labelColor = 'text-white/60';
                    let content = '';
                    
                    let currentSeatNumber = seatNumber;
                    if (cell.type !== 'AISLE' && cell.type !== 'EXIT') {
                        seatNumber++;
                    }

                    if (cell.type === 'STANDARD') {
                      cellBg = 'bg-purple-600/10 border border-purple-500/40 hover:bg-purple-600/30 text-purple-400';
                      content = `${rowLetter}${currentSeatNumber}`;
                    } else if (cell.type === 'VIP') {
                      cellBg = 'bg-red-500/10 border border-red-500/40 hover:bg-red-500/30 text-red-400';
                      content = `${rowLetter}${currentSeatNumber}`;
                    } else if (cell.type === 'COUPLE') {
                      cellBg = 'bg-amber-400/10 border border-amber-400/40 hover:bg-amber-400/30 text-amber-400 font-extrabold';
                      content = `${rowLetter}${currentSeatNumber}`;
                    } else if (cell.type === 'DISABLED') {
                      cellBg = 'bg-sky-500/10 border border-sky-500/40 hover:bg-sky-500/30 text-sky-400 font-extrabold';
                      content = `♿`;
                    } else if (cell.type === 'EXIT') {
                      cellBg = 'bg-emerald-950 border border-emerald-500/50 hover:bg-emerald-900/60 text-emerald-400 flex flex-col justify-center items-center shadow-lg shadow-emerald-950/40';
                      content = '🚪';
                    } else {
                      // AISLE/WALKWAY
                      cellBg = 'bg-zinc-950 border border-dashed border-zinc-900 text-transparent hover:bg-zinc-900/40 hover:border-zinc-800';
                    }

                    const isDisabled = !isLayoutEditable;

                    return (
                      <button
                        key={cIdx}
                        disabled={isDisabled}
                        onMouseDown={() => onCellMouseDown?.(rIdx, cIdx)}
                        onMouseEnter={() => onCellMouseEnter?.(rIdx, cIdx)}
                        className={`w-9 h-9 rounded-lg flex items-center justify-center text-[9px] font-black uppercase tracking-tighter transition-all select-none ${cellBg} ${isDisabled ? 'cursor-default opacity-85' : 'cursor-pointer'}`}
                        title={`Hàng ${rowLetter} - Ghế ${cell.type !== 'AISLE' && cell.type !== 'EXIT' ? currentSeatNumber : (cIdx + 1)} (${cell.type})`}
                      >
                        <span className={labelColor}>
                          {content}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
          </div>
        </div>
      </div>
    </div>
  );
}
