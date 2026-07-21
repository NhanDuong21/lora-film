import { useState, useEffect, useMemo } from 'react';
import { Sliders, Info, AlertCircle, Save } from 'lucide-react';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import BrushToolbar from '@/features/facilities/admin/components/BrushToolbar';
import StatsPanel from '@/features/facilities/admin/components/StatsPanel';
import SeatGridDesigner from '@/features/facilities/admin/components/SeatGridDesigner';

export default function AuditoriumSeatLayoutTab({ auditorium, onUpdateBasicInfo, onUpdateSeats, triggerToast }) {
  // Seating grid dimensions
  const [rows, setRows] = useState(10);
  const [cols, setCols] = useState(12);

  // Layout features
  const [skipIO, setSkipIO] = useState(false);

  // Brush and seat matrix state
  const [activeBrush, setActiveBrush] = useState('STANDARD'); 
  const [matrix, setMatrix] = useState([]);
  const [isMouseDown, setIsMouseDown] = useState(false);

  // Available seat types from DB
  const [dbSeatTypes, setDbSeatTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const initialStatus = auditorium?.auditoriumStatus || 'DRAFT';
  const isLayoutEditable = initialStatus === 'DRAFT';

  useEffect(() => {
    const loadSeatTypes = async () => {
      try {
        const seatTypesRes = await adminRoomService.getSeatTypes();
        if (seatTypesRes?.success && Array.isArray(seatTypesRes.data)) {
          setDbSeatTypes(seatTypesRes.data);
        }
      } catch (err) {
        console.error('Failed to load seat types:', err);
      }
    };
    loadSeatTypes();
  }, []);

  useEffect(() => {
    if (auditorium) {
      const seats = [];
      if (auditorium.rows && Array.isArray(auditorium.rows)) {
        auditorium.rows.forEach(r => {
          if (r.seats && Array.isArray(r.seats)) {
            seats.push(...r.seats);
          }
        });
      }

      let maxRow = 10;
      let maxCol = 12;
      let isSkippingIO = false;
      if (seats.length > 0) {
        maxRow = Math.max(...seats.map(s => s.positionRow || 1));
        maxCol = Math.max(...seats.map(s => s.positionColumn || 1));
        
        // Check if any seat code contains 'I' or 'O' at the start
        const hasIORow = seats.some(s => s.rowLabel === 'I' || s.rowLabel === 'O');
        isSkippingIO = maxRow >= 9 && !hasIORow; // if we reached row 9 (I) and it's missing, then skipIO is true
      }

      // eslint-disable-next-line react-hooks/set-state-in-effect
      setRows(maxRow);
      setCols(maxCol);
      setSkipIO(isSkippingIO);

      const initialMatrix = [];
      for (let r = 0; r < maxRow; r++) {
        const row = [];
        for (let c = 0; c < maxCol; c++) {
          const matchedSeat = seats.find(s => s.positionRow === r + 1 && s.positionColumn === c + 1);
          if (matchedSeat) {
            row.push({
              type: matchedSeat.seatType?.code || 'STANDARD',
              seatPublicId: matchedSeat.seatPublicId,
              status: matchedSeat.status
            });
          } else {
            row.push({ type: 'AISLE' });
          }
        }
        initialMatrix.push(row);
      }
       
      setMatrix(initialMatrix);
    }
  }, [auditorium]);

  // Click & Drag painting handler (only if editable)
  const handleCellPaint = (r, c) => {
    if (!isLayoutEditable) return;
    setMatrix(prev => {
      const copy = prev.map(row => row.map(cell => ({ ...cell })));
      copy[r][c].type = activeBrush;
      return copy;
    });
  };

  const handleCellMouseDown = (r, c) => {
    if (!isLayoutEditable) return;
    setIsMouseDown(true);
    handleCellPaint(r, c);
  };

  const handleCellMouseEnter = (r, c) => {
    if (!isLayoutEditable && isMouseDown) {
      setIsMouseDown(false);
      return;
    }
    if (isMouseDown) {
      handleCellPaint(r, c);
    }
  };

  useEffect(() => {
    const handleMouseUp = () => setIsMouseDown(false);
    window.addEventListener('mouseup', handleMouseUp);
    return () => window.removeEventListener('mouseup', handleMouseUp);
  }, []);

  const stats = useMemo(() => {
    let standard = 0, vip = 0, couple = 0, disabled = 0, exits = 0, aisles = 0;
    matrix.forEach(row => {
      row.forEach(cell => {
        if (cell.type === 'STANDARD') standard++;
        else if (cell.type === 'VIP') vip++;
        else if (cell.type === 'COUPLE') couple++;
        else if (cell.type === 'DISABLED') disabled++;
        else if (cell.type === 'EXIT') exits++;
        else if (cell.type === 'AISLE') aisles++;
      });
    });
    return { standard, vip, couple, disabled, exits, aisles, activeSeats: standard + vip + couple + disabled };
  }, [matrix]);

  const handleSaveLayout = async () => {
    if (!isLayoutEditable) return;
    if (stats.activeSeats === 0) {
      triggerToast?.('Sơ đồ phòng chiếu phải có ít nhất 1 ghế!', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      // Map seat types
      const typeMapping = {};
      ['STANDARD', 'VIP', 'COUPLE', 'DISABLED'].forEach(code => {
        const matched = dbSeatTypes.find(t => t.code === code);
        if (matched) typeMapping[code] = matched.publicId;
      });

      const calculateRowLabel = (rIdx, skip) => {
        let letterCode = 65; // 'A'
        for (let i = 0; i < rIdx; i++) {
          letterCode++;
          if (skip && (letterCode === 73 || letterCode === 79)) { // 'I' is 73, 'O' is 79
            letterCode++;
          }
        }
        if (skip && (letterCode === 73 || letterCode === 79)) {
            letterCode++;
        }
        return String.fromCharCode(letterCode);
      };

      const seatsList = [];
      for (let r = 0; r < rows; r++) {
        const rowLabel = calculateRowLabel(r, skipIO);
        let seatNumber = 1;

        const coupleCols = [];
        for (let c = 0; c < cols; c++) {
          if (matrix[r][c].type === 'COUPLE') coupleCols.push(c);
        }

        for (let c = 0; c < cols; c++) {
          const cell = matrix[r][c];
          if (cell.type === 'AISLE' || cell.type === 'EXIT') continue;

          const seatTypeCode = cell.type;
          const seatTypePublicId = typeMapping[seatTypeCode];
          
          if (!seatTypePublicId) {
            console.warn(`Missing seatType mapping for ${seatTypeCode}`);
          }

          const seatCode = `${rowLabel}${seatNumber}`;

          let pairGroup = null;
          if (seatTypeCode === 'COUPLE') {
            const indexInCoupleCols = coupleCols.indexOf(c);
            if (indexInCoupleCols !== -1) {
              const isEvenIndex = indexInCoupleCols % 2 === 0;
              const partnerIndex = isEvenIndex ? indexInCoupleCols + 1 : indexInCoupleCols - 1;
              const hasPartner = partnerIndex >= 0 && partnerIndex < coupleCols.length 
                                 && Math.abs(coupleCols[indexInCoupleCols] - coupleCols[partnerIndex]) === 1;
              
              if (hasPartner) {
                if (isEvenIndex) {
                  pairGroup = `group_${seatCode}_${rowLabel}${seatNumber + 1}`;
                } else {
                  pairGroup = `group_${rowLabel}${seatNumber - 1}_${seatCode}`;
                }
              }
            }
          }

          seatsList.push({
            seatTypePublicId,
            rowLabel,
            seatNumber,
            seatCode,
            positionRow: r + 1,
            positionColumn: c + 1,
            pairGroup,
            status: 'ACTIVE'
          });
          seatNumber++;
        }
      }

      // Update seats
      const seatsSuccess = await onUpdateSeats(seatsList);
      if (seatsSuccess) {
        // Also update room capacity since activeSeats might have changed
        await onUpdateBasicInfo({
          name: auditorium.auditoriumName,
          screenType: auditorium.screenType,
          soundType: auditorium.soundType,
          capacity: stats.activeSeats,
          cleaningBufferMinutes: auditorium.cleaningBufferMinutes,
          status: auditorium.auditoriumStatus
        });
        triggerToast?.('Lưu sơ đồ ghế thành công!');
      }
    } catch (err) {
      triggerToast?.('Lỗi: Không thể cập nhật sơ đồ ghế', 'error');
      console.error(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRowsChange = (newRows) => {
    if (!isLayoutEditable) return;
    setRows(newRows);
    setMatrix(prev => {
      const nextMatrix = [];
      for (let r = 0; r < newRows; r++) {
        const row = [];
        for (let c = 0; c < cols; c++) {
          row.push(prev[r]?.[c] || { type: 'STANDARD' });
        }
        nextMatrix.push(row);
      }
      return nextMatrix;
    });
  };

  const handleColsChange = (newCols) => {
    if (!isLayoutEditable) return;
    setCols(newCols);
    setMatrix(prev => {
      const nextMatrix = [];
      for (let r = 0; r < rows; r++) {
        const row = [];
        for (let c = 0; c < newCols; c++) {
          row.push(prev[r]?.[c] || { type: 'STANDARD' });
        }
        nextMatrix.push(row);
      }
      return nextMatrix;
    });
  };

  return (
    <div className="flex h-full pb-8">
      {/* Sidebar for settings and stats */}
      <div className="w-80 border-r border-zinc-900 pr-6 flex flex-col gap-6 shrink-0 overflow-y-auto">
        {isLayoutEditable && (
          <div className="space-y-4 bg-zinc-900/30 border border-zinc-800 p-5 rounded-2xl">
            <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
              <Sliders className="w-4 h-4 text-brand-orange" />
              <h3 className="font-bold text-xs text-white uppercase tracking-wider">Kích thước lưới</h3>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-xs font-bold text-zinc-400">
                <span>Số hàng ghế (Rows)</span>
                <span className="text-brand-orange font-black">{rows}</span>
              </div>
              <input 
                type="range" min="4" max="20" value={rows}
                onChange={(e) => handleRowsChange(parseInt(e.target.value))}
                className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-orange"
              />
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-xs font-bold text-zinc-400">
                <span>Số cột ghế (Cols)</span>
                <span className="text-brand-orange font-black">{cols}</span>
              </div>
              <input 
                type="range" min="4" max="20" value={cols}
                onChange={(e) => handleColsChange(parseInt(e.target.value))}
                className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-orange"
              />
            </div>
            
            <div className="pt-2 border-t border-zinc-800/50 mt-4 flex items-center justify-between">
              <span className="text-xs font-bold text-zinc-400">Bỏ qua hàng I, O</span>
              <label className="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  className="sr-only peer" 
                  checked={skipIO}
                  onChange={(e) => setSkipIO(e.target.checked)}
                />
                <div className="w-9 h-5 bg-zinc-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-brand-orange"></div>
              </label>
            </div>
          </div>
        )}
        <StatsPanel stats={stats} />
      </div>

      {/* Main Designer Area */}
      <div className="flex-1 pl-6 flex flex-col overflow-hidden items-center relative">
        <div className="w-full flex justify-end mb-6">
          {isLayoutEditable && (
            <button
              onClick={handleSaveLayout}
              disabled={isSubmitting}
              className="flex items-center gap-2 bg-brand-orange hover:bg-opacity-90 text-white px-6 py-2.5 rounded-xl font-bold uppercase tracking-wider text-xs transition-colors shadow-lg shadow-brand-orange/20 disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {isSubmitting ? 'ĐANG LƯU...' : 'LƯU SƠ ĐỒ GHẾ'}
            </button>
          )}
        </div>

        {isLayoutEditable ? (
          <BrushToolbar activeBrush={activeBrush} setActiveBrush={setActiveBrush} />
        ) : (
          <div className="bg-amber-500/10 border border-amber-500/30 text-amber-400 p-4 rounded-2xl flex items-start gap-3 mb-8 max-w-2xl shadow-xl shadow-black/20 select-none">
            <Info className="w-5 h-5 shrink-0 mt-0.5" />
            <div className="text-xs space-y-1">
              <h4 className="font-extrabold uppercase">Sơ đồ ghế ở chế độ chỉ xem (Read-Only)</h4>
              <p className="font-semibold text-zinc-300">
                Chỉ cho phép thay đổi sơ đồ phòng chiếu khi phòng đang ở trạng thái <strong>DRAFT (Bản nháp)</strong>. 
                Hiện tại phòng này đang ở trạng thái <strong>{initialStatus}</strong>.
              </p>
              <p className="font-semibold text-zinc-450 mt-1">
                Mẹo: Chuyển trạng thái sang INACTIVE, lưu lại, sau đó chuyển về DRAFT ở tab CẤU HÌNH để có thể sửa sơ đồ.
              </p>
            </div>
          </div>
        )}

        <div className="flex-1 overflow-auto w-full flex justify-center pb-20">
          <SeatGridDesigner
            matrix={matrix}
            rows={rows}
            cols={cols}
            skipIO={skipIO}
            isLayoutEditable={isLayoutEditable}
            skipIO={skipIO}
            onCellMouseDown={handleCellMouseDown}
            onCellMouseEnter={handleCellMouseEnter}
          />
        </div>

        {isLayoutEditable && (
          <div className="absolute bottom-10 flex items-center gap-2 text-zinc-500 text-[10px] uppercase font-bold tracking-wider max-w-lg bg-zinc-900/40 backdrop-blur-md border border-zinc-900 p-4 rounded-xl select-none">
            <AlertCircle className="w-4 h-4 text-brand-orange shrink-0" />
            <span>Mẹo: Nhấn chuột xuống và di qua lưới để vẽ hàng ghế/lối đi nhanh hơn.</span>
          </div>
        )}
      </div>
    </div>
  );
}
