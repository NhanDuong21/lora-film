import { useState, useEffect, useMemo } from 'react';
import { Sliders, Info, AlertCircle, Save } from 'lucide-react';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import BrushToolbar from '@/features/facilities/admin/components/BrushToolbar';
import StatsPanel from '@/features/facilities/admin/components/StatsPanel';
import SeatGridDesigner from '@/features/facilities/admin/components/SeatGridDesigner';
import { getAuditoriumStatus } from '@/features/facilities/admin/utils/facilityPresentation';
import { buildSeatItems } from '@/features/facilities/admin/utils/seatLayout';

export default function AuditoriumSeatLayoutTab({ auditorium, onUpdateBasicInfo, onUpdateSeats, triggerToast, futureShowtimeCount = 0 }) {
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
  const [layoutMetadata, setLayoutMetadata] = useState({ version: 1, appliedAt: null });

  const initialStatus = auditorium?.auditoriumStatus || 'DRAFT';
  const isLayoutEditable = initialStatus === 'DRAFT';
  const statusPresentation = getAuditoriumStatus(initialStatus);

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
      if (seats.length > 0) {
        maxRow = Math.max(...seats.map(s => s.positionRow || 1));
        maxCol = Math.max(...seats.map(s => s.positionColumn || 1));
      }

      // eslint-disable-next-line react-hooks/set-state-in-effect
      setRows(maxRow);
      setCols(maxCol);
      setSkipIO(true);

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

  useEffect(() => {
    if (!auditorium || isLayoutEditable) return undefined;
    let active = true;
    const loadCompleteStructure = async () => {
      try {
        const response = await adminRoomService.getClonePreview(auditorium.auditoriumPublicId);
        const preview = response?.success ? response.data : null;
        if (!active || !preview?.matrix?.length) return;
        const seats = (auditorium.rows || []).flatMap(row => row.seats || []);
        const seatByPosition = new Map(seats.map(seat => [
          `${seat.positionRow}:${seat.positionColumn}`,
          seat,
        ]));
        setRows(preview.rows);
        setCols(preview.columns);
        setSkipIO(true);
        setMatrix(preview.matrix.map((row, rowIndex) => row.map((type, columnIndex) => {
          const seat = seatByPosition.get(`${rowIndex + 1}:${columnIndex + 1}`);
          return {
            type: seat?.seatType?.code || type,
            seatPublicId: seat?.seatPublicId,
            status: seat?.status,
            pairGroup: seat?.pairGroup,
          };
        })));
        const appliedAt = seats
          .map(seat => seat.createdAt)
          .filter(Boolean)
          .sort()[0] || null;
        setLayoutMetadata({ version: preview.layoutVersion || 1, appliedAt });
      } catch {
        // Keep the seat-only fallback when structural preview cannot be reconstructed.
      }
    };
    void loadCompleteStructure();
    return () => { active = false; };
  }, [auditorium, isLayoutEditable]);

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
    const activeSeats = standard + vip + couple + disabled;
    const coupleModules = Math.floor(couple / 2);
    return {
      standard, vip, couple, disabled, exits, aisles, activeSeats,
      coupleModules,
      ticketingPositions: activeSeats - coupleModules,
    };
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

      const seatsList = buildSeatItems({ matrix, rows, cols, skipIO, typeMapping });

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
      triggerToast?.(`Lỗi: ${err.message || 'Không thể cập nhật sơ đồ ghế'}`, 'error');
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
          <div className="mb-4 flex w-full max-w-4xl flex-col gap-3">
            <div className="grid gap-2 rounded-2xl border border-zinc-800 bg-zinc-900/25 p-3 text-xs sm:grid-cols-3">
              <p><span className="text-zinc-500">Phiên bản sơ đồ:</span> <strong className="text-white">v{layoutMetadata.version}</strong></p>
              <p><span className="text-zinc-500">Đang áp dụng từ:</span> <strong className="text-white">{layoutMetadata.appliedAt ? new Intl.DateTimeFormat('vi-VN').format(new Date(layoutMetadata.appliedAt)) : 'Chưa có dữ liệu'}</strong></p>
              <p><span className="text-zinc-500">Sử dụng bởi:</span> <strong className="text-white">{futureShowtimeCount} suất chiếu tương lai</strong></p>
            </div>
            <div className="flex items-start gap-3 rounded-2xl border border-amber-500/25 bg-amber-500/[0.07] p-3 text-amber-300 select-none">
            <Info className="w-5 h-5 shrink-0 mt-0.5" />
            <div className="text-xs leading-5">
              <h4 className="font-extrabold uppercase">Sơ đồ đang ở chế độ chỉ xem</h4>
              <p className="text-zinc-300">Phòng đang {statusPresentation.label.toLocaleLowerCase('vi')} nên không thể sửa trực tiếp sơ đồ hiện tại. Thay đổi cấu trúc cần một phiên bản sơ đồ mới để không ảnh hưởng các suất đã có.</p>
            </div>
          </div>
          </div>
        )}

        <div className="flex-1 overflow-auto w-full pb-20">
          {!isLayoutEditable && (
            <div className="mx-auto mb-5 flex max-w-3xl flex-wrap justify-center gap-x-5 gap-y-2 text-[11px] font-bold text-zinc-400">
              {[
                ['bg-purple-500', 'Ghế thường'], ['bg-red-500', 'VIP'], ['bg-amber-400', 'Ghế đôi'],
                ['bg-sky-400', 'Vị trí tiếp cận'], ['border border-zinc-600 bg-transparent', 'Lối đi'],
                ['bg-emerald-500', 'Cửa'], ['border border-dashed border-zinc-700', 'Vùng trống'],
              ].map(([tone, label]) => <span key={label} className="inline-flex items-center gap-2"><i className={`h-2.5 w-2.5 rounded-sm ${tone}`} />{label}</span>)}
            </div>
          )}
          <div className="flex justify-center">
            <SeatGridDesigner
              matrix={matrix}
              rows={rows}
              cols={cols}
              skipIO={skipIO}
              isLayoutEditable={isLayoutEditable}
              onCellMouseDown={handleCellMouseDown}
              onCellMouseEnter={handleCellMouseEnter}
            />
          </div>
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
