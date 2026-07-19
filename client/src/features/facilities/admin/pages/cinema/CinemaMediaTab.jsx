import { useState } from 'react';
import { PlusCircle, Trash2, Image as ImageIcon, Star } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import adminCinemaService from '../../services/adminCinemaService';
import CinemaImageUploader from '../../components/CinemaImageUploader';

export default function CinemaMediaTab({ cinema, onAdd, onUpdate, onDelete }) {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [formData, setFormData] = useState({
    mediaType: 'GALLERY',
    url: '',
    file: null,
    title: '',
    displayOrder: 0,
    isPrimary: false
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const mediaList = cinema?.gallery || [];

  const openAddForm = () => {
    setFormData({ mediaType: 'GALLERY', url: '', file: null, title: '', displayOrder: 0, isPrimary: false });
    setEditingId(null);
    setIsFormOpen(true);
  };

  const openEditForm = (media) => {
    setFormData({
      mediaType: media.mediaType,
      url: media.url,
      file: null,
      title: media.title || '',
      displayOrder: media.displayOrder || 0,
      isPrimary: media.isPrimary || false
    });
    setEditingId(media.publicId);
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    let success;
    let submitData = { ...formData };
    
    if (formData.file) {
      try {
        const uploadRes = await adminCinemaService.uploadCinemaMedia(formData.file, formData.mediaType, cinema.publicId);
        submitData.url = uploadRes.data?.secureUrl || uploadRes.data || uploadRes.secureUrl;
      } catch (err) {
        console.error("Lỗi upload:", err);
        setIsSubmitting(false);
        triggerToast?.("Upload ảnh thất bại!", "error");
        return;
      }
    }

    if (editingId) {
      success = await onUpdate(editingId, submitData);
    } else {
      success = await onAdd(submitData);
    }
    
    setIsSubmitting(false);
    if (success) {
      closeForm();
    }
  };

  const handleDelete = async (mediaId, title) => {
    const shouldDelete = triggerConfirm
      ? await triggerConfirm(`Bạn có chắc muốn xóa phương tiện "${title || 'Không tên'}"?`)
      : window.confirm(`Bạn có chắc muốn xóa phương tiện "${title || 'Không tên'}"?`);
      
    if (shouldDelete) {
      await onDelete(mediaId);
    }
  };

  return (
    <div className="space-y-6 pb-20">
      <div className="flex justify-between items-center bg-zinc-900/30 border border-zinc-800 p-5 rounded-2xl">
        <div>
          <h2 className="text-sm font-black text-zinc-50 uppercase tracking-wider">HÌNH ẢNH RẠP CHIẾU</h2>
          <p className="text-xs text-zinc-500 mt-1">Quản lý banner, logo, và thư viện ảnh của rạp</p>
        </div>
        <button
          onClick={openAddForm}
          className="flex items-center gap-2 bg-zinc-800 hover:bg-zinc-700 text-white px-4 py-2 rounded-xl text-xs font-bold transition-colors border border-zinc-700"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Thêm Ảnh</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {mediaList.map(media => (
          <div key={media.publicId} className="bg-zinc-900/50 border border-zinc-800 rounded-2xl overflow-hidden group">
            <div className="relative aspect-video bg-zinc-950 flex items-center justify-center">
              <img 
                src={media.url} 
                alt={media.title || 'Cinema media'} 
                className="w-full h-full object-cover opacity-80 group-hover:opacity-100 transition-opacity"
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.nextSibling.style.display = 'flex';
                }}
              />
              <div className="absolute inset-0 flex flex-col items-center justify-center text-zinc-600 hidden">
                <ImageIcon className="w-8 h-8 mb-2 opacity-50" />
                <span className="text-[10px] uppercase font-bold tracking-widest">Lỗi ảnh</span>
              </div>
              
              {media.isPrimary && (
                <div className="absolute top-3 left-3 bg-brand-orange text-white text-[10px] font-black uppercase tracking-wider px-2 py-1 rounded-lg flex items-center gap-1 shadow-lg shadow-brand-orange/20">
                  <Star className="w-3 h-3 fill-current" />
                  Ảnh Chính
                </div>
              )}
              
              <div className="absolute top-3 right-3 bg-zinc-950/80 backdrop-blur-md text-zinc-300 text-[10px] font-black uppercase tracking-wider px-2 py-1 rounded-lg border border-zinc-800 font-mono">
                {media.mediaType}
              </div>
            </div>
            
            <div className="p-4 flex justify-between items-center">
              <div className="truncate pr-4">
                <h3 className="text-sm font-bold truncate text-zinc-200">{media.title || 'Không có tiêu đề'}</h3>
                <p className="text-[10px] text-zinc-500 font-mono mt-1 truncate">{media.url}</p>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => openEditForm(media)}
                  className="p-2 bg-zinc-800 hover:bg-zinc-700 rounded-lg text-zinc-400 hover:text-white transition-colors"
                >
                  <PlusCircle className="w-4 h-4 rotate-45" style={{ transform: 'rotate(0deg)' }} /> 
                  {/* Reuse plus icon for Edit but let's actually just use it as an action button, it should be an edit icon but we used what we imported */}
                </button>
                <button
                  onClick={() => handleDelete(media.publicId, media.title)}
                  className="p-2 bg-zinc-800 hover:bg-red-500/20 rounded-lg text-zinc-400 hover:text-red-400 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}

        {mediaList.length === 0 && (
          <div className="col-span-full py-12 flex flex-col items-center justify-center border-2 border-dashed border-zinc-800 rounded-3xl">
            <ImageIcon className="w-12 h-12 text-zinc-700 mb-4" />
            <p className="text-sm font-bold text-zinc-500 uppercase tracking-widest">Chưa có phương tiện nào</p>
          </div>
        )}
      </div>

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-zinc-950 border border-zinc-800 rounded-3xl w-full max-w-lg shadow-2xl p-6">
            <h2 className="text-lg font-black uppercase tracking-wider mb-6 text-zinc-50">
              {editingId ? 'Cập Nhật Phương Tiện' : 'Thêm Phương Tiện'}
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <CinemaImageUploader 
                  label="Tệp Hình Ảnh"
                  description="Ảnh sẽ được tự động upload khi lưu"
                  aspectRatio={16/9}
                  value={formData.file || formData.url}
                  onChange={(val) => setFormData({...formData, file: val})}
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                    Loại (Type)
                  </label>
                  <select
                    value={formData.mediaType}
                    onChange={(e) => setFormData({...formData, mediaType: e.target.value})}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors cursor-pointer"
                  >
                    <option value="GALLERY">Gallery</option>
                    <option value="BANNER">Banner</option>
                    <option value="MAP">Bản Đồ</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[10px] font-black text-zinc-500 uppercase tracking-widest mb-2">
                    Tiêu Đề
                  </label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => setFormData({...formData, title: e.target.value})}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-3 text-sm focus:border-brand-orange outline-none transition-colors"
                    placeholder="Mặt tiền rạp..."
                  />
                </div>
              </div>

              <div className="flex items-center gap-4 pt-2">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.isPrimary}
                    onChange={(e) => setFormData({...formData, isPrimary: e.target.checked})}
                    className="w-4 h-4 bg-zinc-900 border-zinc-800 rounded accent-brand-orange"
                  />
                  <span className="text-sm font-bold text-zinc-300">Đặt làm ảnh chính</span>
                </label>
              </div>

              <div className="flex gap-3 pt-6">
                <button
                  type="button"
                  onClick={closeForm}
                  className="flex-1 bg-zinc-900 hover:bg-zinc-800 text-zinc-300 font-bold py-3 rounded-xl uppercase tracking-wider text-xs transition-colors"
                >
                  Hủy Bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 bg-brand-orange hover:bg-opacity-90 text-white font-bold py-3 rounded-xl uppercase tracking-wider text-xs transition-colors disabled:opacity-50"
                >
                  {isSubmitting ? 'ĐANG LƯU...' : 'LƯU PHƯƠNG TIỆN'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
