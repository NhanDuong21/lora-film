// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useParams, useOutletContext } from 'react-router-dom';
import { 
  ArrowLeft, 
  Sliders, 
  Save, 
  AlertCircle,
  Info
} from 'lucide-react';
// eslint-disable-next-line no-unused-vars
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import apiClient from '@/services/apiClient';

// Import modular sub-components
import RoomForm from '@/features/facilities/admin/components/RoomForm';
import BrushToolbar from '@/features/facilities/admin/components/BrushToolbar';
import StatsPanel from '@/features/facilities/admin/components/StatsPanel';
import SeatGridDesigner from '@/features/facilities/admin/components/SeatGridDesigner';

export default function AdminRoomEditPage() {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const { roomId } = useParams();

   
  // eslint-disable-next-line no-unused-vars
  const [cinemaPublicId, setCinemaPublicId] = useState('');
  
  // Form fields
  const [roomName, setRoomName] = useState('');
  const [screenType, setScreenType] = useState('STANDARD');
  const [soundType, setSoundType] = useState('STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(15);
  const [status, setStatus] = useState('DRAFT');
  const [initialStatus, setInitialStatus] = useState('DRAFT');

  // Seating grid dimensions
  const [rows, setRows] = useState(10);
  const [cols, setCols] = useState(12);

  // Brush and seat matrix state
  const [activeBrush, setActiveBrush] = useState('STANDARD'); 
  const [matrix, setMatrix] = useState([]);
  const [isMouseDown, setIsMouseDown] = useState(false);

  // Available seat types from DB
  const [dbSeatTypes, setDbSeatTypes] = useState([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Read-only grid flag based on status (layout is editable only in DRAFT status in backend)
  const isLayoutEditable = initialStatus === 'DRAFT';

  // Load auditorium layout and seat types
  useEffect(() => {
    const loadData = async () => {
      setIsLoading(true);
      try {
        // 1. Fetch seat types
        const seatTypesRes = await adminRoomService.getSeatTypes();
        if (seatTypesRes?.success && Array.isArray(seatTypesRes.data)) {
          setDbSeatTypes(seatTypesRes.data);
        }

        // 2. Fetch auditorium layout
        const layoutRes = await adminRoomService.getAdminSeatLayout(roomId);
        if (layoutRes?.success && layoutRes.data) {
          const d = layoutRes.data;
          setRoomName(d.auditoriumName);
          setScreenType(d.screenType || 'STANDARD');
          setSoundType(d.soundType || 'STANDARD');
          setCleaningBuffer(d.cleaningBufferMinutes || 15);
          setStatus(d.auditoriumStatus || 'DRAFT');
          setInitialStatus(d.auditoriumStatus || 'DRAFT');

          // Parse seats and configure grid dimensions
          const seats = [];
          if (d.rows && Array.isArray(d.rows)) {
            d.rows.forEach(r => {
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

          setRows(maxRow);
          setCols(maxCol);

          // Populate grid matrix
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
      } catch (err) {
        console.error('Failed to load room details:', err);
        triggerToast?.('Không thể tải thông tin phòng chiếu: ' + err.message, 'error');
        navigate('/admin/rooms');
      } finally {
        setIsLoading(false);
      }
    };

    loadData();
  }, [roomId, triggerToast, navigate]);

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

  // Release mouse down
  useEffect(() => {
    const handleMouseUp = () => setIsMouseDown(false);
    window.addEventListener('mouseup', handleMouseUp);
    return () => window.removeEventListener('mouseup', handleMouseUp);
  }, []);

  // Compute statistics based on matrix
  const stats = useMemo(() => {
    let standard = 0;
    let vip = 0;
    let couple = 0;
    let disabled = 0;
    let exits = 0;
    let aisles = 0;

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

    return {
      standard,
      vip,
      couple,
      disabled,
      exits,
      aisles,
      activeSeats
    };
  }, [matrix]);

  // Seeding backup seat types helper
  const ensureSeatTypesExist = async () => {
    let currentTypes = [...dbSeatTypes];
    try {
      const seatTypesRes = await adminRoomService.getSeatTypes();
      if (seatTypesRes?.success && Array.isArray(seatTypesRes.data)) {
        currentTypes = seatTypesRes.data;
      }
    } catch (e) {
      console.warn("Failed to query seat types before seeding:", e);
    }

    const defaultTypes = [
      { code: 'STANDARD', name: 'Ghế Tiêu Chuẩn', description: 'Ghế ngồi tiêu chuẩn' },
      { code: 'VIP', name: 'Ghế VIP', description: 'Ghế ngồi cao cấp, vị trí đẹp' },
      { code: 'COUPLE', name: 'Ghế Đôi', description: 'Ghế đôi dành cho cặp đôi ở hàng cuối' },
      { code: 'DISABLED', name: 'Ghế Người Khuyết Tật', description: 'Vị trí dành riêng cho xe lăn' }
    ];

    const seededTypes = [];

    for (const defType of defaultTypes) {
      const existing = currentTypes.find(t => t.code === defType.code);
      if (existing) {
        seededTypes.push(existing);
      } else {
        try {
          const createRes = await apiClient.post('/api/admin/seat-types', defType);
          if (createRes.data?.data) {
            seededTypes.push(createRes.data.data);
          }
        } catch (err) {
          console.error(`Failed to seed seat type ${defType.code}:`, err);
        }
      }
    }

    return seededTypes.length > 0 ? seededTypes : currentTypes;
  };

  // Submit Handler
  const handleSave = async () => {
    if (!roomName.trim()) {
      triggerToast?.('Vui lòng nhập tên phòng chiếu!', 'error');
      return;
    }

    if (stats.activeSeats === 0) {
      triggerToast?.('Sơ đồ phòng chiếu phải có ít nhất 1 ghế!', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      // 1. Ensure seat types exist
      const seatTypes = await ensureSeatTypesExist();
      const typeMapping = {};
      ['STANDARD', 'VIP', 'COUPLE', 'DISABLED'].forEach(code => {
        const matched = seatTypes.find(t => t.code === code);
        if (matched) {
          typeMapping[code] = matched.publicId;
        }
      });

      // 2. If layout was editable (initialStatus was DRAFT), sync seats using bulk endpoint first (while status is still DRAFT)
      if (isLayoutEditable) {
        const seatsList = [];

        for (let r = 0; r < rows; r++) {
          const rowLabel = String.fromCharCode(65 + r);
          let seatNumber = 1;

          // Group couple seats
          const coupleCols = [];
          for (let c = 0; c < cols; c++) {
            if (matrix[r][c].type === 'COUPLE') {
              coupleCols.push(c);
            }
          }

          for (let c = 0; c < cols; c++) {
            const cell = matrix[r][c];
            if (cell.type === 'AISLE' || cell.type === 'EXIT') continue;

            const seatTypeCode = cell.type;
            const seatTypePublicId = typeMapping[seatTypeCode];

            let pairGroup = null;
            if (seatTypeCode === 'COUPLE') {
              const indexInCoupleCols = coupleCols.indexOf(c);
              if (indexInCoupleCols !== -1) {
                const pairIndex = Math.floor(indexInCoupleCols / 2) + 1;
                pairGroup = `${rowLabel}_P${pairIndex}`;
              }
            }

            const seatCode = `${rowLabel}${seatNumber}`;

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

        const bulkRes = await adminRoomService.bulkCreateSeats(roomId, { seats: seatsList });
        if (!bulkRes?.success) {
          throw new Error(bulkRes?.message || 'Không thể cập nhật danh sách ghế');
        }
      }

      // 3. Call PUT /api/admin/auditoriums/{roomId} to update auditorium general details (and transition status)
      const updatePayload = {
        name: roomName.trim(),
        screenType,
        soundType,
        capacity: stats.activeSeats,
        cleaningBufferMinutes: parseInt(cleaningBuffer) || 15,
        status: status 
      };

      const updateRes = await adminRoomService.updateAuditorium(roomId, updatePayload);
      if (!updateRes?.success) {
        throw new Error(updateRes?.message || 'Không thể cập nhật phòng chiếu');
      }

      triggerToast?.(`Cập nhật thông tin phòng chiếu "${roomName}" thành công!`);
      navigate('/admin/rooms');
    } catch (err) {
      console.error('Failed to update room:', err);
      triggerToast?.('Lỗi: ' + (err.message || 'Không thể cập nhật phòng chiếu'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Non-destructive grid resizing
  const handleRowsChange = (newRows) => {
    if (!isLayoutEditable) return;
    setRows(newRows);
    setMatrix(prev => {
      const nextMatrix = [];
      for (let r = 0; r < newRows; r++) {
        const row = [];
        for (let c = 0; c < cols; c++) {
          const oldCell = prev[r]?.[c];
          if (oldCell) {
            row.push(oldCell);
          } else {
            row.push({ type: 'STANDARD' });
          }
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
          const oldCell = prev[r]?.[c];
          if (oldCell) {
            row.push(oldCell);
          } else {
            row.push({ type: 'STANDARD' });
          }
        }
        nextMatrix.push(row);
      }
      return nextMatrix;
    });
  };

  // Status transitions list depending on current status
  const availableStatuses = useMemo(() => {
    switch (initialStatus) {
      case 'DRAFT':
        return [
          { value: 'DRAFT', label: 'Bản nháp (DRAFT)' },
          { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
          { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' }
        ];
      case 'ACTIVE':
        return [
          { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
          { value: 'MAINTENANCE', label: 'Bảo trì (MAINTENANCE)' },
          { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' }
        ];
      case 'MAINTENANCE':
        return [
          { value: 'MAINTENANCE', label: 'Bảo trì (MAINTENANCE)' },
          { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
          { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' }
        ];
      case 'INACTIVE':
        return [
          { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' },
          { value: 'DRAFT', label: 'Bản nháp (DRAFT)' },
          { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' }
        ];
      default:
        return [{ value: initialStatus, label: initialStatus }];
    }
  }, [initialStatus]);

  if (isLoading) {
    return (
      <div className="flex-1 h-screen flex flex-col items-center justify-center bg-zinc-950 text-white gap-4 select-none">
        <div className="w-12 h-12 border-4 border-brand-coral border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-semibold tracking-wider text-zinc-550 uppercase">Đang tải thông tin phòng chiếu...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1 h-screen overflow-hidden bg-zinc-950 font-sans text-white">
      
      {/* Top Header */}
      <header className="h-16 bg-zinc-900 border-b border-zinc-800 px-6 flex justify-between items-center select-none shrink-0">
        <div className="flex items-center gap-3">
          <button 
            onClick={() => navigate('/admin/rooms')}
            className="p-2 hover:bg-zinc-800 text-zinc-400 hover:text-white rounded-xl transition-all"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">
              CẬP NHẬT PHÒNG CHIẾU
            </h2>
            <p className="text-[10px] text-zinc-450 font-bold uppercase tracking-wider mt-0.5 font-mono">
              Mã ID: {roomId}
            </p>
          </div>
        </div>

        <button
          onClick={handleSave}
          disabled={isSubmitting}
          className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-zinc-800 disabled:text-zinc-650 text-black text-xs font-black py-2 px-4 rounded-xl uppercase tracking-wider transition-all shadow-lg shadow-emerald-500/10"
        >
          <Save className="w-4 h-4" />
          <span>{isSubmitting ? 'Đang cập nhật...' : 'Cập nhật phòng'}</span>
        </button>
      </header>

      {/* Editor Body */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Left Form Settings Bar */}
        <aside className="w-80 bg-zinc-900 border-r border-zinc-800 p-5 flex flex-col justify-between overflow-y-auto shrink-0 select-none">
          <div className="space-y-6">
            
            {/* General Info Form */}
            <RoomForm
              roomName={roomName}
              setRoomName={setRoomName}
              screenType={screenType}
              setScreenType={setScreenType}
              soundType={soundType}
              setSoundType={setSoundType}
              cleaningBuffer={cleaningBuffer}
              setCleaningBuffer={setCleaningBuffer}
              status={status}
              setStatus={setStatus}
              availableStatuses={availableStatuses}
            />

            {/* Grid Dimensions (Only if Layout is Editable) */}
            {isLayoutEditable && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
                  <Sliders className="w-4 h-4 text-brand-coral" />
                  <h3 className="font-bold text-xs text-white uppercase tracking-wider">Kích thước lưới</h3>
                </div>

                {/* Rows Slider */}
                <div className="space-y-2">
                  <div className="flex justify-between text-xs font-bold text-zinc-400">
                    <span>Số hàng ghế (Rows)</span>
                    <span className="text-brand-coral font-black">{rows}</span>
                  </div>
                  <input 
                    type="range"
                    min="4"
                    max="20"
                    value={rows}
                    onChange={(e) => handleRowsChange(parseInt(e.target.value))}
                    className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-coral"
                  />
                </div>

                {/* Columns Slider */}
                <div className="space-y-2">
                  <div className="flex justify-between text-xs font-bold text-zinc-400">
                    <span>Số cột ghế (Cols)</span>
                    <span className="text-brand-coral font-black">{cols}</span>
                  </div>
                  <input 
                    type="range"
                    min="4"
                    max="20"
                    value={cols}
                    onChange={(e) => handleColsChange(parseInt(e.target.value))}
                    className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-coral"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Stats Summary Panel */}
          <StatsPanel stats={stats} />
        </aside>

        {/* Right Seating Canvas Workspace */}
        <main className="flex-grow bg-zinc-950 p-6 md:p-8 flex flex-col items-center overflow-auto relative">
          
          {/* Seating Brush Toolbar (Only shown if Editable) */}
          {isLayoutEditable ? (
            <BrushToolbar activeBrush={activeBrush} setActiveBrush={setActiveBrush} />
          ) : (
            <div className="bg-amber-500/10 border border-amber-500/30 text-amber-400 p-4 rounded-2xl flex items-start gap-3 mb-8 max-w-2xl shadow-xl shadow-black/20 select-none">
              <Info className="w-5 h-5 shrink-0 mt-0.5" />
              <div className="text-xs space-y-1">
                <h4 className="font-extrabold uppercase">Sơ đồ ghế ở chế độ chỉ xem (Read-Only)</h4>
                <p className="font-semibold text-zinc-300">
                  Cơ sở dữ liệu chỉ cho phép thay đổi sơ đồ phòng chiếu khi phòng đang ở trạng thái **DRAFT (Bản nháp)**. 
                  Hiện tại phòng này đang ở trạng thái `{initialStatus}`.
                </p>
                <p className="font-semibold text-zinc-455 mt-1">
                  Mẹo: Để thiết kế lại ghế, hãy chọn trạng thái **INACTIVE** ở thanh bên trái, nhấn **Cập nhật phòng** để lưu, 
                  sau đó chuyển trạng thái về **DRAFT** và mở lại trang này để vẽ.
                </p>
              </div>
            </div>
          )}

          {/* Seating Grid */}
          <SeatGridDesigner
            matrix={matrix}
            rows={rows}
            cols={cols}
            isLayoutEditable={isLayoutEditable}
            onCellMouseDown={handleCellMouseDown}
            onCellMouseEnter={handleCellMouseEnter}
          />
          
          {/* Seating Guide */}
          {isLayoutEditable && (
            <div className="flex items-center gap-2 mt-8 text-zinc-500 text-[10px] uppercase font-bold tracking-wider max-w-lg bg-zinc-900/20 border border-zinc-900 p-4 rounded-xl select-none">
              <AlertCircle className="w-4 h-4 text-brand-coral shrink-0" />
              <span>Mẹo: Nhấn chuột xuống và di (kéo rê chuột) qua lưới để vẽ hàng ghế/lối đi nhanh hơn. Chỉ có ghế ngồi (Thường, VIP, Đôi, Khuyết tật) được lưu vào cơ sở dữ liệu.</span>
            </div>
          )}

        </main>
      </div>
    </div>
  );
}
