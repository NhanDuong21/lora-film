// eslint-disable-next-line no-unused-vars
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { PlusCircle, Search, Trash2, Edit, RefreshCw, Film } from 'lucide-react';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';

export default function AdminRoomPage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const navigate = useNavigate();

  const [cinemas, setCinemas] = useState([]);
  const [selectedCinemaId, setSelectedCinemaId] = useState('');
  const [rooms, setRooms] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Fetch cinemas list
  useEffect(() => {
    const fetchCinemas = async () => {
      try {
        const res = await adminCinemaService.getCinemas({ page: 0, size: 100 });
        if (res?.success && Array.isArray(res.data?.data)) {
          const list = res.data.data;
          setCinemas(list);
          if (list.length > 0) {
            setSelectedCinemaId(list[0].publicId);
          }
        }
      } catch (err) {
        console.error('Failed to load cinemas:', err);
        triggerToast?.('Không thể tải danh sách cụm rạp: ' + err.message, 'error');
      }
    };
    fetchCinemas();
  }, [triggerToast]);

  // Fetch rooms for selected cinema
  const fetchRooms = useCallback(async () => {
    if (!selectedCinemaId) return;
    setIsLoading(true);
    try {
      const res = await adminCinemaService.getAdminCinemaDetail(selectedCinemaId);
      if (res?.success && res.data) {
        // Since we modified backend to return all rooms in activeAuditoriums, this gets everything
        setRooms(res.data.activeAuditoriums || []);
      }
    } catch (err) {
      console.error('Failed to load rooms:', err);
      triggerToast?.('Không thể tải danh sách phòng chiếu: ' + err.message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [selectedCinemaId, triggerToast]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchRooms();
  }, [fetchRooms]);

  // Delete handler
  const handleDelete = async (publicId, name) => {
    const shouldDelete = triggerConfirm
      ? await triggerConfirm(`Bạn có chắc chắn muốn xóa phòng chiếu "${name}"? Thao tác này sẽ xóa vĩnh viễn phòng chiếu và không thể hoàn tác.`)
      : window.confirm(`Bạn có chắc chắn muốn xóa phòng chiếu "${name}"? Thao tác này sẽ xóa vĩnh viễn phòng chiếu và không thể hoàn tác.`);
      
    if (shouldDelete) {
      try {
        await adminRoomService.deleteAuditorium(publicId);
        triggerToast?.(`Đã xóa phòng chiếu "${name}" thành công!`);
        fetchRooms();
      } catch (err) {
        console.error('Failed to delete room:', err);
        triggerToast?.('Lỗi: ' + (err.response?.data?.message || err.message || 'Không thể xóa phòng chiếu do có ràng buộc dữ liệu'), 'error');
      }
    }
  };

  // Filter logic
  const filteredRooms = rooms.filter(room => {
    const matchesKeyword = room.name.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || room.status === statusFilter;
    return matchesKeyword && matchesStatus;
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-lg">Đang hoạt động</span>;
      case 'DRAFT':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-amber-500/10 border border-amber-500/30 text-amber-400 rounded-lg font-mono">Bản nháp (DRAFT)</span>;
      case 'MAINTENANCE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg">Bảo trì</span>;
      case 'INACTIVE':
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">Ngưng hoạt động</span>;
      default:
        return <span className="px-2.5 py-1 text-[10px] font-black uppercase tracking-wider bg-zinc-800 border border-zinc-700 text-zinc-400 rounded-lg">{status}</span>;
    }
  };

  return (
    <div className="flex flex-col flex-1 p-6 md:p-8 overflow-auto min-h-[400px] bg-zinc-950 text-white space-y-6 select-none font-sans">
      
      {/* Top Title Bar */}
      <div className="flex flex-col md:flex-row md:justify-between md:items-center border-b border-zinc-900 pb-4 gap-4">
        <div>
          <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-zinc-50">QUẢN LÝ PHÒNG CHIẾU</h1>
          <p className="text-xs text-zinc-400 mt-1 uppercase tracking-wider">Cấu hình thông tin, định dạng và sơ đồ ghế ngồi của phòng chiếu</p>
        </div>

        <button
          onClick={() => {
            if (!selectedCinemaId) {
              triggerToast?.('Vui lòng chọn cụm rạp trước!', 'error');
              return;
            }
            navigate(`/admin/rooms/create?cinemaId=${selectedCinemaId}`);
          }}
          className="flex items-center justify-center gap-2 bg-brand-orange hover:bg-opacity-95 text-white text-xs font-black py-2.5 px-4 rounded-xl uppercase tracking-wider transition-all self-start md:self-auto shadow-lg shadow-brand-orange/10 hover:shadow-brand-orange/20 border border-brand-orange/10"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm phòng chiếu mới</span>
        </button>
      </div>

      {/* Control Filters Bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-zinc-900/20 border border-zinc-900 p-5 rounded-2xl">
        {/* Cinema Selection */}
        <div className="flex flex-col gap-2">
          <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Cụm rạp chiếu</label>
          <select
            value={selectedCinemaId}
            onChange={(e) => setSelectedCinemaId(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none focus:border-brand-orange transition-colors cursor-pointer"
          >
            {cinemas.map(c => (
              <option key={c.publicId} value={c.publicId}>{c.name}</option>
            ))}
            {cinemas.length === 0 && (
              <option value="">Đang tải danh sách cụm rạp...</option>
            )}
          </select>
        </div>

        {/* Status Filter */}
        <div className="flex flex-col gap-2">
          <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Trạng thái phòng</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none focus:border-brand-orange transition-colors cursor-pointer"
          >
            <option value="ALL">TẤT CẢ TRẠNG THÁI</option>
            <option value="ACTIVE">ĐANG HOẠT ĐỘNG</option>
            <option value="DRAFT">BẢN NHÁP (DRAFT)</option>
            <option value="MAINTENANCE">BẢO TRÌ</option>
            <option value="INACTIVE">NGƯNG HOẠT ĐỘNG</option>
          </select>
        </div>

        {/* Search Input */}
        <div className="flex flex-col gap-2 md:col-span-2">
          <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">Tìm kiếm phòng</label>
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
            <input
              type="text"
              placeholder="Nhập tên phòng chiếu..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-xl pl-11 pr-4 py-2.5 text-xs font-semibold text-zinc-200 outline-none focus:border-brand-orange transition-colors"
            />
          </div>
        </div>
      </div>

      {/* Rooms Table / Grid */}
      <div className="bg-zinc-900/20 border border-zinc-900 rounded-3xl overflow-hidden shadow-2xl">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-4">
            <RefreshCw className="w-8 h-8 text-brand-orange animate-spin" />
            <span className="text-xs text-zinc-500 font-bold uppercase tracking-wider">Đang tải danh sách phòng...</span>
          </div>
        ) : filteredRooms.length > 0 ? (
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
                {filteredRooms.map((room) => (
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
                      <div className="flex justify-end gap-2">
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
            <p className="text-[10px] text-zinc-600 max-w-xs">Hãy thử thay đổi tiêu chí bộ lọc hoặc nhấn nút "Thêm phòng chiếu mới" để thiết lập.</p>
          </div>
        )}
      </div>
    </div>
  );
}
