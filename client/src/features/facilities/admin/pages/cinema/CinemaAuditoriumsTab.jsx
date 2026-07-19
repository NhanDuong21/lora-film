import { useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { PlusCircle, Edit, Trash2, Film, Copy } from 'lucide-react';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';

export default function CinemaAuditoriumsTab({ cinema, triggerToast }) {
  const { triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();
  const auditoriums = cinema?.activeAuditoriums || [];
  
  const [cloningRoomId, setCloningRoomId] = useState(null);
  const [sourceRoomId, setSourceRoomId] = useState('');
  const [isCloning, setIsCloning] = useState(false);

  const handleDelete = async (publicId, name) => {
    const shouldDelete = triggerConfirm
      ? await triggerConfirm(`Bạn có chắc chắn muốn xóa phòng chiếu "${name}"? Thao tác này sẽ xóa vĩnh viễn phòng chiếu và không thể hoàn tác.`)
      : window.confirm(`Bạn có chắc chắn muốn xóa phòng chiếu "${name}"? Thao tác này sẽ xóa vĩnh viễn phòng chiếu và không thể hoàn tác.`);
      
    if (shouldDelete) {
      try {
        await adminRoomService.deleteAuditorium(publicId);
        triggerToast?.(`Đã xóa phòng chiếu "${name}" thành công!`);
        // Refresh by reloading the page or ideally triggering a fetch in the parent component
        window.location.reload(); 
      } catch (err) {
        console.error('Failed to delete room:', err);
        triggerToast?.('Lỗi: ' + (err.response?.data?.message || err.message || 'Không thể xóa phòng chiếu do có ràng buộc dữ liệu'), 'error');
      }
    }
  };

  const handleClone = async (targetPublicId, targetName, targetCapacity) => {
    if (!sourceRoomId) {
      triggerToast?.('Vui lòng chọn phòng chiếu nguồn để nhân bản', 'warning');
      return;
    }
    
    if (targetCapacity > 0) {
      const shouldOverwrite = triggerConfirm
        ? await triggerConfirm(`Phòng chiếu "${targetName}" đang có sẵn ${targetCapacity} ghế. Nếu bạn tiếp tục, sơ đồ hiện tại sẽ bị xóa đè hoàn toàn bởi sơ đồ phòng mẫu. Bạn có chắc chắn?`)
        : window.confirm(`Phòng chiếu "${targetName}" đang có sẵn ${targetCapacity} ghế. Nếu bạn tiếp tục, sơ đồ hiện tại sẽ bị xóa đè hoàn toàn bởi sơ đồ phòng mẫu. Bạn có chắc chắn?`);
        
      if (!shouldOverwrite) {
        return;
      }
    }

    setIsCloning(true);
    try {
      await adminRoomService.cloneAuditoriumLayout(cinema.publicId, targetPublicId, sourceRoomId);
      triggerToast?.('Nhân bản sơ đồ phòng chiếu thành công!');
      window.location.reload();
    } catch (err) {
      console.error('Failed to clone room:', err);
      triggerToast?.('Lỗi: ' + (err.response?.data?.message || err.message || 'Không thể nhân bản sơ đồ'), 'error');
    } finally {
      setIsCloning(false);
      setCloningRoomId(null);
      setSourceRoomId('');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-lg">Đang hoạt động</span>;
      case 'DRAFT':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-amber-500/10 border border-amber-500/30 text-amber-400 rounded-lg">Bản nháp (DRAFT)</span>;
      case 'MAINTENANCE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg">Bảo trì</span>;
      case 'INACTIVE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">Ngưng hoạt động</span>;
      default:
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">{status}</span>;
    }
  };

  return (
    <div className="space-y-6 pb-20">
      <div className="flex justify-between items-center bg-zinc-900/30 border border-zinc-800 p-5 rounded-2xl">
        <div>
          <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">PHÒNG CHIẾU THUỘC RẠP</h2>
          <p className="text-xs text-zinc-500 mt-1">Danh sách các phòng chiếu hiện có tại rạp {cinema?.name}.</p>
        </div>
        <button
          onClick={() => navigate(`/admin/rooms/create?cinemaId=${cinema?.publicId}`)}
          className="flex items-center gap-2 bg-brand-orange hover:bg-opacity-95 text-white px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all shadow-lg shadow-brand-orange/20"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm Phòng Chiếu Mới</span>
        </button>
      </div>

      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl">
        {auditoriums.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-zinc-900 text-[10px] font-black text-zinc-500 uppercase tracking-wider select-none bg-zinc-950/40">
                  <th className="py-4 px-6">Phòng chiếu</th>
                  <th className="py-4 px-6">Màn hình</th>
                  <th className="py-4 px-6">Âm thanh</th>
                  <th className="py-4 px-6 text-center">Sức chứa</th>
                  <th className="py-4 px-6">Trạng thái</th>
                  <th className="py-4 px-6 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-900/60 text-xs font-semibold">
                {auditoriums.map((room) => (
                  <tr key={room.publicId} className="hover:bg-zinc-900/10 transition-colors">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-brand-orange/10 flex items-center justify-center border border-brand-orange/20">
                          <Film className="w-4 h-4 text-brand-orange" />
                        </div>
                        <span className="font-extrabold text-zinc-200">{room.name}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-zinc-400 font-mono">{room.screenType || 'STANDARD'}</td>
                    <td className="py-4 px-6 text-zinc-400 font-mono">{room.soundType || 'STANDARD'}</td>
                    <td className="py-4 px-6 text-center text-amber-500 font-bold font-mono">{room.capacity || 0} ghế</td>
                    <td className="py-4 px-6">{getStatusBadge(room.status || 'ACTIVE')}</td>
                    <td className="py-4 px-6 text-right">
                      <div className="flex justify-end gap-2 items-center">
                        {room.status === 'DRAFT' && (
                          <div className="relative flex items-center">
                            {cloningRoomId === room.publicId ? (
                              <div className="flex items-center gap-1 bg-zinc-900 border border-zinc-700 p-1 rounded-xl">
                                <select 
                                  value={sourceRoomId}
                                  onChange={(e) => setSourceRoomId(e.target.value)}
                                  className="bg-zinc-950 text-xs text-zinc-300 border-none outline-none py-1 pl-2 pr-6 rounded-lg appearance-none cursor-pointer"
                                >
                                  <option value="">-- Chọn phòng mẫu --</option>
                                  {auditoriums.filter(r => r.publicId !== room.publicId && r.capacity > 0).map(r => (
                                    <option key={r.publicId} value={r.publicId}>{r.name} ({r.capacity} ghế)</option>
                                  ))}
                                </select>
                                <button 
                                  onClick={() => handleClone(room.publicId, room.name, room.capacity)}
                                  disabled={isCloning || !sourceRoomId}
                                  className="p-1.5 bg-brand-orange text-white rounded-lg hover:bg-opacity-90 disabled:opacity-50"
                                >
                                  <Copy className="w-3.5 h-3.5" />
                                </button>
                                <button 
                                  onClick={() => setCloningRoomId(null)}
                                  className="p-1.5 text-zinc-500 hover:text-zinc-300 rounded-lg"
                                >
                                  ✕
                                </button>
                              </div>
                            ) : (
                              <button
                                onClick={() => setCloningRoomId(room.publicId)}
                                className="p-2 bg-zinc-950 border border-zinc-800 hover:border-brand-orange/50 text-zinc-400 hover:text-brand-orange rounded-xl transition-all mr-1"
                                title="Nhân bản sơ đồ từ phòng khác"
                              >
                                <Copy className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        )}
                        <button
                          onClick={() => navigate(`/admin/rooms/edit/${room.publicId}`)}
                          className="p-2 bg-zinc-950 border border-zinc-800 hover:border-amber-500/50 text-zinc-400 hover:text-amber-500 rounded-xl transition-all"
                          title="Sửa phòng chiếu & sơ đồ ghế"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(room.publicId, room.name)}
                          className="p-2 bg-zinc-950 border border-zinc-800 hover:border-red-500/40 text-zinc-400 hover:text-red-400 rounded-xl transition-all"
                          title="Xóa phòng"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-850 flex items-center justify-center">
              <Film className="w-6 h-6 text-zinc-650" />
            </div>
            <div className="text-xs font-bold text-zinc-500 uppercase tracking-widest">Không tìm thấy phòng chiếu nào</div>
            <p className="text-[10px] text-zinc-600 max-w-xs mb-4">Nhấn nút bên dưới để cấu hình phòng chiếu đầu tiên cho rạp này.</p>
            <button
              onClick={() => navigate(`/admin/rooms/create?cinemaId=${cinema?.publicId}`)}
              className="flex items-center gap-2 bg-brand-orange hover:bg-opacity-95 text-white px-6 py-3 rounded-xl text-xs font-bold uppercase tracking-wider transition-all shadow-lg shadow-brand-orange/20 mt-4"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Tạo Phòng Chiếu Đầu Tiên</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
