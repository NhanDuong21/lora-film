// eslint-disable-next-line no-unused-vars
import React from 'react';
// eslint-disable-next-line no-unused-vars
import { Image as ImageIcon, Plus, Trash2, Map, LayoutList } from 'lucide-react';

export default function CinemaMediaForm({
  logoUrl,
  setLogoUrl,
  bannerUrl,
  setBannerUrl,
  galleryUrls,
  setGalleryUrls,
  mapImageUrl,
  setMapImageUrl
}) {

  const handleAddGalleryUrl = () => {
    if (galleryUrls.length >= 25) return;
    setGalleryUrls([...galleryUrls, '']);
  };

  const handleRemoveGalleryUrl = (idx) => {
    const updated = galleryUrls.filter((_, i) => i !== idx);
    setGalleryUrls(updated);
  };

  const handleGalleryChange = (idx, value) => {
    const updated = [...galleryUrls];
    updated[idx] = value;
    setGalleryUrls(updated);
  };

  // Helper to validate and display image preview
  const renderPreview = (url) => {
    if (!url || !url.trim().startsWith('http')) {
      return (
        <div className="w-16 h-16 bg-zinc-950 border border-zinc-800 rounded-lg flex items-center justify-center text-zinc-700 shrink-0">
          <ImageIcon className="w-5 h-5" />
        </div>
      );
    }
    return (
      <img
        src={url}
        alt="Preview"
        className="w-16 h-16 object-cover bg-zinc-950 border border-zinc-800 rounded-lg shrink-0"
        onError={(e) => {
          e.target.style.display = 'none';
        }}
      />
    );
  };

  return (
    <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-6">
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
        <ImageIcon className="w-4 h-4 text-orange-500" />
        <h2 className="text-sm font-bold uppercase tracking-wider text-white">Hình Ảnh Cụm Rạp (Media)</h2>
      </div>

      {/* Main Logo & Banner */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Logo */}
        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Ảnh chính Logo (URL)</label>
          <div className="flex gap-2 items-center">
            {renderPreview(logoUrl)}
            <input
              type="text"
              value={logoUrl}
              onChange={e => setLogoUrl(e.target.value)}
              placeholder="Nhập URL ảnh Logo..."
              className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
            />
          </div>
        </div>

        {/* Banner */}
        <div className="flex flex-col gap-1.5">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Ảnh Banner (URL)</label>
          <div className="flex gap-2 items-center">
            {renderPreview(bannerUrl)}
            <input
              type="text"
              value={bannerUrl}
              onChange={e => setBannerUrl(e.target.value)}
              placeholder="Nhập URL ảnh Banner..."
              className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
            />
          </div>
        </div>
      </div>

      {/* Map Layout Image */}
      <div className="flex flex-col gap-1.5">
        <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider">Ảnh sơ đồ Map (URL)</label>
        <div className="flex gap-2 items-center">
          {renderPreview(mapImageUrl)}
          <input
            type="text"
            value={mapImageUrl}
            onChange={e => setMapImageUrl(e.target.value)}
            placeholder="Nhập URL ảnh sơ đồ rạp/sơ đồ vị trí phòng..."
            className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 px-3.5 text-xs text-zinc-100 focus:outline-none transition-colors"
          />
        </div>
      </div>

      {/* Gallery Section */}
      <div className="flex flex-col gap-3">
        <div className="flex justify-between items-center border-t border-zinc-850 pt-4">
          <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider flex items-center gap-1">
            <LayoutList className="w-3.5 h-3.5 text-zinc-400" />
            Bộ sưu tập ảnh rạp (Gallery) ({galleryUrls.filter(Boolean).length}/25)
          </label>
          {galleryUrls.length < 25 && (
            <button
              type="button"
              onClick={handleAddGalleryUrl}
              className="flex items-center gap-1.5 text-[10px] font-bold text-orange-500 hover:text-orange-400 transition-colors uppercase cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" /> Thêm Ảnh
            </button>
          )}
        </div>

        <div className="flex flex-col gap-2.5 max-h-72 overflow-y-auto pr-1">
          {galleryUrls.map((url, idx) => (
            <div key={idx} className="flex gap-2 items-center">
              {renderPreview(url)}
              <div className="relative flex-1">
                <input
                  type="text"
                  value={url}
                  onChange={e => handleGalleryChange(idx, e.target.value)}
                  placeholder={`Nhập URL ảnh Gallery #${idx + 1}...`}
                  className="w-full bg-zinc-950 border border-zinc-800 focus:border-orange-500/40 rounded-xl py-2.5 pl-3.5 pr-10 text-xs text-zinc-100 focus:outline-none transition-colors"
                />
              </div>
              <button
                type="button"
                onClick={() => handleRemoveGalleryUrl(idx)}
                className="p-2.5 bg-zinc-950 hover:bg-red-950/20 text-zinc-500 hover:text-red-500 rounded-xl border border-zinc-800 hover:border-red-500/20 transition-all cursor-pointer shrink-0"
                title="Xóa URL"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
