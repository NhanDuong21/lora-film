import { useState, useMemo } from 'react';
import { Trophy, Tag, Ticket, PlusCircle, Eye, Edit3, Trash2, X, Search, Calendar, Gift } from 'lucide-react';

const DEFAULT_EVENTS = [
  {
    id: 'e1',
    title: 'Thứ Ba Vui Vẻ - Đồng Giá Vé 60K',
    type: 'PROMOTION',
    status: 'DANG_DIEN_RA',
    dateRange: '2026-05-01 - 2026-12-31',
    rewardDetails: 'Đồng giá vé 60K + Giảm 10% Combo Bắp Nước'
  },
  {
    id: 'e2',
    title: 'Thành Viên Vàng LoraFilm - Nhân Đôi Điểm Tích Lũy Suốt Tháng 6',
    type: 'MEMBER_DISCOUNT',
    status: 'DANG_DIEN_RA',
    dateRange: '2026-06-01 - 2026-06-30',
    rewardDetails: 'x2 điểm tích lũy thành viên khi đặt vé thành công'
  },
  {
    id: 'e3',
    title: 'Đặc Quyền Họp Báo Ra Mắt Phim John Wick: Ballerina',
    type: 'EVENT',
    status: 'SAP_DIEN_RA',
    dateRange: '2026-07-01 - 2026-07-15',
    rewardDetails: '50 vé mời tham gia thảm đỏ và giao lưu trực tiếp'
  }
];

