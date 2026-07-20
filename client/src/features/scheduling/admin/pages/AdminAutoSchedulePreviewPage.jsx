import { useState, useMemo } from 'react';
import { useParams, useOutletContext, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Loader2, Calendar, MapPin, CheckCircle2, XCircle, Clock, AlertTriangle, AlertCircle, Info, RefreshCw } from 'lucide-react';
import useAutoSchedulePreview from '@/features/scheduling/admin/hooks/useAutoSchedulePreview';

const formatTime = (isoString) => {
  if (!isoString) return '';
  const d = new Date(isoString);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
};

const checkOverlap = (item1, item2) => {
  const start1 = new Date(item1.startTime).getTime();
  const end1 = new Date(item1.endTime).getTime();
  const start2 = new Date(item2.startTime).getTime();
  const end2 = new Date(item2.endTime).getTime();
  return start1 < end2 && start2 < end1;
};

const REJECTION_REASON_MAP = {
  "SHOWTIME_OUTSIDE_OPERATING_HOURS": "Ngoài giờ hoạt động của cụm rạp",
  "SHOWTIME_OVERLAPS_EXISTING": "Trùng với suất chiếu hiện có",
  "MOVIE_NOT_ELIGIBLE": "Phim chưa đủ điều kiện",
  "AUDITORIUM_UNAVAILABLE": "Phòng chiếu không khả dụng",
  "NOT_ENOUGH_CLEANING_TIME": "Không đủ thời gian dọn dẹp"
};

const translateReason = (reason) => {
  if (!reason) return '';
  for (const [key, value] of Object.entries(REJECTION_REASON_MAP)) {
    if (reason.toUpperCase().includes(key)) return value;
  }
  return reason;
};

