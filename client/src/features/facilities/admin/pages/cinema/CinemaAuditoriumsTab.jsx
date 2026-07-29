import { useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { CopyPlus, Film, PlusCircle, Wrench } from 'lucide-react';
import adminRoomService from '../../services/adminRoomService';
import {
  getAuditoriumReadiness,
  getAuditoriumStatus,
  SCREEN_TYPE_LABELS,
  SOUND_TYPE_LABELS,
} from '../../utils/facilityPresentation';

export default function CinemaAuditoriumsTab({ cinema, triggerToast, onRefresh }) {
  const navigate = useNavigate();
  const { triggerConfirm } = useOutletContext() || {};
  const [busyRoom, setBusyRoom] = useState(null);
  const rooms = cinema?.activeAuditoriums || [];

  const cloneRoom = async (room) => {
    const confirmed = await triggerConfirm?.({
      title: `Tạo phòng mới từ “${room.name}”?`,
      message:
        'Phòng mới giữ nguyên định dạng và sơ đồ ghế, ở trạng thái đang thiết lập để bạn kiểm tra trước khi vận hành.',
      confirmLabel: 'Tạo phòng bản sao',
    });
    if (!confirmed) return;

    setBusyRoom(room.publicId);
    try {
      const created = await adminRoomService.createAuditorium(cinema.publicId, {
        name: `${room.name} - Bản sao`,
        screenType: room.screenType,
        soundType: room.soundType,
        capacity: 0,
        cleaningBufferMinutes: room.cleaningBufferMinutes || 15,
      });
      if (!created?.success || !created.data?.publicId) {
        throw new Error('Không thể tạo phòng mới');
      }
      await adminRoomService.cloneAuditoriumLayout(
        cinema.publicId,
        created.data.publicId,
        room.publicId,
      );
      triggerToast?.('Đã tạo phòng bản sao ở trạng thái đang thiết lập.');
      await onRefresh?.();
    } catch (error) {
      triggerToast?.(
        error.response?.data?.message || error.message || 'Không thể tạo phòng bản sao',
        'error',
      );
    } finally {
      setBusyRoom(null);
    }
  };

  return (
    <div className="space-y-6 pb-20">
      <section className="flex flex-col gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-5 md:flex-row md:items-center md:justify-between">
        <div>
          <h2 className="text-sm font-black uppercase tracking-wider text-white">Phòng chiếu</h2>
          <p className="mt-1 text-xs text-zinc-500">
            Theo dõi mức sẵn sàng, sơ đồ ghế và trạng thái vận hành của từng phòng.
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate(`/admin/rooms/create?cinemaId=${cinema.publicId}`)}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-4 py-3 text-xs font-black text-white"
        >
          <PlusCircle className="h-4 w-4" />
          Thiết lập phòng chiếu mới
        </button>
      </section>

      <div className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/20">
        {rooms.length === 0 ? (
          <div className="flex flex-col items-center px-6 py-16 text-center">
            <Film className="h-8 w-8 text-zinc-700" />
            <h3 className="mt-4 text-sm font-bold text-zinc-200">Chưa có phòng chiếu</h3>
            <p className="mt-2 text-xs text-zinc-500">
              Tạo phòng đầu tiên, thiết lập sơ đồ ghế rồi mới đưa vào vận hành.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-left">
              <thead className="border-b border-zinc-800 bg-zinc-950/50 text-[10px] uppercase tracking-wider text-zinc-500">
                <tr>
                  <th className="px-5 py-4">Phòng</th>
                  <th className="px-5 py-4">Định dạng</th>
                  <th className="px-5 py-4">Sơ đồ ghế</th>
                  <th className="px-5 py-4">Vận hành hiện tại</th>
                  <th className="px-5 py-4">Suất kế tiếp</th>
                  <th className="px-5 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/70 text-xs">
                {rooms.map((room) => {
                  const status = getAuditoriumStatus(room.status);
                  const readiness = getAuditoriumReadiness(room);
                  return (
                    <tr key={room.publicId} className="hover:bg-zinc-900/40">
                      <td className="px-5 py-5">
                        <p className="font-black text-zinc-100">{room.name}</p>
                        <p className="mt-1 text-zinc-500">{room.capacity || 0} vị trí ghế</p>
                      </td>
                      <td className="px-5 py-5 text-zinc-300">
                        <p>{SCREEN_TYPE_LABELS[room.screenType] || 'Tiêu chuẩn'}</p>
                        <p className="mt-1 text-zinc-500">
                          {SOUND_TYPE_LABELS[room.soundType] || 'Âm thanh tiêu chuẩn'}
                        </p>
                      </td>
                      <td className="px-5 py-5">
                        <span className={readiness.hasSeatLayout ? 'text-emerald-400' : 'text-amber-400'}>
                          {readiness.seatLayoutLabel}
                        </span>
                      </td>
                      <td className="px-5 py-5">
                        <span className={`inline-flex rounded-lg border px-2.5 py-1 font-bold ${status.className}`}>
                          {status.label}
                        </span>
                        {room.status !== 'ACTIVE' && (
                          <p className="mt-2 max-w-[220px] leading-5 text-zinc-500">
                            {status.description}
                          </p>
                        )}
                      </td>
                      <td className="px-5 py-5 text-zinc-500">
                        Chưa có dữ liệu lịch kế tiếp
                      </td>
                      <td className="px-5 py-5">
                        <div className="flex justify-end gap-2">
                          <button
                            type="button"
                            onClick={() => navigate(`/admin/rooms/edit/${room.publicId}`)}
                            className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 font-bold text-zinc-200 hover:border-orange-500/50"
                          >
                            <Wrench className="h-4 w-4" />
                            Mở trung tâm phòng
                          </button>
                          <button
                            type="button"
                            disabled={busyRoom === room.publicId || !readiness.hasSeatLayout}
                            onClick={() => cloneRoom(room)}
                            className="inline-flex items-center gap-2 rounded-xl border border-zinc-800 px-3 py-2 font-bold text-zinc-400 hover:text-white disabled:opacity-40"
                          >
                            <CopyPlus className="h-4 w-4" />
                            Nhân bản
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="rounded-2xl border border-sky-500/20 bg-sky-500/5 p-4 text-xs leading-5 text-zinc-400">
        Để tạm ngừng hoặc bảo trì phòng, chọn <strong className="text-zinc-200">Mở trung tâm phòng</strong>.
        Giao diện sẽ hướng dẫn chuyển trạng thái phù hợp và giữ nguyên dữ liệu lịch sử.
      </p>
    </div>
  );
}
