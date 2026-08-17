// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams, useOutletContext } from 'react-router-dom';
import { 
  ArrowLeft, 
  // eslint-disable-next-line no-unused-vars
  Settings, 
  Sliders, 
  Save, 
  AlertCircle,
  Sparkles,
  Undo2,
  Redo2,
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
import { buildSeatItems } from '@/features/facilities/admin/utils/seatLayout';

export default function AdminRoomCreatePage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const cinemaId = searchParams.get('cinemaId');

  const [cinemaName, setCinemaName] = useState('');
  
  // Form fields
  const [roomName, setRoomName] = useState('');
  const [screenType, setScreenType] = useState('STANDARD');
  const [soundType, setSoundType] = useState('STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(15);
  const [approvedCapacity, setApprovedCapacity] = useState(120);

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
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [historyState, setHistoryState] = useState({ undoCount: 0, redoCount: 0 });
  const undoStackRef = useRef([]);
  const redoStackRef = useRef([]);

  const cloneMatrix = source => source.map(row => row.map(cell => ({ ...cell })));

  const createLayoutSnapshot = (
    source = matrix,
    snapshotRows = rows,
    snapshotCols = cols,
    snapshotSkipIO = skipIO,
  ) => ({
    matrix: cloneMatrix(source),
    rows: snapshotRows,
    cols: snapshotCols,
    skipIO: snapshotSkipIO,
  });

  const restoreLayoutSnapshot = snapshot => {
    setRows(snapshot.rows);
    setCols(snapshot.cols);
    setSkipIO(snapshot.skipIO);
    setMatrix(cloneMatrix(snapshot.matrix));
  };

  const pushHistory = source => {
    undoStackRef.current = [
      ...undoStackRef.current.slice(-49),
      createLayoutSnapshot(source),
    ];
    redoStackRef.current = [];
    setHistoryState({ undoCount: undoStackRef.current.length, redoCount: 0 });
    setHasUnsavedChanges(true);
  };

  const undo = () => {
    const previous = undoStackRef.current.pop();
    if (!previous) return;
    redoStackRef.current.push(createLayoutSnapshot());
    restoreLayoutSnapshot(previous);
    setHistoryState({
      undoCount: undoStackRef.current.length,
      redoCount: redoStackRef.current.length,
    });
    setHasUnsavedChanges(true);
  };

  const redo = () => {
    const next = redoStackRef.current.pop();
    if (!next) return;
    undoStackRef.current.push(createLayoutSnapshot());
    restoreLayoutSnapshot(next);
    setHistoryState({
      undoCount: undoStackRef.current.length,
      redoCount: redoStackRef.current.length,
    });
    setHasUnsavedChanges(true);
  };

  useEffect(() => {
    const warnBeforeUnload = event => {
      if (!hasUnsavedChanges) return;
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => window.removeEventListener('beforeunload', warnBeforeUnload);
  }, [hasUnsavedChanges]);

  // Load initial cinema detail and seat types
  useEffect(() => {
    if (!cinemaId) {
      triggerToast?.('Không xác định được cụm rạp cần thêm phòng chiếu.', 'error');
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
    pushHistory(matrix);
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
    pushHistory(matrix);
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
    if (approvedCapacity < stats.activeSeats) {
      triggerToast?.('Số vị trí trong sơ đồ không được vượt sức chứa theo hồ sơ.', 'error');
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
          throw new Error(`Loại ghế "${tCode}" chưa được cấu hình trong hệ thống.`);
        }
      }

      // Validate and format the complete layout before creating the room so a
      // malformed manual couple-seat selection cannot leave an orphan DRAFT.
      const seatsList = buildSeatItems({ matrix, rows, cols, skipIO, typeMapping });

      // Create the auditorium and its initial booking layout in one backend
      // transaction so a failed layout can never leave an orphan room draft.
      const roomPayload = {
        name: roomName.trim(),
        screenType,
        soundType,
        capacity: approvedCapacity,
        cleaningBufferMinutes: parseInt(cleaningBuffer) || 15
      };

      const auditoriumRes = await adminRoomService.createAuditoriumWithLayout(
        cinemaId,
        roomPayload,
        { seats: seatsList, capacity: approvedCapacity },
      );
      if (!auditoriumRes?.success || !auditoriumRes.data) {
        throw new Error(auditoriumRes?.message || 'Không thể tạo phòng chiếu');
      }

      triggerToast?.(
        `Đã tạo phòng "${roomName}" với ${stats.activeSeats} ghế. Phòng đang ở bước thiết lập để bạn kiểm tra trước khi mở bán.`
      );
      setHasUnsavedChanges(false);
      navigate('/admin/rooms');
    } catch (err) {
      console.error('Failed to save room:', err);
      triggerToast?.('Lỗi: ' + (err.message || 'Không thể lưu sơ đồ phòng chiếu'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const leaveEditor = async () => {
    if (!hasUnsavedChanges) {
      navigate('/admin/rooms');
      return;
    }
    const confirmed = triggerConfirm
      ? await triggerConfirm({
          title: 'Rời bản nháp chưa lưu?',
          message: 'Các thay đổi trên sơ đồ và thông tin phòng sẽ bị mất.',
          confirmLabel: 'Rời trang',
          tone: 'danger',
        })
      : window.confirm('Các thay đổi chưa lưu sẽ bị mất. Rời trang?');
    if (confirmed) navigate('/admin/rooms');
  };

  return (
    <div className="flex flex-col flex-1 h-screen overflow-hidden bg-zinc-950 font-sans text-white">
      
      {/* Top Header */}
      <header className="h-16 bg-zinc-900 border-b border-zinc-800 px-6 flex justify-between items-center select-none shrink-0">
        <div className="flex items-center gap-3">
          <button 
            onClick={leaveEditor}
            className="inline-flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-bold text-zinc-400 transition-all hover:bg-zinc-800 hover:text-white"
          >
            <ArrowLeft className="w-5 h-5" />
            Quay lại
          </button>
          <div>
            <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">
              TẠO PHÒNG CHIẾU MỚI
            </h2>
            <p className="text-[10px] text-zinc-450 font-bold uppercase tracking-wider mt-0.5">
              {cinemaName || 'Đang tải thông tin cụm rạp...'}
            </p>
          </div>
          <span className="rounded-lg border border-amber-500/20 bg-amber-500/5 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-amber-300">
            Bản nháp · Chưa xác nhận
          </span>
        </div>

        <div className="flex items-center gap-2">
          <span className="mr-2 text-[10px] font-bold text-zinc-500">
            {hasUnsavedChanges ? 'Có thay đổi chưa lưu' : 'Chưa có thay đổi mới'}
          </span>
          <button
            type="button"
            onClick={undo}
            disabled={historyState.undoCount === 0}
            aria-label="Hoàn tác"
            className="rounded-lg border border-zinc-800 p-2 text-zinc-300 disabled:opacity-30"
          >
            <Undo2 className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={redo}
            disabled={historyState.redoCount === 0}
            aria-label="Làm lại"
            className="rounded-lg border border-zinc-800 p-2 text-zinc-300 disabled:opacity-30"
          >
            <Redo2 className="h-4 w-4" />
          </button>
          <button
            onClick={handleSave}
            disabled={isSubmitting}
            className="flex items-center gap-2 bg-brand-orange hover:bg-orange-500 disabled:bg-zinc-800 disabled:text-zinc-650 text-black text-xs font-black py-2 px-4 rounded-xl uppercase tracking-wider transition-all"
          >
            <Save className="w-4 h-4" />
            <span>{isSubmitting ? 'Đang lưu...' : 'Tạo bản nháp phòng'}</span>
          </button>
        </div>
      </header>

      <div className="grid min-h-0 flex-1 grid-cols-[230px_minmax(0,1fr)_340px] overflow-hidden">
        <aside className="overflow-y-auto border-r border-zinc-800 bg-zinc-900/70 p-4 select-none">
          <p className="mb-3 text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Công cụ sơ đồ</p>
          <button
            onClick={() => setIsWizardOpen(true)}
            className="mb-4 flex w-full items-center justify-center gap-2 rounded-xl border border-brand-orange/40 bg-zinc-950 px-4 py-3 text-xs font-black uppercase tracking-wider text-brand-orange transition hover:bg-brand-orange/10"
          >
            <Sparkles className="h-4 w-4" />
            Tạo bố cục khởi đầu
          </button>
          <BrushToolbar
            activeBrush={activeBrush}
            setActiveBrush={setActiveBrush}
            orientation="vertical"
          />
          <div className="mt-4 rounded-xl border border-sky-500/20 bg-sky-500/5 p-3 text-[11px] leading-5 text-sky-200">
            Lối đi và cửa là dữ liệu số hóa theo hồ sơ đã duyệt; LoraFilm không tự xác nhận PCCC.
          </div>
        </aside>

        <main className="relative flex min-w-0 flex-col items-center overflow-auto bg-zinc-950 p-6 md:p-8">
          <div className="mb-6 w-full max-w-5xl rounded-2xl border border-zinc-900 bg-zinc-900/20 px-4 py-3 text-xs text-zinc-400">
            Canvas sơ đồ · Màn hình chiếu ở phía trên · {stats.activeSeats} vị trí đặt vé
          </div>
          <SeatGridDesigner
            matrix={matrix}
            rows={rows}
            cols={cols}
            skipIO={skipIO}
            isLayoutEditable={true}
            onCellMouseDown={handleCellMouseDown}
            onCellMouseEnter={handleCellMouseEnter}
          />
          
          <div className="flex items-center gap-2 mt-8 text-zinc-500 text-[10px] uppercase font-bold tracking-wider max-w-lg bg-zinc-900/20 border border-zinc-900 p-4 rounded-xl select-none">
            <AlertCircle className="w-4 h-4 text-brand-orange shrink-0" />
            <span>Mẹo: Nhấn chuột xuống và di (kéo rê chuột) qua lưới để vẽ hàng ghế/lối đi nhanh hơn. Chỉ có ghế ngồi (Thường, VIP, Đôi, Khuyết tật) được lưu vào cơ sở dữ liệu.</span>
          </div>

        </main>

        <aside className="overflow-y-auto border-l border-zinc-800 bg-zinc-900/60 p-5 select-none">
          <RoomForm
            roomName={roomName}
            setRoomName={(value) => { setRoomName(value); setHasUnsavedChanges(true); }}
            screenType={screenType}
            setScreenType={(value) => { setScreenType(value); setHasUnsavedChanges(true); }}
            soundType={soundType}
            setSoundType={(value) => { setSoundType(value); setHasUnsavedChanges(true); }}
            cleaningBuffer={cleaningBuffer}
            setCleaningBuffer={(value) => { setCleaningBuffer(value); setHasUnsavedChanges(true); }}
            capacity={stats.activeSeats}
            approvedCapacity={approvedCapacity}
            setApprovedCapacity={(value) => {
              setApprovedCapacity(value);
              setHasUnsavedChanges(true);
            }}
            isCreateMode={true}
          />

          <div className="mt-6 space-y-4 border-t border-zinc-800 pt-5">
            <div className="flex items-center gap-2">
              <Sliders className="h-4 w-4 text-brand-orange" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-white">Kích thước canvas</h3>
            </div>
            <RangeField
              label="Số hàng"
              value={rows}
              onChange={(value) => { pushHistory(matrix); setRows(value); }}
            />
            <RangeField
              label="Số cột"
              value={cols}
              onChange={(value) => { pushHistory(matrix); setCols(value); }}
            />
            <label className="flex cursor-pointer items-center gap-2 text-xs font-bold text-zinc-400">
              <input
                type="checkbox"
                checked={skipIO}
                onChange={(event) => {
                  setSkipIO(event.target.checked);
                  setHasUnsavedChanges(true);
                }}
                className="h-4 w-4 accent-orange-500"
              />
              Bỏ qua ký tự I, O
            </label>
          </div>
          <StatsPanel stats={stats} />
        </aside>

        <AutoLayoutWizardModal 
          isOpen={isWizardOpen} 
          onClose={() => setIsWizardOpen(false)} 
          onApply={handleApplyWizard} 
          currentSkipIO={skipIO}
          currentMatrix={matrix}
        />
      </div>
    </div>
  );
}

function RangeField({ label, value, onChange }) {
  return (
    <label className="block space-y-2">
      <span className="flex items-center justify-between text-[10px] font-black uppercase tracking-widest text-zinc-500">
        <span>{label}</span>
        <span className="rounded-md bg-zinc-950 px-2 py-1 text-zinc-200">{value}</span>
      </span>
      <input
        type="range"
        min="3"
        max="30"
        value={value}
        onChange={event => onChange(Number.parseInt(event.target.value, 10))}
        className="h-1.5 w-full cursor-pointer accent-orange-500"
      />
    </label>
  );
}
