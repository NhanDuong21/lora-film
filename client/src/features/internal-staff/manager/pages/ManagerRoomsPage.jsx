import { useMemo } from 'react';
import { useOutletContext } from 'react-router-dom';
import MaintenanceWorkspace from '@/features/facilities/shared/components/MaintenanceWorkspace';
import managerCinemaService from '../services/managerCinemaService';

export default function ManagerRoomsPage() {
  const {
    selectedCinema,
    selectedCinemaId,
    cinemaState,
    triggerToast,
  } = useOutletContext();
  const rooms = useMemo(() => selectedCinema?.activeAuditoriums || [], [selectedCinema]);

  if (cinemaState.loading) {
    return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  }
  if (!selectedCinema) {
    return <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-8 text-center"><h1 className="text-xl font-black">Chưa có rạp để quản lý phòng chiếu</h1><p className="mt-2 text-sm text-amber-100/70">Quản trị viên cần phân công rạp cho tài khoản này trước.</p></div>;
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.2em] text-brand-orange">Cơ sở vật chất tại rạp</p>
          <h1 className="mt-2 text-3xl font-black">Phòng chiếu & bảo trì</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-500">
            Xem phòng nào đang phục vụ được, kiểm tra suất chiếu bị ảnh hưởng và ghi nhận thời gian phòng cần ngừng hoạt động.
          </p>
        </div>
        <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-xs font-bold text-emerald-200">
          Chỉ dữ liệu của {selectedCinema.name}
        </div>
      </header>

      <MaintenanceWorkspace
        rooms={rooms}
        loadWindows={() => managerCinemaService.getMaintenanceWindows(selectedCinemaId)}
        createWindow={(roomId, payload) => managerCinemaService.createMaintenanceWindow(
          selectedCinemaId,
          roomId,
          payload,
        )}
        cancelWindow={item => managerCinemaService.cancelMaintenanceWindow(
          selectedCinemaId,
          item.id,
        )}
        previewImpact={(roomId, payload) => managerCinemaService.previewMaintenanceImpact(
          selectedCinemaId,
          roomId,
          payload,
        )}
        viewerRole="manager"
        onNotify={triggerToast}
      />
    </div>
  );
}
