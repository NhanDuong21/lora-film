import { useState, useRef } from 'react';
import { UploadCloud, Image as ImageIcon, Trash2 } from 'lucide-react';
import ImageCropDialog from './ImageCropDialog';
import { useOutletContext } from 'react-router-dom';

export default function CinemaImageUploader({ 
  label, 
  description, 
  value, 
  onChange, 
  aspectRatio = 1, // 1 for Logo, 16/9 for Banner
  required = false
}) {
  const { triggerToast } = useOutletContext() || {};
  const fileInputRef = useRef(null);
  const [selectedImageStr, setSelectedImageStr] = useState(null);
  const [showCropDialog, setShowCropDialog] = useState(false);

  // value is expected to be a File object (Blob) when locally selected and cropped, 
  // or a string URL if it's already uploaded.
  
  const previewUrl = value ? (typeof value === 'string' ? value : URL.createObjectURL(value)) : null;

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        triggerToast?.('Vui lòng chọn file hình ảnh hợp lệ.', 'error');
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        setSelectedImageStr(reader.result);
        setShowCropDialog(true);
      };
      reader.readAsDataURL(file);
    }
    // reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleCropComplete = (croppedBlob) => {
    setShowCropDialog(false);
    setSelectedImageStr(null);
    onChange(croppedBlob);
  };

  const handleRemove = (e) => {
    e.stopPropagation();
    onChange(null);
  };

  return (
    <div className="flex flex-col gap-2">
      <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">
        {label} {required && <span className="text-brand-orange">*</span>}
      </label>
      {description && <p className="text-[10px] text-zinc-600 mb-1">{description}</p>}
      
      <div 
        onClick={() => !value && fileInputRef.current?.click()}
        className={`relative flex flex-col items-center justify-center rounded-xl overflow-hidden transition-all border-2 border-dashed
          ${previewUrl ? 'border-zinc-800' : 'border-zinc-700 hover:border-brand-orange bg-zinc-900 hover:bg-zinc-800 cursor-pointer'}
          ${aspectRatio === 1 ? 'aspect-square max-w-[200px]' : 'aspect-video w-full'}
        `}
      >
        {previewUrl ? (
          <>
            <img src={previewUrl} alt={label} className="w-full h-full object-cover opacity-90" />
            <div className="absolute inset-0 bg-black/50 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center gap-4 backdrop-blur-sm">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="p-2 bg-zinc-800 hover:bg-zinc-700 text-white rounded-lg transition-colors shadow-lg"
                title="Thay đổi ảnh"
              >
                <UploadCloud className="w-5 h-5" />
              </button>
              <button
                type="button"
                onClick={handleRemove}
                className="p-2 bg-zinc-800 hover:bg-red-500/20 text-white hover:text-red-400 rounded-lg transition-colors shadow-lg"
                title="Xóa ảnh"
              >
                <Trash2 className="w-5 h-5" />
              </button>
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center gap-2 text-zinc-500 p-6 text-center">
            <ImageIcon className="w-8 h-8 opacity-50 mb-2" />
            <span className="text-xs font-bold uppercase tracking-wider">Nhấn để chọn ảnh</span>
            <span className="text-[9px] font-mono opacity-60">Tỉ lệ {aspectRatio === 1 ? '1:1' : '16:9'}</span>
          </div>
        )}
      </div>

      <input 
        type="file" 
        accept="image/*" 
        className="hidden" 
        ref={fileInputRef}
        onChange={handleFileChange}
      />

      {showCropDialog && selectedImageStr && (
        <ImageCropDialog
          imageUrl={selectedImageStr}
          aspectRatio={aspectRatio}
          onCrop={handleCropComplete}
          onCancel={() => {
            setShowCropDialog(false);
            setSelectedImageStr(null);
          }}
        />
      )}
    </div>
  );
}
