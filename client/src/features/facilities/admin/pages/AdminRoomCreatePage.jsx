import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  Armchair,
  CheckCircle2,
  Copy,
  Grid3X3,
  Library,
  Minus,
  Plus,
  Redo2,
  Save,
  Sparkles,
  Undo2,
  Wrench,
} from 'lucide-react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import apiClient from '@/services/apiClient';
import AutoLayoutWizardModal from '@/features/facilities/admin/components/AutoLayoutWizardModal';
import BrushToolbar from '@/features/facilities/admin/components/BrushToolbar';
import SeatGridDesigner from '@/features/facilities/admin/components/SeatGridDesigner';
import {
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '@/features/facilities/admin/utils/facilityPresentation';
import {
  buildSeatItems,
  calculateRowLabel,
} from '@/features/facilities/admin/utils/seatLayout';

const SOURCE_AUDITORIUM = 'AUDITORIUM';
const SOURCE_TEMPLATE = 'TEMPLATE';
const SOURCE_MANUAL = 'MANUAL';

const cloneMatrix = source => source.map(row => row.map(cell => ({ ...cell })));
const responseMatrixToCells = source => (
  Array.isArray(source)
    ? source.map(row => row.map(type => ({ type })))
    : []
);

function summarizeMatrix(matrix) {
  const stats = {
    standard: 0,
    vip: 0,
    couple: 0,
    disabled: 0,
    exits: 0,
    aisleCells: 0,
  };
  matrix.forEach(row => row.forEach((cell) => {
    if (cell.type === 'STANDARD') stats.standard++;
    else if (cell.type === 'VIP') stats.vip++;
    else if (cell.type === 'COUPLE') stats.couple++;
    else if (cell.type === 'DISABLED') stats.disabled++;
    else if (cell.type === 'EXIT') stats.exits++;
    else if (cell.type === 'AISLE') stats.aisleCells++;
  }));
  stats.activeSeats = stats.standard + stats.vip + stats.couple + stats.disabled;
  stats.coupleModules = Math.floor(stats.couple / 2);
  stats.ticketingPositions = stats.activeSeats - stats.coupleModules;
  return stats;
}

function suggestRoomName(rooms) {
  const numbers = rooms
    .map(room => room.name?.match(/phòng\s*0*(\d+)/i)?.[1])
    .filter(Boolean)
    .map(Number);
  const next = numbers.length > 0 ? Math.max(...numbers) + 1 : rooms.length + 1;
  return `Phòng ${String(next).padStart(2, '0')}`;
}

function deriveCleaningBuffer(rooms) {
  const frequency = new Map();
  rooms.forEach((room) => {
    if (room.cleaningBufferMinutes == null) return;
    const value = Number(room.cleaningBufferMinutes);
    frequency.set(value, (frequency.get(value) || 0) + 1);
  });
  return [...frequency.entries()]
    .sort((left, right) => right[1] - left[1] || right[0] - left[0])[0]?.[0] ?? 15;
}

function hasValidCouplePairs(matrix) {
  return matrix.every((row) => {
    for (let column = 0; column < row.length; column++) {
      if (row[column].type !== 'COUPLE') continue;
      if (row[column + 1]?.type !== 'COUPLE') return false;
      column++;
    }
    return true;
  });
}

function localValidation(stats, matrix) {
  const couplePairsValid = hasValidCouplePairs(matrix);
  return [
    { code: 'UNIQUE_SEAT_CODES', label: 'Không trùng mã ghế', passed: true, severity: 'SUCCESS' },
    { code: 'NO_SEAT_ON_AISLE', label: 'Không có ghế nằm trên lối đi', passed: true, severity: 'SUCCESS' },
    { code: 'CAPACITY_MATCH', label: 'Sức chứa tự tính từ sơ đồ', passed: stats.activeSeats > 0, severity: 'SUCCESS' },
    {
      code: 'ACCESSIBLE_POSITION',
      label: 'Có vị trí tiếp cận',
      passed: stats.disabled > 0,
      severity: stats.disabled > 0 ? 'SUCCESS' : 'WARNING',
    },
    {
      code: 'COUPLE_PAIR',
      label: 'Ghế đôi tạo thành module đầy đủ',
      passed: couplePairsValid,
      severity: couplePairsValid ? 'SUCCESS' : 'ERROR',
    },
    { code: 'WITHIN_CANVAS', label: 'Không có phần tử nằm ngoài canvas', passed: true, severity: 'SUCCESS' },
  ];
}

export default function AdminRoomCreatePage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const cinemaId = searchParams.get('cinemaId');

  const [cinema, setCinema] = useState(null);
  const [sourceMode, setSourceMode] = useState(null);
  const [templates, setTemplates] = useState([]);
  const [sourceRooms, setSourceRooms] = useState([]);
  const [selectedSource, setSelectedSource] = useState(null);
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isSourceLoading, setIsSourceLoading] = useState(false);

  const [roomName, setRoomName] = useState('');
  const [screenType, setScreenType] = useState('STANDARD');
  const [soundType, setSoundType] = useState('STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(15);
  const [isCleaningOverride, setIsCleaningOverride] = useState(false);

  const [rows, setRows] = useState(0);
  const [cols, setCols] = useState(0);
  const [matrix, setMatrix] = useState([]);
  const [activeBrush, setActiveBrush] = useState('STANDARD');
  const [isMouseDown, setIsMouseDown] = useState(false);
  const [isWizardOpen, setIsWizardOpen] = useState(false);
  const [previewScale, setPreviewScale] = useState(0.8);
  const [previewDimensions, setPreviewDimensions] = useState({ width: 0, height: 0 });
  const [selectedCell, setSelectedCell] = useState(null);

  const [dbSeatTypes, setDbSeatTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [historyState, setHistoryState] = useState({ undoCount: 0, redoCount: 0 });
  const undoStackRef = useRef([]);
  const redoStackRef = useRef([]);
  const previewViewportRef = useRef(null);
  const previewContentRef = useRef(null);

  const stats = useMemo(() => summarizeMatrix(matrix), [matrix]);
  const isManualMode = sourceMode === SOURCE_MANUAL;
  const validationItems = isManualMode
    ? localValidation(stats, matrix)
    : (selectedSource?.validation || []);
  const isLayoutValid = validationItems.every(item => item.passed || item.severity === 'WARNING')
    && stats.activeSeats > 0;
  const sourceLabel = selectedSource
    ? `${selectedSource.name}${selectedSource.layoutVersion ? ` — Phiên bản ${selectedSource.layoutVersion}` : ''}`
    : (isManualMode ? 'Bố cục thủ công' : 'Chưa chọn nguồn');

  useEffect(() => {
    const viewport = previewViewportRef.current;
    const content = previewContentRef.current;
    if (!viewport || !content || matrix.length === 0) return undefined;

    const fitPreview = () => {
      const naturalWidth = content.offsetWidth;
      const naturalHeight = content.offsetHeight;
      const availableWidth = viewport.clientWidth - 48;
      if (naturalWidth <= 0 || naturalHeight <= 0 || availableWidth <= 0) return;

      setPreviewDimensions(current => (
        current.width === naturalWidth && current.height === naturalHeight
          ? current
          : { width: naturalWidth, height: naturalHeight }
      ));

      const rawScale = Math.min(1, availableWidth / naturalWidth);
      const fittedScale = Math.max(0.5, Math.floor(rawScale * 20) / 20);
      setPreviewScale(fittedScale);
    };

    fitPreview();
    if (typeof ResizeObserver === 'undefined') return undefined;

    const resizeObserver = new ResizeObserver(fitPreview);
    resizeObserver.observe(viewport);
    return () => resizeObserver.disconnect();
  }, [cols, rows, sourceMode, selectedSource?.sourcePublicId, matrix.length]);

  const createSnapshot = () => ({ matrix: cloneMatrix(matrix), rows, cols });
  const restoreSnapshot = (snapshot) => {
    setMatrix(cloneMatrix(snapshot.matrix));
    setRows(snapshot.rows);
    setCols(snapshot.cols);
  };
  const pushHistory = () => {
    undoStackRef.current = [...undoStackRef.current.slice(-49), createSnapshot()];
    redoStackRef.current = [];
    setHistoryState({ undoCount: undoStackRef.current.length, redoCount: 0 });
    setHasUnsavedChanges(true);
  };
  const undo = () => {
    const previous = undoStackRef.current.pop();
    if (!previous) return;
    redoStackRef.current.push(createSnapshot());
    restoreSnapshot(previous);
    setHistoryState({
      undoCount: undoStackRef.current.length,
      redoCount: redoStackRef.current.length,
    });
  };
  const redo = () => {
    const next = redoStackRef.current.pop();
    if (!next) return;
    undoStackRef.current.push(createSnapshot());
    restoreSnapshot(next);
    setHistoryState({
      undoCount: undoStackRef.current.length,
      redoCount: redoStackRef.current.length,
    });
  };

  const applyPreview = (preview, markChanged = true) => {
    const nextMatrix = responseMatrixToCells(preview.matrix);
    setSelectedSource(preview);
    setMatrix(nextMatrix);
    setRows(preview.rows || nextMatrix.length);
    setCols(preview.columns || nextMatrix[0]?.length || 0);
    setScreenType(preview.recommendedScreenType || 'STANDARD');
    setSoundType(preview.recommendedSoundType || 'STANDARD');
    setSelectedCell(null);
    if (markChanged) setHasUnsavedChanges(true);
  };

  useEffect(() => {
    const warnBeforeUnload = (event) => {
      if (!hasUnsavedChanges) return;
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => window.removeEventListener('beforeunload', warnBeforeUnload);
  }, [hasUnsavedChanges]);

  useEffect(() => {
    if (!cinemaId) {
      triggerToast?.('Không xác định được cụm rạp cần thêm phòng chiếu.', 'error');
      navigate('/admin/rooms');
      return;
    }

    let active = true;
    const load = async () => {
      setIsInitialLoading(true);
      try {
        const [cinemaResult, templateResult, seatTypeResult] = await Promise.allSettled([
          adminCinemaService.getAdminCinemaDetail(cinemaId),
          adminRoomService.getLayoutTemplates(),
          adminRoomService.getSeatTypes(),
        ]);
        if (!active) return;

        if (cinemaResult.status === 'rejected') throw cinemaResult.reason;
        const cinemaRes = cinemaResult.value;
        const templateRes = templateResult.status === 'fulfilled' ? templateResult.value : null;
        const seatTypeRes = seatTypeResult.status === 'fulfilled' ? seatTypeResult.value : null;

        const cinemaData = cinemaRes?.success ? cinemaRes.data : null;
        const roomList = Array.isArray(cinemaData?.activeAuditoriums)
          ? cinemaData.activeAuditoriums
          : [];
        const cloneCandidates = roomList.filter(room => (
          room.status === 'ACTIVE' && Number(room.capacity || 0) > 0
        ));
        const templateList = templateRes?.success && Array.isArray(templateRes.data)
          ? templateRes.data
          : [];

        setCinema(cinemaData);
        setSourceRooms(cloneCandidates);
        setTemplates(templateList);
        setDbSeatTypes(seatTypeRes?.success && Array.isArray(seatTypeRes.data) ? seatTypeRes.data : []);
        setRoomName(suggestRoomName(roomList));
        setCleaningBuffer(deriveCleaningBuffer(roomList));

        if (cloneCandidates.length > 0) {
          setSourceMode(SOURCE_AUDITORIUM);
          setIsSourceLoading(true);
          try {
            const previewRes = await adminRoomService.getClonePreview(cloneCandidates[0].publicId);
            if (active && previewRes?.success && previewRes.data) applyPreview(previewRes.data, false);
          } finally {
            if (active) setIsSourceLoading(false);
          }
        } else if (templateList.length > 0) {
          const recommended = templateList.find(template => template.capacity === 120) || templateList[0];
          setSourceMode(SOURCE_TEMPLATE);
          applyPreview(recommended, false);
        }
      } catch (error) {
        if (active) {
          triggerToast?.(
            error.response?.data?.message || error.message || 'Không thể tải nguồn sơ đồ',
            'error',
          );
        }
      } finally {
        if (active) setIsInitialLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, [cinemaId, navigate, triggerToast]);

  useEffect(() => {
    if (!isManualMode || rows <= 0 || cols <= 0) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMatrix((previous) => {
      if (previous.length === rows && previous[0]?.length === cols) return previous;
      return Array.from({ length: rows }, (_, row) => (
        Array.from({ length: cols }, (_, column) => (
          previous[row]?.[column] ? { ...previous[row][column] } : { type: 'STANDARD' }
        ))
      ));
    });
  }, [cols, isManualMode, rows]);

  useEffect(() => {
    const release = () => setIsMouseDown(false);
    window.addEventListener('mouseup', release);
    return () => window.removeEventListener('mouseup', release);
  }, []);

  const selectSourceRoom = async (room) => {
    setSourceMode(SOURCE_AUDITORIUM);
    setIsSourceLoading(true);
    try {
      const response = await adminRoomService.getClonePreview(room.publicId);
      if (!response?.success || !response.data) throw new Error('Không thể xem trước phòng nguồn');
      applyPreview(response.data);
    } catch (error) {
      triggerToast?.(error.response?.data?.message || error.message, 'error');
    } finally {
      setIsSourceLoading(false);
    }
  };

  const selectTemplate = (template) => {
    setSourceMode(SOURCE_TEMPLATE);
    applyPreview(template);
  };

  const chooseMode = (mode) => {
    if (mode === SOURCE_AUDITORIUM) {
      setSourceMode(mode);
      if (!selectedSource || selectedSource.sourceType !== SOURCE_AUDITORIUM) {
        if (sourceRooms[0]) selectSourceRoom(sourceRooms[0]);
      }
      return;
    }
    setSourceMode(mode);
    if (!selectedSource || selectedSource.sourceType !== SOURCE_TEMPLATE) {
      const recommended = templates.find(template => template.capacity === 120) || templates[0];
      if (recommended) selectTemplate(recommended);
    }
  };

  const openManualBlank = () => {
    setSourceMode(SOURCE_MANUAL);
    setSelectedSource(null);
    setRows(10);
    setCols(12);
    setMatrix(Array.from({ length: 10 }, () => (
      Array.from({ length: 12 }, () => ({ type: 'STANDARD' }))
    )));
    setHasUnsavedChanges(true);
    setIsWizardOpen(true);
  };

  const editSelectedSource = () => {
    if (!selectedSource) return;
    setSourceMode(SOURCE_MANUAL);
    setHasUnsavedChanges(true);
    undoStackRef.current = [];
    redoStackRef.current = [];
    setHistoryState({ undoCount: 0, redoCount: 0 });
  };

  const handleCellPaint = (row, column) => {
    setMatrix(previous => previous.map((cells, rowIndex) => (
      cells.map((cell, columnIndex) => (
        rowIndex === row && columnIndex === column ? { type: activeBrush } : { ...cell }
      ))
    )));
  };
  const handleCellMouseDown = (row, column) => {
    pushHistory();
    setIsMouseDown(true);
    handleCellPaint(row, column);
  };
  const handleCellMouseEnter = (row, column) => {
    if (isMouseDown) handleCellPaint(row, column);
  };
  const handleApplyWizard = (nextMatrix, nextRows, nextCols) => {
    pushHistory();
    setRows(nextRows);
    setCols(nextCols);
    setMatrix(nextMatrix);
    setIsWizardOpen(false);
  };

  const ensureSeatTypesExist = async () => {
    let currentTypes = [...dbSeatTypes];
    const response = await adminRoomService.getSeatTypes();
    if (response?.success && Array.isArray(response.data)) currentTypes = response.data;
    const defaults = [
      { code: 'STANDARD', name: 'Ghế Tiêu Chuẩn', description: 'Ghế ngồi tiêu chuẩn' },
      { code: 'VIP', name: 'Ghế VIP', description: 'Ghế ngồi cao cấp, vị trí đẹp' },
      { code: 'COUPLE', name: 'Ghế Đôi', description: 'Ghế đôi dành cho cặp đôi ở hàng cuối' },
      { code: 'DISABLED', name: 'Vị trí tiếp cận', description: 'Vị trí dành cho xe lăn' },
    ];
    for (const definition of defaults) {
      if (currentTypes.some(type => type.code === definition.code)) continue;
      const created = await apiClient.post('/api/admin/seat-types', definition);
      if (created.data?.data) currentTypes = [...currentTypes, created.data.data];
    }
    setDbSeatTypes(currentTypes);
    return currentTypes;
  };

  const handleSave = async () => {
    if (!roomName.trim()) {
      triggerToast?.('Vui lòng nhập tên phòng chiếu.', 'error');
      return;
    }
    if (!selectedSource && !isManualMode) {
      triggerToast?.('Vui lòng chọn một nguồn sơ đồ.', 'error');
      return;
    }
    if (!isLayoutValid) {
      triggerToast?.('Sơ đồ còn lỗi chặn. Hãy kiểm tra phần xác thực trước khi tạo.', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      const commonPayload = {
        cinemaPublicId: cinemaId,
        name: roomName.trim(),
        screenType,
        soundType,
        cleaningBufferMinutes: Number(cleaningBuffer),
      };
      let response;
      if (sourceMode === SOURCE_TEMPLATE) {
        response = await adminRoomService.createAuditoriumFromTemplate({
          ...commonPayload,
          templatePublicId: selectedSource.sourcePublicId,
        });
      } else if (sourceMode === SOURCE_AUDITORIUM) {
        response = await adminRoomService.cloneAuditoriumAsNew(
          selectedSource.sourcePublicId,
          commonPayload,
        );
      } else {
        const seatTypes = await ensureSeatTypesExist();
        const typeMapping = Object.fromEntries(seatTypes.map(type => [type.code, type.publicId]));
        const seats = buildSeatItems({ matrix, rows, cols, skipIO: true, typeMapping });
        response = await adminRoomService.createAuditoriumWithLayout(
          cinemaId,
          {
            name: commonPayload.name,
            screenType,
            soundType,
            capacity: stats.activeSeats,
            cleaningBufferMinutes: Number(cleaningBuffer),
          },
          { seats, capacity: stats.activeSeats },
        );
      }
      if (!response?.success || !response.data) {
        throw new Error(response?.message || 'Không thể tạo phòng chiếu');
      }
      setHasUnsavedChanges(false);
      triggerToast?.(`Đã tạo phòng “${roomName.trim()}” từ ${sourceLabel}.`);
      navigate('/admin/rooms');
    } catch (error) {
      triggerToast?.(
        error.response?.data?.message || error.message || 'Không thể tạo phòng chiếu',
        'error',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const leavePage = async () => {
    if (!hasUnsavedChanges) {
      navigate('/admin/rooms');
      return;
    }
    const confirmed = triggerConfirm
      ? await triggerConfirm({
          title: 'Rời cấu hình chưa lưu?',
          message: 'Nguồn sơ đồ và thông tin phòng đang nhập sẽ bị mất.',
          confirmLabel: 'Rời trang',
          tone: 'danger',
        })
      : window.confirm('Các thay đổi chưa lưu sẽ bị mất. Rời trang?');
    if (confirmed) navigate('/admin/rooms');
  };

  if (isInitialLoading) {
    return (
      <div className="flex min-h-[560px] flex-1 items-center justify-center bg-zinc-950 text-white">
        <div className="text-center">
          <Sparkles className="mx-auto h-8 w-8 animate-pulse text-brand-orange" />
          <p className="mt-4 text-sm font-bold text-zinc-400">Đang tải nguồn sơ đồ chuẩn hóa...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-1 flex-col overflow-hidden bg-zinc-950 text-white">
      <header className="flex min-h-16 shrink-0 items-center justify-between gap-4 border-b border-zinc-800 bg-zinc-900 px-6">
        <div className="flex min-w-0 items-center gap-3">
          <button
            type="button"
            onClick={leavePage}
            className="inline-flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-bold text-zinc-400 hover:bg-zinc-800 hover:text-white"
          >
            <ArrowLeft className="h-5 w-5" />
            Quay lại
          </button>
          <div className="min-w-0">
            <h1 className="truncate text-sm font-black uppercase tracking-wider">Tạo phòng chiếu mới</h1>
            <p className="mt-0.5 truncate text-[10px] font-bold uppercase tracking-wider text-zinc-500">
              {cinema?.name || 'Cụm rạp'} · Chọn nguồn → Xem kết quả → Tạo phòng
            </p>
          </div>
          <span className="hidden rounded-lg border border-emerald-500/20 bg-emerald-500/5 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-emerald-300 lg:inline-flex">
            Khởi tạo từ cấu hình chuẩn
          </span>
        </div>
        <div className="flex items-center gap-2">
          {isManualMode && (
            <>
              <button type="button" onClick={undo} disabled={historyState.undoCount === 0} aria-label="Hoàn tác" className="rounded-lg border border-zinc-800 p-2 text-zinc-300 disabled:opacity-30">
                <Undo2 className="h-4 w-4" />
              </button>
              <button type="button" onClick={redo} disabled={historyState.redoCount === 0} aria-label="Làm lại" className="rounded-lg border border-zinc-800 p-2 text-zinc-300 disabled:opacity-30">
                <Redo2 className="h-4 w-4" />
              </button>
            </>
          )}
          <button
            type="button"
            onClick={handleSave}
            disabled={isSubmitting || isSourceLoading || !isLayoutValid}
            className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase tracking-wider text-black transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:bg-zinc-800 disabled:text-zinc-500"
          >
            <Save className="h-4 w-4" />
            {isSubmitting ? 'Đang tạo...' : 'Tạo phòng'}
          </button>
        </div>
      </header>

      <div className="grid min-h-0 flex-1 grid-cols-[300px_minmax(0,1fr)_350px] overflow-hidden">
        <aside className="overflow-y-auto border-r border-zinc-800 bg-zinc-900/65 p-4">
          {isManualMode ? (
            <ManualTools
              activeBrush={activeBrush}
              setActiveBrush={setActiveBrush}
              rows={rows}
              cols={cols}
              onOpenWizard={() => setIsWizardOpen(true)}
              onResize={(dimension, value) => {
                pushHistory();
                if (dimension === 'rows') setRows(value);
                else setCols(value);
              }}
              onReturn={() => {
                if (selectedSource) {
                  setSourceMode(selectedSource.sourceType);
                  applyPreview(selectedSource);
                } else {
                  chooseMode(templates.length > 0 ? SOURCE_TEMPLATE : SOURCE_AUDITORIUM);
                }
              }}
            />
          ) : (
            <SourcePicker
              mode={sourceMode}
              sourceRooms={sourceRooms}
              templates={templates}
              selectedSource={selectedSource}
              isLoading={isSourceLoading}
              onModeChange={chooseMode}
              onRoomSelect={selectSourceRoom}
              onTemplateSelect={selectTemplate}
              onManual={openManualBlank}
            />
          )}
        </aside>

        <main className="relative min-w-0 overflow-auto bg-zinc-950 p-6">
          <div className="mx-auto max-w-6xl">
            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 px-4 py-3">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                  {isManualMode ? 'Trình chỉnh sửa kỹ thuật' : 'Preview chỉ đọc'}
                </p>
                <p className="mt-1 text-sm font-bold text-white">Nguồn: {sourceLabel}</p>
              </div>
              <div className="flex items-center gap-2">
                {!isManualMode && selectedSource && (
                  <button
                    type="button"
                    onClick={editSelectedSource}
                    className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange hover:text-brand-orange"
                  >
                    <Wrench className="h-4 w-4" />
                    Chỉnh sửa nâng cao
                  </button>
                )}
                <button type="button" onClick={() => setPreviewScale(value => Math.max(0.5, value - 0.05))} aria-label="Thu nhỏ sơ đồ" className="rounded-lg border border-zinc-800 p-2 text-zinc-400 hover:text-white">
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-12 text-center text-xs font-bold text-zinc-400">{Math.round(previewScale * 100)}%</span>
                <button type="button" onClick={() => setPreviewScale(value => Math.min(1.2, value + 0.05))} aria-label="Phóng to sơ đồ" className="rounded-lg border border-zinc-800 p-2 text-zinc-400 hover:text-white">
                  <Plus className="h-4 w-4" />
                </button>
              </div>
            </div>

            {isSourceLoading ? (
              <div className="flex min-h-[520px] items-center justify-center rounded-3xl border border-zinc-900 bg-zinc-900/20">
                <p className="text-sm font-bold text-zinc-500">Đang dựng preview từ phòng nguồn...</p>
              </div>
            ) : matrix.length > 0 ? (
              <div ref={previewViewportRef} data-testid="seat-preview-viewport" className="overflow-auto rounded-3xl border border-zinc-900 bg-black/20 p-6">
                <div
                  data-testid="seat-preview-footprint"
                  className="mx-auto"
                  style={previewDimensions.width > 0 ? {
                    width: `${previewDimensions.width * previewScale}px`,
                    height: `${previewDimensions.height * previewScale}px`,
                  } : undefined}
                >
                  <div ref={previewContentRef} data-testid="seat-preview-content" className="w-max origin-top-left transition-transform" style={{ transform: `scale(${previewScale})` }}>
                    <SeatGridDesigner
                      matrix={matrix}
                      rows={rows}
                      cols={cols}
                      skipIO={true}
                      isLayoutEditable={isManualMode}
                      onCellMouseDown={isManualMode ? handleCellMouseDown : undefined}
                      onCellMouseEnter={isManualMode ? handleCellMouseEnter : undefined}
                      onCellClick={!isManualMode ? (row, column, cell) => setSelectedCell({ row, column, type: cell.type }) : undefined}
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex min-h-[520px] items-center justify-center rounded-3xl border border-dashed border-zinc-800 bg-zinc-900/15">
                <div className="text-center">
                  <Grid3X3 className="mx-auto h-10 w-10 text-zinc-700" />
                  <p className="mt-4 text-sm font-bold text-zinc-400">Chọn một phòng hoặc mẫu để xem kết quả</p>
                </div>
              </div>
            )}

            {selectedCell && !isManualMode && (
              <div className="mt-4 rounded-2xl border border-sky-500/20 bg-sky-500/5 px-4 py-3 text-xs text-sky-200">
                Hàng {calculateRowLabel(selectedCell.row, true)} · Cột {selectedCell.column + 1} · <strong>{cellLabel(selectedCell.type)}</strong>
              </div>
            )}
            <LayoutLegend />
          </div>
        </main>

        <aside className="overflow-y-auto border-l border-zinc-800 bg-zinc-900/55 p-5">
          <OperationalRoomForm
            roomName={roomName}
            setRoomName={(value) => { setRoomName(value); setHasUnsavedChanges(true); }}
            screenType={screenType}
            setScreenType={(value) => { setScreenType(value); setHasUnsavedChanges(true); }}
            soundType={soundType}
            setSoundType={(value) => { setSoundType(value); setHasUnsavedChanges(true); }}
            cleaningBuffer={cleaningBuffer}
            setCleaningBuffer={(value) => { setCleaningBuffer(value); setHasUnsavedChanges(true); }}
            isCleaningOverride={isCleaningOverride}
            setIsCleaningOverride={setIsCleaningOverride}
            sourceLabel={sourceLabel}
            stats={stats}
            selectedSource={selectedSource}
            validationItems={validationItems}
          />
        </aside>
      </div>

      <AutoLayoutWizardModal
        isOpen={isWizardOpen}
        onClose={() => setIsWizardOpen(false)}
        onApply={handleApplyWizard}
        currentSkipIO={true}
        currentMatrix={matrix}
      />
    </div>
  );
}

function SourcePicker({ mode, sourceRooms, templates, selectedSource, isLoading, onModeChange, onRoomSelect, onTemplateSelect, onManual }) {
  return (
    <div>
      <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Nguồn sơ đồ</p>
      <div className="mt-3 space-y-2">
        <SourceModeButton active={mode === SOURCE_AUDITORIUM} icon={Copy} title="Sao chép phòng hiện có" description="Nhanh nhất · giữ nguyên bố cục và loại ghế" onClick={() => onModeChange(SOURCE_AUDITORIUM)} />
        <SourceModeButton active={mode === SOURCE_TEMPLATE} icon={Library} title="Dùng mẫu có sẵn" description="Cấu hình phòng đã được LoraFilm chuẩn hóa" onClick={() => onModeChange(SOURCE_TEMPLATE)} />
      </div>

      <div className="my-4 h-px bg-zinc-800" />
      {mode === SOURCE_AUDITORIUM && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Phòng nguồn</p>
            {sourceRooms.length > 0 && <span className="text-[10px] text-zinc-600">{sourceRooms.length} phòng</span>}
          </div>
          {sourceRooms.length === 0 ? (
            <p className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs leading-5 text-amber-200">Chưa có phòng ACTIVE đủ điều kiện sao chép. Hãy dùng mẫu hệ thống.</p>
          ) : sourceRooms.map((room, index) => (
            <button
              type="button"
              key={room.publicId}
              onClick={() => onRoomSelect(room)}
              disabled={isLoading}
              className={`w-full rounded-2xl border p-3 text-left transition ${selectedSource?.sourcePublicId === room.publicId ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-950/70 hover:border-zinc-700'}`}
            >
              {index === 0 && <span className="mb-2 inline-flex rounded-md bg-brand-orange px-2 py-1 text-[9px] font-black uppercase text-black">Đề xuất cho bạn</span>}
              <p className="font-black text-white">{room.name}</p>
              <p className="mt-1 text-[11px] text-zinc-500">{SCREEN_TYPE_LABELS[room.screenType] || room.screenType} · {room.capacity} người</p>
              <p className="mt-2 text-[10px] text-zinc-600">Dọn phòng {room.cleaningBufferMinutes ?? 15} phút</p>
            </button>
          ))}
        </div>
      )}

      {mode === SOURCE_TEMPLATE && (
        <div className="space-y-3">
          <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Thư viện mẫu</p>
          {templates.map(template => (
            <button
              type="button"
              key={template.sourcePublicId}
              onClick={() => onTemplateSelect(template)}
              className={`w-full rounded-2xl border p-3 text-left transition ${selectedSource?.sourcePublicId === template.sourcePublicId ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-950/70 hover:border-zinc-700'}`}
            >
              <MiniLayout matrix={template.matrix} />
              <p className="mt-3 font-black text-white">{template.name}</p>
              <p className="mt-1 text-[11px] text-zinc-500">{template.capacity} người · {template.vipSeats} VIP · {template.coupleModules} module đôi</p>
              <p className="mt-1 text-[10px] text-zinc-600">{template.aisleCount} lối đi · {template.doorCount} cửa · {template.accessiblePositions} tiếp cận</p>
            </button>
          ))}
        </div>
      )}

      <button type="button" onClick={onManual} className="mt-5 w-full text-left text-xs font-bold leading-5 text-zinc-500 underline decoration-zinc-700 underline-offset-4 hover:text-brand-orange">
        Không tìm thấy mẫu phù hợp? Tạo bố cục thủ công nâng cao
      </button>
    </div>
  );
}

function SourceModeButton({ active, icon: Icon, title, description, onClick }) {
  return (
    <button type="button" onClick={onClick} className={`flex w-full items-start gap-3 rounded-2xl border p-3 text-left transition ${active ? 'border-brand-orange bg-brand-orange/10' : 'border-zinc-800 bg-zinc-950 hover:border-zinc-700'}`}>
      <span className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl ${active ? 'bg-brand-orange text-black' : 'bg-zinc-900 text-zinc-500'}`}><Icon className="h-4 w-4" /></span>
      <span>
        <span className="block text-xs font-black text-white">{title}</span>
        <span className="mt-1 block text-[10px] leading-4 text-zinc-500">{description}</span>
      </span>
    </button>
  );
}

function MiniLayout({ matrix = [] }) {
  const columns = matrix[0]?.length || 1;
  const colors = { STANDARD: 'bg-purple-500/70', VIP: 'bg-rose-500/80', COUPLE: 'bg-amber-400/80', DISABLED: 'bg-sky-400/90', EXIT: 'bg-emerald-500/80', AISLE: 'bg-zinc-900', EMPTY: 'bg-transparent' };
  return (
    <div className="grid h-16 w-full gap-px overflow-hidden rounded-lg border border-zinc-800 bg-black/40 p-2" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }} aria-hidden="true">
      {matrix.flatMap((row, rowIndex) => row.map((type, columnIndex) => <span key={`${rowIndex}-${columnIndex}`} className={`min-h-1 rounded-[1px] ${colors[type] || 'bg-transparent'}`} />))}
    </div>
  );
}

function ManualTools({ activeBrush, setActiveBrush, rows, cols, onOpenWizard, onResize, onReturn }) {
  return (
    <div>
      <div className="flex items-center gap-2">
        <Wrench className="h-4 w-4 text-brand-orange" />
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Chế độ kỹ thuật</p>
          <p className="mt-1 text-xs font-bold text-white">Chỉnh geometry và phân loại ghế</p>
        </div>
      </div>
      <button type="button" onClick={onReturn} className="mt-4 w-full rounded-xl border border-zinc-800 px-3 py-2 text-xs font-bold text-zinc-400 hover:text-white">Quay lại chọn nguồn</button>
      <button type="button" onClick={onOpenWizard} className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-brand-orange/40 bg-zinc-950 px-4 py-3 text-xs font-black uppercase tracking-wider text-brand-orange hover:bg-brand-orange/10">
        <Sparkles className="h-4 w-4" /> Tạo lưới ghế thủ công
      </button>
      <div className="mt-4"><BrushToolbar activeBrush={activeBrush} setActiveBrush={setActiveBrush} orientation="vertical" /></div>
      <div className="mt-5 space-y-4 border-t border-zinc-800 pt-5">
        <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Kích thước lưới</p>
        <RangeField label="Số hàng" value={rows} onChange={value => onResize('rows', value)} />
        <RangeField label="Số cột" value={cols} onChange={value => onResize('cols', value)} />
        <p className="rounded-xl border border-sky-500/20 bg-sky-500/5 p-3 text-[10px] leading-5 text-sky-200">Quy tắc hàng áp dụng toàn hệ thống: bỏ ký tự I và O.</p>
      </div>
    </div>
  );
}

function RangeField({ label, value, onChange }) {
  return (
    <label className="block">
      <span className="mb-2 flex justify-between text-[10px] font-bold uppercase text-zinc-500"><span>{label}</span><strong className="text-white">{value}</strong></span>
      <input type="range" min="4" max="20" value={value} onChange={event => onChange(Number(event.target.value))} className="w-full accent-orange-500" />
    </label>
  );
}

function OperationalRoomForm({ roomName, setRoomName, screenType, setScreenType, soundType, setSoundType, cleaningBuffer, setCleaningBuffer, isCleaningOverride, setIsCleaningOverride, sourceLabel, stats, selectedSource, validationItems }) {
  return (
    <div className="space-y-5">
      <div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-zinc-500">Thông tin phòng</p><p className="mt-1 text-xs text-zinc-500">Chỉ nhập dữ liệu phục vụ vận hành.</p></div>
      <Field label="Tên phòng chiếu"><input value={roomName} onChange={event => setRoomName(event.target.value)} placeholder="Ví dụ: Phòng 07" className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-bold text-white outline-none focus:border-brand-orange" /></Field>
      <Field label="Công nghệ màn hình">
        <select value={screenType} onChange={event => setScreenType(event.target.value)} className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-bold text-white outline-none focus:border-brand-orange">
          {Object.entries(SCREEN_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </Field>
      <Field label="Hệ thống âm thanh">
        <select value={soundType} onChange={event => setSoundType(event.target.value)} className="w-full rounded-xl border border-zinc-800 bg-zinc-950 px-4 py-3 text-sm font-bold text-white outline-none focus:border-brand-orange">
          {Object.entries(SOUND_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
      </Field>

      <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
        <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Thời gian dọn phòng</p>
        {isCleaningOverride ? (
          <div className="mt-2">
            <div className="relative"><input type="number" min="0" max="120" value={cleaningBuffer} onChange={event => setCleaningBuffer(Number(event.target.value))} className="w-full rounded-xl border border-brand-orange/50 bg-black px-3 py-2 pr-14 text-sm font-bold text-white outline-none" /><span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-zinc-500">phút</span></div>
            <button type="button" onClick={() => setIsCleaningOverride(false)} className="mt-2 text-[10px] font-bold text-zinc-500 underline hover:text-white">Dùng lại cấu hình kế thừa</button>
          </div>
        ) : (
          <><p className="mt-2 text-lg font-black text-white">Kế thừa từ cụm rạp: {cleaningBuffer} phút</p><button type="button" onClick={() => setIsCleaningOverride(true)} className="mt-2 text-[10px] font-bold text-brand-orange underline decoration-brand-orange/40 underline-offset-4">Thiết lập riêng cho phòng này</button></>
        )}
      </div>

      <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
        <div className="flex items-center justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Sức chứa của sơ đồ</p><p className="mt-1 text-2xl font-black text-emerald-300">{stats.activeSeats} người</p></div><Armchair className="h-6 w-6 text-zinc-700" /></div>
        <p className="mt-2 text-[10px] leading-4 text-zinc-500">Nguồn: {sourceLabel}</p>
      </div>
      <LayoutStats stats={stats} selectedSource={selectedSource} />
      <ValidationPanel items={validationItems} />
      <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-[10px] leading-5 text-amber-200">Phòng được tạo ở trạng thái đang thiết lập. Không sao chép lịch chiếu, bảo trì, trạng thái ghế hỏng hoặc lịch sử của phòng nguồn.</div>
    </div>
  );
}

function Field({ label, children }) {
  return <label className="block"><span className="mb-1.5 block text-[10px] font-black uppercase tracking-widest text-zinc-500">{label}</span>{children}</label>;
}

function LayoutStats({ stats, selectedSource }) {
  const items = [
    ['Sức chứa người', stats.activeSeats],
    ['Vị trí bán vé', stats.ticketingPositions],
    ['Ghế thường', stats.standard],
    ['Ghế VIP', stats.vip],
    ['Ghế đôi', `${stats.coupleModules} module / ${stats.couple} người`],
    ['Vị trí tiếp cận', stats.disabled],
    ['Lối đi', selectedSource?.aisleCount ?? 'Theo canvas'],
    ['Cửa', stats.exits],
  ];
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
      <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Cấu trúc hoàn chỉnh</p>
      <dl className="mt-3 space-y-2 text-xs">{items.map(([label, value]) => <div key={label} className="flex items-center justify-between gap-3"><dt className="text-zinc-500">{label}</dt><dd className="font-bold text-white">{value}</dd></div>)}</dl>
    </div>
  );
}

function ValidationPanel({ items }) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
      <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Kiểm tra sơ đồ</p>
      <div className="mt-3 space-y-2">
        {items.map(item => <div key={item.code} className="flex items-start gap-2 text-[11px] leading-4">{item.passed ? <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0 text-emerald-400" /> : <AlertTriangle className={`mt-0.5 h-3.5 w-3.5 shrink-0 ${item.severity === 'ERROR' ? 'text-red-400' : 'text-amber-400'}`} />}<span className={item.passed ? 'text-zinc-300' : 'text-amber-200'}>{item.label}</span></div>)}
      </div>
    </div>
  );
}

function LayoutLegend() {
  const items = [['bg-purple-500', 'Thường'], ['bg-rose-500', 'VIP'], ['bg-amber-400', 'Ghế đôi'], ['bg-sky-400', 'Tiếp cận'], ['bg-zinc-900 border border-dashed border-zinc-700', 'Lối đi'], ['bg-emerald-500', 'Cửa']];
  return <div className="mt-4 flex flex-wrap items-center justify-center gap-4 text-[10px] font-bold text-zinc-500">{items.map(([className, label]) => <span key={label} className="inline-flex items-center gap-1.5"><span className={`h-2.5 w-2.5 rounded-sm ${className}`} />{label}</span>)}</div>;
}

function cellLabel(type) {
  return { STANDARD: 'Ghế thường', VIP: 'Ghế VIP', COUPLE: 'Ghế đôi', DISABLED: 'Vị trí tiếp cận', AISLE: 'Lối đi', EXIT: 'Cửa', EMPTY: 'Vùng trống' }[type] || type;
}
