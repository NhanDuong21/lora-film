// React is removed
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

const AdminAutoSchedulePreviewPage = () => {
  const { id } = useParams();
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const handleSuccess = () => {
    navigate('/admin/showtimes');
  };

  const {
    preview, groupedItems,
    isLoading, isApplying, isUpdatingSelection,
    selectedItemIds, handleToggleSelection,
    handleApply, fetchPreview
  } = useAutoSchedulePreview(id, { triggerToast, onSuccess: handleSuccess });

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
  const canApply = preview.status === 'GENERATED' && !isExpired;

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
              onClick={handleApply}
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

        {/* Timeline View */}
        <div className="space-y-10">
          {Object.keys(groupedItems).sort().map(dateKey => (
            <div key={dateKey} className="space-y-4">
              <h2 className="text-lg font-black text-white flex items-center gap-3 border-b border-zinc-800 pb-2">
                <Calendar className="w-5 h-5 text-brand-orange" />
                {formatDate(dateKey)}
              </h2>

              <div className="space-y-6">
                {Object.keys(groupedItems[dateKey]).sort().map(audKey => {
                  const audItems = groupedItems[dateKey][audKey];
                  
                  return (
                    <div key={audKey} className="bg-zinc-900/40 border border-zinc-800/80 rounded-2xl overflow-hidden">
                      <div className="bg-zinc-900 px-5 py-3 border-b border-zinc-800 flex items-center justify-between">
                        <h3 className="font-bold text-sm text-zinc-200">{audKey}</h3>
                        <span className="text-xs text-zinc-500">{audItems.length} suất chiếu</span>
                      </div>
                      
                      <div className="p-5 overflow-x-auto custom-scrollbar">
                        <div className="flex gap-4 min-w-max pb-2">
                          {audItems.map(item => {
                            const isValid = item.validationStatus === 'VALID';
                            const isSelected = selectedItemIds.has(item.itemPublicId);
                            const isItemApplied = item.applyStatus === 'APPLIED';
                            
                            return (
                              <div 
                                key={item.itemPublicId}
                                className={`relative w-[280px] flex-shrink-0 rounded-xl border p-4 transition-all ${
                                  isItemApplied ? 'border-green-500/30 bg-green-500/5' :
                                  !isValid ? 'border-red-500/30 bg-red-500/5 opacity-70' :
                                  isSelected ? 'border-brand-orange bg-brand-orange/10 shadow-[0_0_15px_rgba(255,107,0,0.15)]' : 
                                  'border-zinc-800 bg-zinc-950 hover:border-zinc-600'
                                }`}
                              >
                                {/* Checkbox for valid/unapplied items */}
                                {isValid && !isItemApplied && canApply && (
                                  <div className="absolute top-4 right-4 z-10">
                                    <input 
                                      type="checkbox"
                                      checked={isSelected}
                                      onChange={() => handleToggleSelection(item.itemPublicId, isSelected)}
                                      disabled={isUpdatingSelection || isApplying}
                                      className="w-5 h-5 rounded border-zinc-700 text-brand-orange focus:ring-brand-orange bg-zinc-900 cursor-pointer disabled:cursor-not-allowed"
                                    />
                                  </div>
                                )}

                                {/* Status Badge */}
                                <div className="absolute top-4 right-4">
                                  {isItemApplied && <CheckCircle2 className="w-5 h-5 text-green-500" />}
                                  {!isValid && <XCircle className="w-5 h-5 text-red-500" title={item.rejectionReason} />}
                                </div>

                                <div className="pr-8">
                                  <div className="flex items-center gap-2 mb-3">
                                    <span className="bg-zinc-800 text-zinc-300 px-2 py-1 rounded text-xs font-bold tracking-wider">
                                      {formatTime(item.startTime)} - {formatTime(item.endTime)}
                                    </span>
                                  </div>

                                  <h4 className="font-bold text-white text-sm line-clamp-1 mb-1" title={item.movieTitle}>
                                    {item.movieTitle}
                                  </h4>
                                  
                                  <p className="text-xs text-zinc-400 mb-3 line-clamp-1">
                                    {item.versionName} • {item.format} • {item.audioLanguage}
                                  </p>

                                  {!isValid && (
                                    <div className="mt-3 p-2 bg-red-500/10 border border-red-500/20 rounded text-[10px] text-red-400 flex items-start gap-1.5">
                                      <Info className="w-3 h-3 flex-shrink-0 mt-0.5" />
                                      <span>{item.rejectionReason || 'Xung đột lịch chiếu'}</span>
                                    </div>
                                  )}

                                  {isItemApplied && (
                                    <div className="mt-3 text-[10px] text-green-400 font-bold flex items-center gap-1">
                                      <CheckCircle2 className="w-3 h-3" /> Đã tạo thành công
                                    </div>
                                  )}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}

          {Object.keys(groupedItems).length === 0 && (
            <div className="text-center py-20 text-zinc-500">
              <p>Không có ứng viên suất chiếu nào được tạo.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminAutoSchedulePreviewPage;
