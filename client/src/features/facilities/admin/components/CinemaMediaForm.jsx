import { Image as ImageIcon } from 'lucide-react';
import CinemaImageUploader from './CinemaImageUploader';
import CinemaGalleryUploader from './CinemaGalleryUploader';

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

  return (
    <div className="bg-zinc-900/60 border border-zinc-800/80 rounded-2xl p-6 flex flex-col gap-8">
      <div className="flex items-center gap-2 border-b border-zinc-800/80 pb-3">
        <ImageIcon className="w-4 h-4 text-brand-coral" />
        <h2 className="text-sm font-bold uppercase tracking-wider text-white">Hình Ảnh Cụm Rạp (Media)</h2>
      </div>

      {/* Main Logo & Banner */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <CinemaImageUploader 
          label="Ảnh Logo"
          description="Khuyên dùng ảnh vuông, nền trong suốt."
          aspectRatio={1}
          value={logoUrl}
          onChange={setLogoUrl}
        />
        
        <CinemaImageUploader 
          label="Ảnh Banner"
          description="Khuyên dùng ảnh 16:9, chất lượng cao."
          aspectRatio={16/9}
          value={bannerUrl}
          onChange={setBannerUrl}
        />
      </div>

      <div className="border-t border-zinc-800/50 pt-6">
        <CinemaImageUploader 
          label="Sơ Đồ Rạp (Map)"
          description="Sơ đồ bố trí các phòng chiếu trong cụm rạp."
          aspectRatio={16/9}
          value={mapImageUrl}
          onChange={setMapImageUrl}
        />
      </div>

      <div className="border-t border-zinc-800/50 pt-6">
        <CinemaGalleryUploader 
          label="Bộ Sưu Tập (Gallery)"
          description="Hình ảnh không gian, sảnh chờ, v.v..."
          files={galleryUrls.filter(Boolean)}
          onChange={setGalleryUrls}
          maxFiles={25}
        />
      </div>
    </div>
  );
}