export default function AdminEventView({ 
  events = [], 
  updateEventsState, 
  triggerToast 
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [previewEvent, setPreviewEvent] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Form states
  const [formTitle, setFormTitle] = useState('');
  const [formType, setFormType] = useState('PROMOTION');
  const [formStartDate, setFormStartDate] = useState('');
  const [formEndDate, setFormEndDate] = useState('');
  const [formReward, setFormReward] = useState('');

  const getStatus = (startDate, endDate) => {
    const now = new Date();
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    now.setHours(0, 0, 0, 0);
    start.setHours(0, 0, 0, 0);
    end.setHours(0, 0, 0, 0);
    
    if (now > end) {
      return 'DA_KET_THUC';
    } else if (now < start) {
      return 'SAP_DIEN_RA';
    } else {
      return 'DANG_DIEN_RA';
    }
  };

  const activeEventsList = useMemo(() => {
    const list = events && events.length > 0 ? events : DEFAULT_EVENTS;
    return list;
  }, [events]);

  const filteredEvents = useMemo(() => {
    return activeEventsList.filter(e => 
      e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.type.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (e.rewardDetails && e.rewardDetails.toLowerCase().includes(searchQuery.toLowerCase()))
    );
  }, [activeEventsList, searchQuery]);

  const metrics = useMemo(() => {
    let active = 0;
    activeEventsList.forEach(e => {
      if (e.status === 'DANG_DIEN_RA') {
        active++;
      }
    });
    return {
      activeEvents: active,
      totalPromos: activeEventsList.filter(e => e.type === 'PROMOTION').length,
      redeemedOffers: 2100
    };
  }, [activeEventsList]);

  const handleOpenAdd = () => {
    setSelectedEvent(null);
    setFormTitle('');
    setFormType('PROMOTION');
    setFormStartDate(new Date().toISOString().split('T')[0]);
    setFormEndDate(new Date(Date.now() + 14 * 86400000).toISOString().split('T')[0]);
    setFormReward('');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (event) => {
    setSelectedEvent(event);
    setFormTitle(event.title);
    setFormType(event.type);
    
    const dates = event.dateRange ? event.dateRange.split(' - ') : [];
    setFormStartDate(dates[0] || new Date().toISOString().split('T')[0]);
    setFormEndDate(dates[1] || new Date(Date.now() + 14 * 86400000).toISOString().split('T')[0]);
    
    setFormReward(event.rewardDetails || '');
    setIsModalOpen(true);
  };

  const handleSave = (e) => {
    e.preventDefault();
    if (!formTitle || !formStartDate || !formEndDate || !formReward) {
      triggerToast('Vui lòng điền đầy đủ các thông tin bắt buộc.', 'error');
      return;
    }

    const calculatedStatus = getStatus(formStartDate, formEndDate);

    const eventPayload = {
      title: formTitle,
      type: formType,
      dateRange: `${formStartDate} - ${formEndDate}`,
      rewardDetails: formReward,
      status: calculatedStatus
    };

    if (selectedEvent) {
      const updated = activeEventsList.map(ev => ev.id === selectedEvent.id ? { ...eventPayload, id: ev.id } : ev);
      if (updateEventsState) {
        updateEventsState(updated);
      } else {
        localStorage.setItem('lora_admin_events', JSON.stringify(updated));
      }
      triggerToast('Cập nhật sự kiện thành công!');
    } else {
      const newId = 'e_' + Date.now();
      const updated = [...activeEventsList, { ...eventPayload, id: newId }];
      if (updateEventsState) {
        updateEventsState(updated);
      } else {
        localStorage.setItem('lora_admin_events', JSON.stringify(updated));
      }
      triggerToast('Thêm sự kiện mới thành công!');
    }
    setIsModalOpen(false);
  };

  const handleDelete = (id) => {
    if (confirm('Bạn có chắc chắn muốn xóa sự kiện này?')) {
      const updated = activeEventsList.filter(ev => ev.id !== id);
      if (updateEventsState) {
        updateEventsState(updated);
      } else {
        localStorage.setItem('lora_admin_events', JSON.stringify(updated));
      }
      triggerToast('Đã xóa sự kiện thành công!');
    }
  };

  const getTypeLabel = (type) => {
    if (type === 'PROMOTION') return 'Khuyến mãi';
    if (type === 'EVENT') return 'Sự kiện';
    if (type === 'MEMBER_DISCOUNT') return 'Ưu đãi thành viên';
    return type;
  };

  return (
    <div className="space-y-6">
      
      {/* 3 Upper localized analytical metrics cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              SỰ KIỆN HOẠT ĐỘNG
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.activeEvents} Sự kiện</span>
          </div>
          <div className="p-3 bg-brand-coral/10 border border-brand-coral/20 rounded-xl">
            <Trophy className="w-5 h-5 text-brand-coral" />
          </div>
        </div>

        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              CHIẾN DỊCH ƯU ĐÃI
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.totalPromos} Khuyến mãi</span>
          </div>
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
            <Tag className="w-5 h-5 text-amber-500" />
          </div>
        </div>

        <div className="bg-zinc-900/60 backdrop-blur-md border border-zinc-800/80 rounded-2xl p-5 flex items-center justify-between shadow-xl shadow-black/40 hover:border-zinc-700/60 transition-all duration-300">
          <div>
            <span className="text-zinc-400 text-xs font-black uppercase tracking-wider block mb-1">
              LƯỢT QUY ĐỔI VOUCHER
            </span>
            <span className="text-2xl font-bold text-amber-400">{metrics.redeemedOffers.toLocaleString('vi-VN')} Lượt dùng</span>
          </div>
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
            <Ticket className="w-5 h-5 text-emerald-500" />
          </div>
        </div>
      </div>

      {/* Action Strip */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-4 border-t border-zinc-900">
        <div className="relative w-full sm:w-80">
          <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-zinc-500">
            <Search className="w-4 h-4" />
          </span>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm kiếm sự kiện..."
            className="w-full bg-zinc-950 border border-zinc-900 rounded-xl py-2.5 pl-9 pr-4 text-xs text-zinc-100 focus:outline-none focus:border-brand-coral transition-colors"
          />
        </div>
        <button
          onClick={handleOpenAdd}
          className="flex items-center gap-2 bg-brand-coral hover:bg-opacity-90 text-white text-xs font-black py-2.5 px-4 rounded-xl transition-all w-full sm:w-auto justify-center"
        >
          <PlusCircle className="w-4.5 h-4.5" />
          <span>THÊM SỰ KIỆN MỚI</span>
        </button>
      </div>

      {/* Grid Data Sheet */}
      <div className="bg-zinc-900/40 backdrop-blur-md border border-zinc-900 rounded-2xl shadow-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-zinc-400">
            <thead className="bg-zinc-950/80 text-zinc-500 font-black uppercase tracking-wider border-b border-zinc-900">
              <tr>
                <th className="py-4 px-6 text-center w-20">ID</th>
                <th className="py-4 px-6">Tên Sự Kiện</th>
                <th className="py-4 px-6">Phân Loại</th>
                <th className="py-4 px-6">Trạng Thái</th>
                <th className="py-4 px-6">Thời Gian Diễn Ra</th>
                <th className="py-4 px-6">Ưu Đãi Đi Kèm</th>
                <th className="py-4 px-6 text-center w-36">Hành Động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-900/60">
              {filteredEvents.length === 0 ? (
                <tr>
                  <td colSpan="7" className="py-12 text-center text-zinc-500 font-semibold">
                    Không tìm thấy sự kiện nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredEvents.map((event) => {
                  return (
                    <tr key={event.id} className="hover:bg-zinc-900/20 transition-colors">
                      <td className="py-4 px-6 text-center text-zinc-400 font-mono font-medium">{event.id}</td>
                      <td className="py-4 px-6 font-bold text-zinc-100">{event.title}</td>
                      <td className="py-4 px-6">
                        <span className="text-zinc-300 bg-zinc-950/60 px-2.5 py-1 rounded-xl font-bold border border-zinc-900">
                          {getTypeLabel(event.type)}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        {event.status === 'DANG_DIEN_RA' && (
                          <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-black px-2.5 py-0.5 rounded-full inline-block">
                            Đang diễn ra
                          </span>
                        )}
                        {event.status === 'SAP_DIEN_RA' && (
                          <span className="bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-black px-2.5 py-0.5 rounded-full inline-block">
                            Sắp diễn ra
                          </span>
                        )}
                        {event.status === 'DA_KET_THUC' && (
                          <span className="bg-zinc-850 text-zinc-400 border border-zinc-900 text-[10px] font-black px-2.5 py-0.5 rounded-full inline-block">
                            Đã kết thúc
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-zinc-300 font-medium">
                        {event.dateRange}
                      </td>
                      <td className="py-4 px-6 text-zinc-300 font-medium max-w-xs truncate">{event.rewardDetails}</td>
                      <td className="py-4 px-6">
                        <div className="flex justify-center gap-2">
                          <button
                            onClick={() => setPreviewEvent(event)}
                            className="p-2 text-zinc-400 hover:text-white bg-zinc-950 border border-zinc-900 rounded-xl transition-all"
                            title="Xem chi tiết"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleOpenEdit(event)}
                            className="p-2 text-zinc-400 hover:text-white bg-zinc-950 border border-zinc-900 rounded-xl transition-all"
                            title="Sửa sự kiện"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                          <button
                            onClick={() => handleDelete(event.id)}
                            className="p-2 text-red-400 hover:text-red-300 bg-red-950/20 border border-red-900/40 rounded-xl transition-all"
                            title="Xóa sự kiện"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Add/Edit */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-zinc-950 border border-zinc-900 w-full max-w-lg rounded-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
            <div className="px-6 py-4 border-b border-zinc-900 flex items-center justify-between bg-zinc-950/40">
              <h3 className="text-sm font-black uppercase tracking-wider text-zinc-100 flex items-center gap-2">
                <Gift className="w-4 h-4 text-brand-coral" />
                {selectedEvent ? 'CẬP NHẬT SỰ KIỆN' : 'THÊM SỰ KIỆN MỚI'}
              </h3>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="text-zinc-500 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSave} className="p-6 space-y-4 overflow-y-auto">
              <div>
                <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5 font-bold">Tên Sự Kiện *</label>
                <input
                  type="text"
                  required
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="Ví dụ: Thứ Ba Vui Vẻ - Đồng Giá Vé 60K"
                  className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5 font-bold">Loại Sự Kiện</label>
                  <select
                    value={formType}
                    onChange={(e) => setFormType(e.target.value)}
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  >
                    <option value="PROMOTION">Khuyến mãi</option>
                    <option value="EVENT">Sự kiện</option>
                    <option value="MEMBER_DISCOUNT">Ưu đãi thành viên</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5 font-bold">Ưu Đãi Đi Kèm *</label>
                  <input
                    type="text"
                    required
                    value={formReward}
                    onChange={(e) => setFormReward(e.target.value)}
                    placeholder="Ví dụ: Đồng giá vé 60K"
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5 font-bold">Ngày Bắt Đầu *</label>
                  <input
                    type="date"
                    required
                    value={formStartDate}
                    onChange={(e) => setFormStartDate(e.target.value)}
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-black uppercase text-zinc-500 mb-1.5 font-bold">Ngày Kết Thúc *</label>
                  <input
                    type="date"
                    required
                    value={formEndDate}
                    onChange={(e) => setFormEndDate(e.target.value)}
                    className="w-full bg-zinc-900/40 border border-zinc-900 rounded-xl px-3 py-2 text-xs text-zinc-100 outline-none focus:border-brand-coral transition-colors"
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-zinc-900 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 border border-zinc-900 hover:bg-zinc-900 rounded-xl text-xs font-semibold transition-colors"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-brand-coral hover:bg-opacity-90 text-white rounded-xl text-xs font-black transition-colors shadow-lg shadow-brand-coral/10"
                >
                  Lưu sự kiện
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {previewEvent && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-zinc-950 border border-zinc-900 w-full max-w-md rounded-2xl overflow-hidden shadow-2xl flex flex-col animate-fade-in">
            <div className="px-6 py-4 border-b border-zinc-900 flex items-center justify-between bg-zinc-950/40">
              <h3 className="text-xs font-black uppercase tracking-wider text-zinc-100">
                Chi Tiết Sự Kiện
              </h3>
              <button 
                onClick={() => setPreviewEvent(null)}
                className="text-zinc-500 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div>
                <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-black block mb-0.5">Tên sự kiện</span>
                <p className="text-sm font-bold text-zinc-100">{previewEvent.title}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-black block mb-0.5">Phân loại</span>
                  <p className="text-xs text-zinc-300 font-bold">{getTypeLabel(previewEvent.type)}</p>
                </div>
                <div>
                  <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-black block mb-0.5">Thời gian</span>
                  <p className="text-xs text-zinc-300 font-medium flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-zinc-500" />
                    {previewEvent.dateRange}
                  </p>
                </div>
              </div>

              <div>
                <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-black block mb-0.5">Ưu đãi đi kèm</span>
                <p className="text-xs text-brand-coral font-bold">{previewEvent.rewardDetails}</p>
              </div>
            </div>

            <div className="px-6 py-4 bg-zinc-950/20 border-t border-zinc-900 flex justify-end">
              <button
                onClick={() => setPreviewEvent(null)}
                className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl text-xs font-semibold transition-colors"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
