import { useState } from 'react';
import useMovieMedia from '@/features/catalog/admin/hooks/useMovieMedia';
import { useOutletContext } from 'react-router-dom';
import { AsyncState, Select, Input, LazyImage } from '@/components/common/ui/uiKit';
import { Plus, Trash2, Loader2, Play } from 'lucide-react';
import { getYoutubeEmbedUrl } from '@/utils/movieHelpers';

const DEFAULT_NEW_MEDIA = {
  mediaType: 'POSTER',
  url: '',
  title: '',
  displayOrder: 1,
  isPrimary: false,
  status: 'ACTIVE'
};

export default function MovieMediaTab({ movie }) {
  const { mediaList, isLoading, error, isSubmitting, reload, addMedia, removeMedia } = useMovieMedia(movie.publicId);
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [newMedia, setNewMedia] = useState(DEFAULT_NEW_MEDIA);
  const [showAdd, setShowAdd] = useState(false);
  const [playVideo, setPlayVideo] = useState(null);

  const handleAdd = async () => {
    if (!newMedia.url.trim()) {
      triggerToast('URL không được bỏ trống', 'error');
      return;
    }
    const res = await addMedia(newMedia);
    if (res.success) {
      triggerToast('Đã thêm media');
      setNewMedia(DEFAULT_NEW_MEDIA);
      setShowAdd(false);
    } else {
      triggerToast(res.error, 'error');
    }
  };

  const handleRemove = async (id) => {
    const shouldRemove = triggerConfirm 
      ? await triggerConfirm('Xóa hình ảnh/video này?')
      : window.confirm('Xóa hình ảnh/video này?');
      
    if (!shouldRemove) return;
    
    const res = await removeMedia(id);
    if (res.success) {
      triggerToast('Đã xóa media');
    } else {
      triggerToast(res.error, 'error');
    }
  };

  return (
    <div className="space-y-6">
      <AsyncState isLoading={isLoading} error={error} onRetry={reload}>
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-100">Danh sách Hình ảnh / Video</h3>
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
              <Select 
                label="Loại" 
                value={newMedia.mediaType} 
                onChange={e => setNewMedia(p => ({ ...p, mediaType: e.target.value }))}
              >
                <option value="POSTER">Poster</option>
                <option value="BANNER">Banner / Backdrop</option>
                <option value="TRAILER">Trailer</option>
                <option value="TEASER">Teaser</option>
                <option value="STILL_IMAGE">Ảnh tĩnh</option>
                <option value="BEHIND_THE_SCENES">Hậu trường</option>
              </Select>
              <Input 
                label="URL" 
                value={newMedia.url} 
                onChange={e => setNewMedia(p => ({ ...p, url: e.target.value }))}
                placeholder="https://..."
              />
              <Input 
                label="Tiêu đề (Tùy chọn)" 
                value={newMedia.title} 
                onChange={e => setNewMedia(p => ({ ...p, title: e.target.value }))}
              />
              <div className="flex items-center gap-4 mt-6">
                <label className="flex items-center gap-2 text-sm text-zinc-400 cursor-pointer">
                  <input 
                    type="checkbox" 
                    checked={newMedia.isPrimary}
                    onChange={e => setNewMedia(p => ({ ...p, isPrimary: e.target.checked }))}
                    className="rounded border-zinc-700 bg-zinc-900 text-brand-orange focus:ring-brand-orange focus:ring-offset-zinc-900"
                  />
                  Đặt làm ảnh chính
                </label>
              </div>
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

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {mediaList.map(m => {
            const isVideo = m.mediaType === 'TRAILER' || m.mediaType === 'TEASER';
            const embedUrl = isVideo ? getYoutubeEmbedUrl(m.url) : null;
            
            return (
              <div key={m.publicId} className="group relative bg-[#0a0a0a] border border-zinc-800 rounded-xl overflow-hidden">
                <div className="aspect-[16/9] relative bg-zinc-900">
                  {isVideo ? (
                    playVideo === m.publicId && embedUrl ? (
                      <iframe
                        src={`${embedUrl}?autoplay=1`}
                        allow="autoplay; encrypted-media"
                        allowFullScreen
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-black">
                        <button 
                          onClick={() => setPlayVideo(m.publicId)}
                          className="w-12 h-12 bg-brand-orange/20 hover:bg-brand-orange text-brand-orange hover:text-white rounded-full flex items-center justify-center transition-all"
                        >
                          <Play size={24} className="ml-1" />
                        </button>
                      </div>
                    )
                  ) : (
                    <LazyImage src={m.url} alt={m.title || m.mediaType} className="w-full h-full object-cover" />
                  )}
                  <div className="absolute top-2 left-2 flex gap-1">
                    <span className="bg-black/60 backdrop-blur text-white text-[10px] font-bold px-2 py-0.5 rounded uppercase">
                      {m.mediaType}
                    </span>
                    {m.isPrimary && (
                      <span className="bg-brand-orange text-white text-[10px] font-bold px-2 py-0.5 rounded uppercase">
                        Primary
                      </span>
                    )}
                  </div>
                </div>
                <div className="p-3 flex items-center justify-between">
                  <span className="text-xs text-zinc-400 truncate pr-2">{m.title || m.url}</span>
                  <button
                    onClick={() => handleRemove(m.publicId)}
                    disabled={isSubmitting}
                    className="text-zinc-500 hover:text-red-500 transition-colors disabled:opacity-50"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
        {mediaList.length === 0 && !showAdd && (
          <div className="text-center py-12 border border-zinc-800 border-dashed rounded-xl">
            <p className="text-sm text-zinc-500">Chưa có media nào.</p>
          </div>
        )}
      </AsyncState>
    </div>
  );
}
