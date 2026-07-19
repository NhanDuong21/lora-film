// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useSearchParams, useOutletContext } from 'react-router-dom';
import { 
  ArrowLeft, 
  // eslint-disable-next-line no-unused-vars
  Settings, 
  Sliders, 
  Save, 
  AlertCircle,
  Sparkles
} from 'lucide-react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import apiClient from '@/services/apiClient';

// Import modular sub-components
import RoomForm from '@/features/facilities/admin/components/RoomForm';
import BrushToolbar from '@/features/facilities/admin/components/BrushToolbar';
import StatsPanel from '@/features/facilities/admin/components/StatsPanel';
import SeatGridDesigner from '@/features/facilities/admin/components/SeatGridDesigner';
import AutoLayoutWizardModal from '@/features/facilities/admin/components/AutoLayoutWizardModal';

export default function AdminRoomCreatePage() {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const cinemaId = searchParams.get('cinemaId');

  const [cinemaName, setCinemaName] = useState('');
  
  // Form fields
  const [roomName, setRoomName] = useState('');
  const [screenType, setScreenType] = useState('STANDARD');
  const [soundType, setSoundType] = useState('STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(15);
  const [status, setStatus] = useState('DRAFT');

  // Seating grid dimensions
  const [rows, setRows] = useState(10);
  const [cols, setCols] = useState(12);
  const [skipIO, setSkipIO] = useState(false);

  // Brush and seat matrix state
  const [activeBrush, setActiveBrush] = useState('STANDARD'); 
  const [matrix, setMatrix] = useState([]);
  const [isMouseDown, setIsMouseDown] = useState(false);

  // Auto-Layout Wizard State
  const [isWizardOpen, setIsWizardOpen] = useState(false);

  // Available seat types from DB
  const [dbSeatTypes, setDbSeatTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load initial cinema detail and seat types
  useEffect(() => {
    if (!cinemaId) {
      triggerToast?.('Lỗi: Thiếu Cinema ID để tạo phòng chiếu!', 'error');
      navigate('/admin/rooms');
      return;
    }

    const loadInitialData = async () => {
      try {
        const cinemaRes = await adminCinemaService.getAdminCinemaDetail(cinemaId);
        if (cinemaRes?.success && cinemaRes.data) {
          setCinemaName(cinemaRes.data.name);
        }

        const seatTypesRes = await adminRoomService.getSeatTypes();
        if (seatTypesRes?.success && Array.isArray(seatTypesRes.data)) {
          setDbSeatTypes(seatTypesRes.data);
        }
      } catch (err) {
        console.error('Failed to load initial data:', err);
        triggerToast?.('Không thể tải thông tin khởi tạo: ' + err.message, 'error');
      }
    };

    loadInitialData();
  }, [cinemaId, triggerToast, navigate]);

  // Generate initial seat matrix when rows/cols change
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMatrix(prev => {
      // Avoid resetting if dimensions match exactly (e.g. after wizard apply)
      if (prev.length === rows && prev[0]?.length === cols) return prev;
      
      const nextMatrix = [];
      for (let r = 0; r < rows; r++) {
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
  }, [rows, cols]);

  // Click & Drag painting handler
  const handleCellPaint = (r, c) => {
    setMatrix(prev => {
      const copy = prev.map(row => row.map(cell => ({ ...cell })));
      copy[r][c].type = activeBrush;
      return copy;
    });
  };

  const handleCellMouseDown = (r, c) => {
    setIsMouseDown(true);
    handleCellPaint(r, c);
  };

  const handleCellMouseEnter = (r, c) => {
    if (isMouseDown) {
      handleCellPaint(r, c);
    }
  };

  // Helper to globally release mouse down
  useEffect(() => {
    const handleMouseUp = () => setIsMouseDown(false);
    window.addEventListener('mouseup', handleMouseUp);
    return () => window.removeEventListener('mouseup', handleMouseUp);
  }, []);

  // Compute statistics based on the painted matrix
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

  // Check if standard seat types exist in DB, and create them if not.
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

  // Handle Wizard Apply
  const handleApplyWizard = (newMatrix, newRows, newCols) => {
    setRows(newRows);
    setCols(newCols);
    setMatrix(newMatrix);
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
      // 1. Ensure seat types exist in DB and obtain public IDs
      const seatTypes = await ensureSeatTypesExist();

      const typeMapping = {};
      ['STANDARD', 'VIP', 'COUPLE', 'DISABLED'].forEach(code => {
        const matched = seatTypes.find(t => t.code === code);
        if (matched) {
          typeMapping[code] = matched.publicId;
        }
      });

      // Check if any painted seat type is missing in DB mapping
      const paintedTypes = new Set();
      matrix.forEach(row => row.forEach(cell => {
        if (['STANDARD', 'VIP', 'COUPLE', 'DISABLED'].includes(cell.type)) {
          paintedTypes.add(cell.type);
        }
      }));

      for (const tCode of paintedTypes) {
        if (!typeMapping[tCode]) {
          throw new Error(`Loại ghế "${tCode}" chưa được khởi tạo trên Database.`);
        }
      }

      // 2. Create the Auditorium Room (status defaults to DRAFT)
      const roomPayload = {
        name: roomName.trim(),
        screenType,
        soundType,
        capacity: stats.activeSeats,
        cleaningBufferMinutes: parseInt(cleaningBuffer) || 15
      };

      const auditoriumRes = await adminRoomService.createAuditorium(cinemaId, roomPayload);
      if (!auditoriumRes?.success || !auditoriumRes.data) {
        throw new Error(auditoriumRes?.message || 'Không thể tạo phòng chiếu');
      }

      const roomPublicId = auditoriumRes.data.publicId;

      // 3. Format seat layout items
      const seatsList = [];

      const calculateRowLabel = (rIdx, skip) => {
        let letterCode = 65; // 'A'
        for (let i = 0; i < rIdx; i++) {
          letterCode++;
          if (skip && (letterCode === 73 || letterCode === 79)) {
            letterCode++;
          }
        }
        if (skip && (letterCode === 73 || letterCode === 79)) {
            letterCode++;
        }
        return String.fromCharCode(letterCode);
      };

      for (let r = 0; r < rows; r++) {
        const rowLabel = calculateRowLabel(r, skipIO);
        let seatNumber = 1;

        // Group couple seats in pairs: list all column indexes containing a COUPLE seat in this row
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

          // Compute pairGroup for couple seats
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

      // 4. Save seats via bulk endpoint
      const bulkRes = await adminRoomService.bulkCreateSeats(roomPublicId, { seats: seatsList });
      if (!bulkRes?.success) {
        throw new Error(bulkRes?.message || 'Không thể đồng bộ danh sách ghế');
      }

      // 5. If status chosen is ACTIVE/INACTIVE, update status
      if (status !== 'DRAFT') {
        await adminRoomService.updateAuditorium(roomPublicId, {
          name: roomName.trim(),
          screenType,
          soundType,
          capacity: stats.activeSeats,
          cleaningBufferMinutes: parseInt(cleaningBuffer) || 15,
          status
        });
      }

      triggerToast?.(`Tạo phòng chiếu "${roomName}" thành công với ${stats.activeSeats} ghế!`);
      navigate('/admin/rooms');
    } catch (err) {
      console.error('Failed to save room:', err);
      triggerToast?.('Lỗi: ' + (err.message || 'Không thể lưu sơ đồ phòng chiếu'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const availableStatuses = [
    { value: 'DRAFT', label: 'Bản nháp (DRAFT)' },
    { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
    { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' }
  ];

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
              TẠO PHÒNG CHIẾU MỚI
            </h2>
            <p className="text-[10px] text-zinc-450 font-bold uppercase tracking-wider mt-0.5">
              {cinemaName || 'Đang tải thông tin cụm rạp...'}
            </p>
          </div>
        </div>

        <button
          onClick={handleSave}
          disabled={isSubmitting}
          className="flex items-center gap-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-zinc-800 disabled:text-zinc-650 text-black text-xs font-black py-2 px-4 rounded-xl uppercase tracking-wider transition-all shadow-lg shadow-emerald-500/10"
        >
          <Save className="w-4 h-4" />
          <span>{isSubmitting ? 'Đang lưu...' : 'Lưu Phòng Chiếu'}</span>
        </button>
      </header>

      {/* Editor Body */}
      <div className="flex-1 flex overflow-hidden">
        
        {/* Left Form Settings Bar */}
        <aside className="w-80 bg-zinc-900 border-r border-zinc-800 p-5 flex flex-col justify-between overflow-y-auto shrink-0 select-none">
          <div className="space-y-6">
            
            {/* Quick Layout Trigger */}
            <button 
              onClick={() => setIsWizardOpen(true)}
              className="w-full flex items-center justify-center gap-2 bg-zinc-950 border border-brand-orange/40 hover:bg-brand-orange/10 text-brand-orange font-black py-3 px-4 rounded-xl text-xs uppercase tracking-wider transition-all"
            >
              <Sparkles className="w-4 h-4" />
              Sinh sơ đồ tự động
            </button>

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
              capacity={stats.activeSeats}
              isCreateMode={true}
              availableStatuses={availableStatuses}
            />

            {/* Grid Dimensions */}
            <div className="space-y-4">
              <div className="flex items-center gap-2 border-b border-zinc-800 pb-2">
                <Sliders className="w-4 h-4 text-brand-orange" />
                <h3 className="font-bold text-xs text-white uppercase tracking-wider">Kích thước lưới</h3>
              </div>

              {/* Rows Selector */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs font-bold text-zinc-400">
                  <span>Số hàng ghế (Rows)</span>
                  <span className="text-brand-orange font-black">{rows}</span>
                </div>
                <input 
                  type="range"
                  min="4"
                  max="20"
                  value={rows}
                  onChange={(e) => setRows(parseInt(e.target.value))}
                  className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-orange"
                />
              </div>

              {/* Columns Selector */}
              <div className="space-y-2">
                <div className="flex justify-between text-xs font-bold text-zinc-400">
                  <span>Số cột ghế (Cols)</span>
                  <span className="text-brand-orange font-black">{cols}</span>
                </div>
                <input 
                  type="range"
                  min="4"
                  max="20"
                  value={cols}
                  onChange={(e) => setCols(parseInt(e.target.value))}
                  className="w-full h-1 bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-brand-orange"
                />
              </div>

              {/* Skip I/O Selector */}
              <div className="pt-2">
                <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-zinc-400 hover:text-white transition-colors">
                  <input 
                    type="checkbox" 
                    checked={skipIO}
                    onChange={(e) => setSkipIO(e.target.checked)}
                    className="w-4 h-4 rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange/50 focus:ring-offset-zinc-950"
                  />
                  <span>Bỏ qua ký tự dễ gây nhầm lẫn (I, O)</span>
                </label>
              </div>
            </div>
          </div>

          {/* Stats Summary Panel */}
          <StatsPanel stats={stats} />
        </aside>

        {/* Right Seating Canvas Workspace */}
        <main className="flex-grow bg-zinc-950 p-6 md:p-8 flex flex-col items-center overflow-auto relative">
          
          {/* Seating Brush Selector Top Toolbar */}
          <BrushToolbar activeBrush={activeBrush} setActiveBrush={setActiveBrush} />

          {/* Seating Grid */}
          <SeatGridDesigner
            matrix={matrix}
            rows={rows}
            cols={cols}
            skipIO={skipIO}
            isLayoutEditable={true}
            onCellMouseDown={handleCellMouseDown}
            onCellMouseEnter={handleCellMouseEnter}
          />
          
          {/* Seating Map Guide */}
          <div className="flex items-center gap-2 mt-8 text-zinc-500 text-[10px] uppercase font-bold tracking-wider max-w-lg bg-zinc-900/20 border border-zinc-900 p-4 rounded-xl select-none">
            <AlertCircle className="w-4 h-4 text-brand-orange shrink-0" />
            <span>Mẹo: Nhấn chuột xuống và di (kéo rê chuột) qua lưới để vẽ hàng ghế/lối đi nhanh hơn. Chỉ có ghế ngồi (Thường, VIP, Đôi, Khuyết tật) được lưu vào cơ sở dữ liệu.</span>
          </div>

        </main>
        <AutoLayoutWizardModal 
          isOpen={isWizardOpen} 
          onClose={() => setIsWizardOpen(false)} 
          onApply={handleApplyWizard} 
          currentSkipIO={skipIO}
        />
      </div>
    </div>
  );
}
