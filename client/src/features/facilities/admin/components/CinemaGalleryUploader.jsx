import { useRef, useState } from 'react';
import { UploadCloud, X } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';

export default function CinemaGalleryUploader({ 
  label, 
  description, 
  files = [], 
  onChange, 
  maxFiles = 25 
}) {
  const { triggerToast } = useOutletContext() || {};
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFiles = (newFiles) => {
    const validImageFiles = Array.from(newFiles).filter(file => file.type.startsWith('image/'));
    if (validImageFiles.length !== newFiles.length) {
      if (triggerToast) triggerToast('Một số file không phải là hình ảnh và đã bị bỏ qua.', 'warning');
      else alert('Một số file không phải là hình ảnh và đã bị bỏ qua.');
    }

    // Filter duplicates by name and size (rudimentary check)
    const existingFileSignatures = files.map(f => typeof f === 'string' ? f : `${f.name}-${f.size}`);
    
    const uniqueNewFiles = validImageFiles.filter(file => {
      const signature = `${file.name}-${file.size}`;
      return !existingFileSignatures.includes(signature);
    });

    const combined = [...files, ...uniqueNewFiles].slice(0, maxFiles);
    onChange(combined);
    
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const onDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const onDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const onDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const removeFile = (index) => {
    const newFiles = [...files];
    newFiles.splice(index, 1);
    onChange(newFiles);
  };

  return (
    <div className="flex flex-col gap-2">
      <div className="flex justify-between items-end mb-1">
        <div>
          <label className="text-[10px] font-black text-zinc-500 uppercase tracking-widest">{label}</label>
          {description && <p className="text-[10px] text-zinc-600 mt-0.5">{description}</p>}
        </div>
        <span className="text-[10px] font-mono text-zinc-500">
          {files.length} / {maxFiles}
        </span>
      </div>

      <div
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`relative flex flex-col items-center justify-center p-8 rounded-xl border-2 border-dashed transition-all cursor-pointer
          ${isDragging ? 'border-brand-orange bg-brand-orange/5' : 'border-zinc-700 hover:border-zinc-500 bg-zinc-900/50 hover:bg-zinc-900'}
        `}
      >
        <UploadCloud className={`w-8 h-8 mb-3 ${isDragging ? 'text-brand-orange' : 'text-zinc-500'}`} />
        <p className="text-xs font-bold text-zinc-400 uppercase tracking-wider text-center">
          Kéo thả ảnh vào đây hoặc nhấn để chọn
        </p>
        <p className="text-[10px] text-zinc-600 mt-2 font-mono">
          Hỗ trợ JPG, PNG, WEBP (Tối đa {maxFiles} ảnh)
        </p>
      </div>

      <input 
        type="file" 
        accept="image/*" 
        multiple 
        className="hidden" 
        ref={fileInputRef}
        onChange={(e) => handleFiles(e.target.files)}
      />

      {files.length > 0 && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-4">
          {files.map((file, idx) => {
            const isString = typeof file === 'string';
            const previewUrl = isString ? file : URL.createObjectURL(file);
            return (
              <div key={idx} className="relative aspect-video rounded-xl overflow-hidden bg-zinc-900 border border-zinc-800 group">
                <img src={previewUrl} alt={`Gallery ${idx}`} className="w-full h-full object-cover" />
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile(idx);
                  }}
                  className="absolute top-2 right-2 p-1.5 bg-black/60 hover:bg-red-500/80 text-white rounded-lg opacity-0 group-hover:opacity-100 transition-all backdrop-blur-sm"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
