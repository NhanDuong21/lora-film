import { useState } from 'react';
import useMovieVersions from '@/features/catalog/admin/hooks/useMovieVersions';
import { useOutletContext } from 'react-router-dom';
import { AsyncState, Select, Input } from '@/components/common/ui/uiKit';
import { Plus, Trash2, Loader2 } from 'lucide-react';
import { FORMAT_MAP_TO_API, FORMAT_MAP_FROM_API } from '@/utils/movieHelpers';
import { getVersionStatusConfig } from '@/features/catalog/admin/config/movieVersionStatusConfig';

const DEFAULT_NEW_VERSION = {
  versionName: '',
  format: '2D',
  audioLanguage: 'EN',
  subtitleLanguage: 'VI',
  dubLanguage: 'NONE',
  status: 'ACTIVE'
};

export default function MovieVersionTab({ movie, onUpdate }) {
  const { versions, isLoading, error, isSubmitting, reload, addVersion, removeVersion } = useMovieVersions(movie.publicId);
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [newVer, setNewVer] = useState(DEFAULT_NEW_VERSION);
  const [showAdd, setShowAdd] = useState(false);

  const handleAdd = async () => {
    if (!newVer.versionName.trim()) {
      triggerToast('Tên phiên bản không được bỏ trống', 'error');
      return;
    }
    const payload = {
      ...newVer,
      format: FORMAT_MAP_TO_API[newVer.format] || newVer.format,
      audioLanguage: newVer.audioLanguage,
      subtitleLanguage: newVer.subtitleLanguage,
      dubLanguage: newVer.dubLanguage,
    };
    
    const res = await addVersion(payload);
    if (res.success) {
      triggerToast('Đã thêm phiên bản mới');
      setNewVer(DEFAULT_NEW_VERSION);
      setShowAdd(false);
      if (onUpdate) onUpdate();
    } else {
      triggerToast(res.error, 'error');
    }
  };

  const handleRemove = async (id) => {
    const shouldRemove = await triggerConfirm?.({
      title: 'Xóa phiên bản phim?',
      message: 'Chỉ có thể xóa phiên bản chưa được dùng trong lịch chiếu.',
      confirmLabel: 'Xóa phiên bản',
      tone: 'danger',
    });
      
    if (!shouldRemove) return;
    
    const res = await removeVersion(id);
    if (res.success) {
      triggerToast('Đã xóa phiên bản');
      if (onUpdate) onUpdate();
    } else {
      triggerToast(res.error, 'error');
    }
  };

  return (
    <div className="space-y-6">
      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        {versions.length === 0 && !showAdd ? (
          <div className="text-center py-12 bg-zinc-900/50 rounded-xl border border-zinc-800 border-dashed">
            <p className="text-sm text-zinc-500 mb-4">Chưa có bản chiếu nào.<br />Thêm bản chiếu đầu tiên để nhân viên có thể lập lịch suất chiếu.</p>
            <button
              onClick={() => setShowAdd(true)}
              className="inline-flex items-center gap-2 bg-brand-orange text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-brand-orange/90 transition-colors"
            >
              <Plus size={16} /> Thêm phiên bản đầu tiên
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-zinc-100">Bản chiếu của phim</h3>
                <p className="mt-1 text-xs text-zinc-500">Ví dụ: 2D phụ đề, 3D lồng tiếng hoặc IMAX.</p>
              </div>
              {!showAdd && (
                <button
                  onClick={() => setShowAdd(true)}
                  className="flex items-center gap-2 text-xs font-semibold text-brand-orange hover:text-brand-orange/80 transition-colors"
                >
                  <Plus size={14} /> Thêm mới
                </button>
              )}
            </div>

            {showAdd && (
              <div className="bg-[#050506] border border-zinc-800 rounded-xl p-4 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input 
                    label="Tên phiên bản" 
                    value={newVer.versionName} 
                    onChange={e => setNewVer(p => ({ ...p, versionName: e.target.value }))}
                    placeholder="Vd: 2D Vietsub"
                  />
                  <Select 
                    label="Định dạng" 
                    value={newVer.format} 
                    onChange={e => setNewVer(p => ({ ...p, format: e.target.value }))}
                  >
                    <option value="2D">2D</option>
                    <option value="3D">3D</option>
                    <option value="IMAX">IMAX</option>
                    <option value="4DX">4DX</option>
                  </Select>
                  <Input 
                    label="Ngôn ngữ gốc" 
                    value={newVer.audioLanguage} 
                    onChange={e => setNewVer(p => ({ ...p, audioLanguage: e.target.value.toUpperCase() }))}
                  />
                  <Input 
                    label="Ngôn ngữ phụ đề" 
                    value={newVer.subtitleLanguage} 
                    onChange={e => setNewVer(p => ({ ...p, subtitleLanguage: e.target.value.toUpperCase() }))}
                  />
                </div>
                <div className="flex items-center gap-2 justify-end">
                  <button 
                    onClick={() => setShowAdd(false)}
                    className="px-4 py-2 text-xs font-semibold text-zinc-400 hover:text-zinc-100 transition-colors"
                  >
                    Hủy
                  </button>
                  <button
                    onClick={handleAdd}
                    disabled={isSubmitting}
                    className="flex items-center gap-2 bg-brand-orange text-white px-4 py-2 rounded-lg text-xs font-semibold hover:bg-brand-orange/90 transition-colors disabled:opacity-50"
                  >
                    {isSubmitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
                    Lưu
                  </button>
                </div>
              </div>
            )}

            <div className="grid gap-3">
              {versions.map(v => {
                const statusCfg = getVersionStatusConfig(v.status);
                return (
                <div key={v.publicId} className="flex items-start justify-between p-4 bg-zinc-900/50 border border-zinc-800 rounded-xl">
                  <div>
                    <div className="flex items-center gap-3">
                      <h4 className="text-sm font-semibold text-zinc-100">{v.versionName}</h4>
                      <span className={`text-[10px] font-black px-2 py-0.5 rounded uppercase border ${statusCfg.colorClass}`}>
                        {statusCfg.label}
                      </span>
                    </div>
                    
                    <div className="flex flex-wrap items-center gap-4 mt-3 text-xs text-zinc-400">
                      <div>
                        <span className="text-zinc-600 mr-1">Định dạng:</span>
                        <span className="font-medium text-zinc-300">{FORMAT_MAP_FROM_API[v.format] || v.format}</span>
                      </div>
                      <div>
                        <span className="text-zinc-600 mr-1">Âm thanh:</span>
                        <span className="font-medium text-zinc-300">{v.audioLanguage}</span>
                      </div>
                      <div>
                        <span className="text-zinc-600 mr-1">Phụ đề:</span>
                        <span className="font-medium text-zinc-300">{v.subtitleLanguage}</span>
                      </div>
                      <div>
                        <span className="text-zinc-600 mr-1">Lồng tiếng:</span>
                        <span className="font-medium text-zinc-300">
                          {v.dubLanguage && v.dubLanguage !== 'NONE' ? v.dubLanguage : 'Không'}
                        </span>
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={() => handleRemove(v.publicId)}
                    disabled={isSubmitting}
                    className="p-2 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-colors disabled:opacity-50"
                    title="Xóa phiên bản"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
                );
              })}
            </div>
          </div>
        )}
      </AsyncState>
    </div>
  );
}
