import { useState } from 'react';
import useMovieVersions from '@/features/catalog/admin/hooks/useMovieVersions';
import { useOutletContext } from 'react-router-dom';
import { AsyncState, Select, Input } from '@/components/common/ui/uiKit';
import { Plus, Trash2, Loader2 } from 'lucide-react';
import { FORMAT_MAP_TO_API, FORMAT_MAP_FROM_API } from '@/utils/movieHelpers';

const DEFAULT_NEW_VERSION = {
  versionName: '',
  format: '2D',
  audioLanguage: 'EN',
  subtitleLanguage: 'VI',
  dubLanguage: 'NONE',
  status: 'ACTIVE'
};

export default function MovieVersionTab({ movie }) {
  const { versions, isLoading, error, isSubmitting, reload, addVersion, removeVersion } = useMovieVersions(movie.publicId);
  const { triggerToast } = useOutletContext();
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
    } else {
      triggerToast(res.error, 'error');
    }
  };

  const handleRemove = async (id) => {
    if (!window.confirm('Xóa phiên bản này?')) return;
    const res = await removeVersion(id);
    if (res.success) {
      triggerToast('Đã xóa phiên bản');
    } else {
      triggerToast(res.error, 'error');
    }
  };

  return (
    <div className="space-y-6">
      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        {versions.length === 0 && !showAdd ? (
          <div className="text-center py-12 bg-[#050506] rounded-xl border border-zinc-800 border-dashed">
            <p className="text-sm text-zinc-500 mb-4">Phim chưa có phiên bản nào.</p>
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
              <h3 className="text-sm font-semibold text-zinc-100">Danh sách phiên bản</h3>
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
              {versions.map(v => (
                <div key={v.publicId} className="flex items-center justify-between p-4 bg-[#0a0a0a] border border-zinc-800 rounded-xl">
                  <div>
                    <h4 className="text-sm font-semibold text-zinc-100">{v.versionName}</h4>
                    <div className="flex items-center gap-2 mt-1 text-xs text-zinc-500">
                      <span className="bg-zinc-800 px-2 py-0.5 rounded">{FORMAT_MAP_FROM_API[v.format] || v.format}</span>
                      <span>•</span>
                      <span>Audio: {v.audioLanguage}</span>
                      <span>•</span>
                      <span>Phụ đề: {v.subtitleLanguage}</span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleRemove(v.publicId)}
                    disabled={isSubmitting}
                    className="p-2 text-zinc-500 hover:text-red-500 hover:bg-red-500/10 rounded-lg transition-colors disabled:opacity-50"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </AsyncState>
    </div>
  );
}