const AdminAutoSchedulePreviewPage = () => {
  const { id } = useParams();
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/admin/showtimes', {
      state: {
        status: 'DRAFT',
        message: `Đã tạo ${selectedItemIds.size} suất chiếu ở trạng thái DRAFT.`
      }
    });
  };

  const {
    preview, items,
    isLoading, isApplying, isUpdatingSelection,
    selectedItemIds, handleToggleSelection,
    handleApply, fetchPreview
  } = useAutoSchedulePreview(id, { triggerToast, onSuccess: handleSuccess });

  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterAuditorium, setFilterAuditorium] = useState('');
  const [filterReason, setFilterReason] = useState('');
  const [filterDate, setFilterDate] = useState('');
  const [showApplyModal, setShowApplyModal] = useState(false);

  const rejectionReasons = useMemo(() => {
    if (!items) return {};
    const reasons = {};
    items.forEach(item => {
      if (item.validationStatus !== 'VALID' && item.rejectionReason) {
        reasons[item.rejectionReason] = (reasons[item.rejectionReason] || 0) + 1;
      }
    });
    return reasons;
  }, [items]);

  const uniqueAuditoriums = useMemo(() => {
    if (!items) return [];
    const auds = new Set();
    items.forEach(item => auds.add(item.auditoriumName || item.auditoriumPublicId));
    return Array.from(auds).sort();
  }, [items]);

  const uniqueDates = useMemo(() => {
    if (!items) return [];
    const dates = new Set();
    items.forEach(item => {
      const d = new Date(item.startTime);
      dates.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
    });
    return Array.from(dates).sort();
  }, [items]);

  const filteredItems = useMemo(() => {
    if (!items) return [];
    return items.filter(item => {
      if (filterStatus === 'VALID' && item.validationStatus !== 'VALID') return false;
      if (filterStatus === 'INVALID' && item.validationStatus === 'VALID') return false;
      const audKey = item.auditoriumName || item.auditoriumPublicId;
      if (filterAuditorium && audKey !== filterAuditorium) return false;
      if (filterReason && item.rejectionReason !== filterReason) return false;
      if (filterDate) {
        const d = new Date(item.startTime);
        const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        if (dateKey !== filterDate) return false;
      }
      return true;
    });
  }, [items, filterStatus, filterAuditorium, filterReason, filterDate]);

  const groupedFilteredItems = useMemo(() => {
    const groups = {};
    filteredItems.forEach(item => {
      const d = new Date(item.startTime);
      const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      if (!groups[dateKey]) groups[dateKey] = {};
      const audKey = item.auditoriumName || item.auditoriumPublicId;
      if (!groups[dateKey][audKey]) groups[dateKey][audKey] = [];
      groups[dateKey][audKey].push(item);
    });
    Object.keys(groups).forEach(dateKey => {
      Object.keys(groups[dateKey]).forEach(audKey => {
        groups[dateKey][audKey].sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
      });
    });
    return groups;
  }, [filteredItems]);

  if (isLoading) {
    return (
      <div className="flex flex-col flex-1 items-center justify-center p-8 bg-zinc-950 text-zinc-400">
        <Loader2 className="w-8 h-8 animate-spin text-brand-orange mb-4" />
        <p>Đang tải chi tiết bản xem trước...</p>
      </div>
    );
  }

  if (!preview) {
    return (
      <div className="flex flex-col flex-1 items-center justify-center p-8 bg-zinc-950 text-zinc-400">
        <AlertTriangle className="w-12 h-12 text-red-500 mb-4 opacity-80" />
        <h2 className="text-xl font-bold text-white mb-2">Không tìm thấy</h2>
        <p>Bản xem trước xếp lịch không tồn tại hoặc đã hết hạn.</p>
        <button onClick={() => navigate(-1)} className="mt-6 text-brand-orange hover:underline text-sm">Quay lại</button>
      </div>
    );
  }

  const isExpired = preview.status === 'EXPIRED' || new Date(preview.expiresAt) < new Date();
  const isApplied = preview.status === 'APPLIED';
  const canApply = preview.status === 'PREVIEWED' && !isExpired;

  return (
    <div className="flex flex-col flex-1 bg-zinc-950 text-white min-h-[400px] animate-fade-in">
      
      {/* Sticky Header */}
      <div className="sticky top-0 z-20 bg-zinc-950/80 backdrop-blur-md border-b border-zinc-800 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400 hover:text-white flex-shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-xl md:text-2xl font-black uppercase tracking-wider text-white">
                BẢN XEM TRƯỚC LỊCH CHIẾU
              </h1>
              <span className={`px-2 py-0.5 text-[10px] font-black uppercase tracking-wider rounded border ${
                isApplied ? 'bg-green-500/10 text-green-400 border-green-500/20' : 
                isExpired ? 'bg-red-500/10 text-red-400 border-red-500/20' : 
                'bg-blue-500/10 text-blue-400 border-blue-500/20'
              }`}>
                {isExpired && !isApplied ? 'EXPIRED' : preview.status}
              </span>
            </div>
            <div className="text-zinc-400 text-sm mt-1.5 flex flex-wrap items-center gap-4">
              <span className="flex items-center gap-1.5"><MapPin className="w-3.5 h-3.5" /> {preview.cinemaName}</span>
              <span className="flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5" /> {formatDate(preview.scheduleFrom)} - {formatDate(preview.scheduleTo)}</span>
            </div>
          </div>
        </div>
        
        <div className="flex items-center gap-3">
          <button
            onClick={fetchPreview}
            disabled={isUpdatingSelection || isApplying}
            className="p-2.5 rounded-xl border border-zinc-800 hover:bg-zinc-800 text-zinc-400 transition-colors disabled:opacity-50"
            title="Làm mới"
          >
            <RefreshCw className={`w-4 h-4 ${isUpdatingSelection ? 'animate-spin' : ''}`} />
          </button>

          {canApply && (
            <button
              onClick={() => setShowApplyModal(true)}
              disabled={isApplying || isUpdatingSelection || selectedItemIds.size === 0}
              className="bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black px-6 py-2.5 rounded-xl text-xs uppercase tracking-wider transition-all duration-300 shadow-lg shadow-brand-orange/10 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isApplying ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              <span>ÁP DỤNG LỊCH CHIẾU ({selectedItemIds.size})</span>
            </button>
          )}
        </div>
      </div>

      <div className="p-6 md:p-8 space-y-8 max-w-[1600px] mx-auto w-full">
        
        {/* Warning Banner if Expired */}
        {isExpired && !isApplied && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="font-bold text-sm">Bản xem trước đã hết hạn</h3>
              <p className="text-xs mt-1 opacity-80">Bạn không thể áp dụng bản xem trước này nữa vì nó đã vượt quá thời gian tồn tại (TTL). Vui lòng tạo bản xem trước mới.</p>
            </div>
          </div>
        )}

        {/* Stats Row */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4">
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-1 block">Tổng ứng viên</span>
            <span className="text-2xl font-black text-white">{preview.totalCandidateCount}</span>
          </div>
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4">
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-1 block">Hợp lệ</span>
            <span className="text-2xl font-black text-green-400">{preview.validCandidateCount}</span>
          </div>
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4">
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-1 block">Xung đột / Từ chối</span>
            <span className="text-2xl font-black text-red-400">{preview.rejectedCandidateCount}</span>
          </div>
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-4">
            <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-1 block">Hết hạn vào</span>
            <span className="text-sm font-bold text-amber-400 mt-2 block flex items-center gap-1.5">
              <Clock className="w-4 h-4" />
              {new Date(preview.expiresAt).toLocaleString('vi-VN')}
            </span>
          </div>
        </div>

        {/* Breakdown of Rejection Reasons */}
        {Object.keys(rejectionReasons).length > 0 && (
          <div className="bg-zinc-900/40 border border-zinc-800 rounded-2xl p-5">
            <h3 className="text-xs font-bold text-zinc-400 uppercase tracking-wider mb-3 flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-red-500" /> Lý do từ chối (Tổng cộng: {preview.rejectedCandidateCount})
            </h3>
            <div className="flex flex-wrap gap-3">
              {Object.entries(rejectionReasons).map(([reason, count]) => (
                <div key={reason} className="bg-zinc-950 border border-zinc-800 px-3 py-2 rounded-xl flex items-center gap-2">
                  <span className="text-red-400 font-black text-sm">{count}</span>
                  <span className="text-xs text-zinc-300">{translateReason(reason)}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="bg-zinc-900/60 border border-zinc-800 p-4 rounded-2xl flex flex-wrap gap-4 items-center">
          <select 
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-xs transition-colors focus:outline-none"
          >
            <option value="ALL">Tất cả ứng viên</option>
            <option value="VALID">Chỉ Hợp lệ</option>
            <option value="INVALID">Chỉ Bị từ chối</option>
          </select>
          
          <select 
            value={filterAuditorium}
            onChange={(e) => setFilterAuditorium(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-xs transition-colors focus:outline-none"
          >
            <option value="">Tất cả Phòng chiếu</option>
            {uniqueAuditoriums.map(aud => (
              <option key={aud} value={aud}>{aud}</option>
            ))}
          </select>

          <select 
            value={filterDate}
            onChange={(e) => setFilterDate(e.target.value)}
            className="bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-xs transition-colors focus:outline-none"
          >
            <option value="">Tất cả Ngày</option>
            {uniqueDates.map(dateKey => (
              <option key={dateKey} value={dateKey}>{formatDate(dateKey)}</option>
            ))}
          </select>

            <select 
              value={filterReason}
              onChange={(e) => setFilterReason(e.target.value)}
              className="bg-zinc-950 border border-zinc-800 text-zinc-300 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-xs transition-colors focus:outline-none"
            >
              <option value="">Tất cả Lý do</option>
              {Object.keys(rejectionReasons).map(reason => (
                <option key={reason} value={reason}>{translateReason(reason)}</option>
              ))}
            </select>
        </div>

        {/* Timeline View */}
        {/* Timeline View */}
        <div className="space-y-10">
          {Object.keys(groupedFilteredItems).sort().map(dateKey => (
            <div key={dateKey} className="space-y-4">
              <h2 className="text-lg font-black text-white flex items-center gap-3 border-b border-zinc-800 pb-2">
                <Calendar className="w-5 h-5 text-brand-orange" />
                {formatDate(dateKey)}
              </h2>

              <div className="space-y-6">
                {Object.keys(groupedFilteredItems[dateKey]).sort().map(audKey => {
                  const audItems = groupedFilteredItems[dateKey][audKey];
                  
                  return (
                    <div key={audKey} className="bg-zinc-900/40 border border-zinc-800/80 rounded-2xl overflow-hidden">
                      <div className="bg-zinc-900 px-5 py-3 border-b border-zinc-800 flex items-center justify-between">
                        <h3 className="font-bold text-sm text-zinc-200">{audKey}</h3>
                        <span className="text-xs text-zinc-500">{audItems.length} suất chiếu</span>
                      </div>
                      
                      <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse whitespace-nowrap">
                          <thead>
                            <tr className="bg-zinc-950/50 border-b border-zinc-800/80 text-[10px] font-black text-zinc-400 uppercase tracking-wider">
                              <th className="py-3 px-5 w-10 text-center">
                                {/* Removed select-all to prevent overlapping selection errors */}
                              </th>
                              <th className="py-3 px-5">THỜI GIAN</th>
                              <th className="py-3 px-5">PHIM & ĐỊNH DẠNG</th>
                              <th className="py-3 px-5">TRẠNG THÁI</th>
                              <th className="py-3 px-5">CHI TIẾT</th>
                            </tr>
                          </thead>
                          <tbody>
                            {audItems.map(item => {
                              const isValid = item.validationStatus === 'VALID';
                              const isSelected = selectedItemIds.has(item.itemPublicId);
                              const isItemApplied = item.applyStatus === 'APPLIED';
                              
                              let isConflicting = false;
                              let conflictItem = null;
                              
                              if (isValid && !isSelected && !isItemApplied) {
                                for (const selectedId of selectedItemIds) {
                                  const selectedObj = audItems.find(i => i.itemPublicId === selectedId);
                                  if (selectedObj && checkOverlap(item, selectedObj)) {
                                    isConflicting = true;
                                    conflictItem = selectedObj;
                                    break;
                                  }
                                }
                              }

                              const isCheckboxDisabled = isUpdatingSelection || isApplying || isConflicting;
                              
                              return (
                                <tr 
                                  key={item.itemPublicId}
                                  className={`border-b border-zinc-800/50 hover:bg-zinc-800/30 transition-colors ${
                                    isItemApplied ? 'bg-green-500/5' :
                                    !isValid ? 'bg-red-500/5' :
                                    isConflicting ? 'bg-red-500/10 opacity-75 grayscale' :
                                    isSelected ? 'bg-brand-orange/5' : 'bg-zinc-950'
                                  }`}
                                >
                                  <td className="py-3 px-5 text-center">
                                    {isValid && !isItemApplied && canApply && (
                                      <input 
                                        type="checkbox"
                                        checked={isSelected}
                                        onChange={() => handleToggleSelection(item.itemPublicId, isSelected)}
                                        disabled={isCheckboxDisabled}
                                        className="w-4 h-4 rounded border-zinc-700 text-brand-orange focus:ring-brand-orange bg-zinc-900 cursor-pointer disabled:cursor-not-allowed"
                                      />
                                    )}
                                  </td>
                                  <td className="py-3 px-5">
                                    <div className="flex items-center gap-2">
                                      <span className="font-bold text-zinc-200">{formatTime(item.startTime)}</span>
                                      <span className="text-zinc-500">-</span>
                                      <span className="font-bold text-zinc-400">{formatTime(item.endTime)}</span>
                                    </div>
                                  </td>
                                  <td className="py-3 px-5">
                                    <div className="flex flex-col">
                                      <span className="font-bold text-sm text-white">{item.movieTitle}</span>
                                      <span className="text-xs text-zinc-400">{item.versionName} • {item.format} • {item.audioLanguage}</span>
                                    </div>
                                  </td>
                                  <td className="py-3 px-5">
                                    {isItemApplied ? (
                                      <span className="bg-green-500/10 text-green-400 border border-green-500/20 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider flex items-center gap-1 w-max">
                                        <CheckCircle2 className="w-3 h-3" /> APPLIED
                                      </span>
                                    ) : isConflicting ? (
                                      <span className="bg-red-500/10 text-red-400 border border-red-500/20 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider w-max block">
                                        XUNG ĐỘT
                                      </span>
                                    ) : isValid ? (
                                      <span className="bg-blue-500/10 text-blue-400 border border-blue-500/20 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider w-max block">
                                        HỢP LỆ
                                      </span>
                                    ) : (
                                      <span className="bg-red-500/10 text-red-400 border border-red-500/20 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider w-max block">
                                        TỪ CHỐI
                                      </span>
                                    )}
                                  </td>
                                  <td className="py-3 px-5 whitespace-normal min-w-[200px]">
                                    {!isValid && (
                                      <div className="text-xs text-red-400 flex items-start gap-1">
                                        <Info className="w-3.5 h-3.5 flex-shrink-0 mt-0.5" />
                                        <span>{translateReason(item.rejectionReason)}</span>
                                      </div>
                                    )}
                                    {isItemApplied && (
                                      <div className="text-xs text-green-400">
                                        Đã được tạo thành lịch chiếu chính thức.
                                      </div>
                                    )}
                                    {isConflicting && (
                                      <div className="text-xs text-red-400 font-bold">
                                        Xung đột với suất đã chọn: {formatTime(conflictItem.startTime)}-{formatTime(conflictItem.endTime)}
                                      </div>
                                    )}
                                    {isValid && !isItemApplied && !isConflicting && (
                                      <div className="text-xs text-zinc-500">
                                        Đủ điều kiện. {isSelected ? 'Sẽ được áp dụng.' : 'Chưa được chọn.'}
                                      </div>
                                    )}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}

          {Object.keys(groupedFilteredItems).length === 0 && (
            <div className="text-center py-20 text-zinc-500">
              <p>Không có ứng viên suất chiếu nào phù hợp với bộ lọc.</p>
            </div>
          )}
        </div>
      </div>

      {/* Confirmation Modal */}
      {showApplyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-zinc-800 flex items-center gap-3 text-brand-orange">
              <AlertCircle className="w-6 h-6" />
              <h2 className="text-lg font-black uppercase tracking-wider text-white">Xác nhận áp dụng lịch chiếu</h2>
            </div>
            
            <div className="p-6 space-y-4">
              <p className="text-zinc-300">
                Bạn đang chuẩn bị áp dụng <strong className="text-white">{selectedItemIds.size} suất chiếu</strong> cho cụm rạp <strong className="text-white">{preview.cinemaName}</strong>.
              </p>
              
              <div className="bg-zinc-950 p-4 rounded-xl border border-zinc-800">
                <ul className="space-y-2 text-sm text-zinc-400">
                  <li className="flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-green-500" /> {selectedItemIds.size} ứng viên đã được chọn</li>
                  <li className="flex items-center gap-2"><Calendar className="w-4 h-4 text-blue-500" /> Từ ngày {formatDate(preview.scheduleFrom)} đến {formatDate(preview.scheduleTo)}</li>
                </ul>
              </div>

              <div className="bg-blue-500/10 border border-blue-500/20 p-3 rounded-xl flex items-start gap-2 text-blue-400">
                <Info className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <p className="text-xs leading-relaxed">
                  Tất cả các suất chiếu sẽ được tạo ở trạng thái <strong>DRAFT</strong>. Bạn cần chuyển chúng sang trạng thái <strong>OPEN FOR BOOKING</strong> để hệ thống có thể mở bán vé.
                </p>
              </div>
            </div>

            <div className="p-4 bg-zinc-950/50 border-t border-zinc-800 flex items-center justify-end gap-3">
              <button 
                onClick={() => setShowApplyModal(false)}
                className="px-5 py-2.5 rounded-xl font-bold text-xs uppercase tracking-wider text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
              >
                Hủy bỏ
              </button>
              <button 
                onClick={() => {
                  setShowApplyModal(false);
                  handleApply();
                }}
                className="px-5 py-2.5 rounded-xl font-black text-xs uppercase tracking-wider bg-brand-orange text-zinc-950 hover:bg-opacity-90 transition-all shadow-lg shadow-brand-orange/10 flex items-center gap-2"
              >
                <Save className="w-4 h-4" /> Xác nhận Áp dụng
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default AdminAutoSchedulePreviewPage;
