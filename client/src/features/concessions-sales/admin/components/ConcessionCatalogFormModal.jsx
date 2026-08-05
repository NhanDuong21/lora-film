import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { ImagePlus, LoaderCircle, PackagePlus, Pencil, Trash2, X } from 'lucide-react';
import { createPortal } from 'react-dom';

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
const SUPPORTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const IMAGE_TYPE_BY_EXTENSION = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp'
};

const typeOptions = [
  { value: 'FOOD', label: 'Bắp và đồ ăn' },
  { value: 'DRINK', label: 'Nước uống' },
  { value: 'COMBO', label: 'Combo' }
];

const initialState = item => ({
  code: item?.code || '',
  name: item?.name || '',
  type: item?.type || 'FOOD',
  price: item?.price ? String(item.price) : '',
  imageUrl: item?.imageUrl || '',
  active: item?.active ?? true,
  sellable: item?.sellable ?? true
});

const isSupportedImageSource = value => {
  if (!value) return true;
  return /^https?:\/\//i.test(value) || value.startsWith('/');
};

const normalizeImageForUpload = async file => {
  // Re-encode images saved from websites so misleading metadata or codecs do
  // not make the remote image storage reject an otherwise viewable image.
  if (
    typeof createImageBitmap !== 'function'
    || typeof document === 'undefined'
    || (typeof navigator !== 'undefined' && /jsdom/i.test(navigator.userAgent || ''))
  ) return file;

  const bitmap = await createImageBitmap(file);
  const canvas = document.createElement('canvas');
  canvas.width = bitmap.width;
  canvas.height = bitmap.height;
  const context = canvas.getContext('2d');
  if (!context) {
    bitmap.close?.();
    return file;
  }
  if (typeof canvas.toBlob !== 'function') {
    bitmap.close?.();
    return file;
  }
  context.drawImage(bitmap, 0, 0);
  bitmap.close?.();

  const outputType = file.type === 'image/jpeg' ? 'image/jpeg' : 'image/png';
  const blob = await new Promise(resolve => canvas.toBlob(resolve, outputType, 0.92));
  if (!blob) return file;

  const baseName = file.name?.replace(/\.[^.]+$/, '') || 'product-image';
  return new File([blob], `${baseName}.${outputType === 'image/jpeg' ? 'jpg' : 'png'}`, {
    type: outputType,
    lastModified: file.lastModified
  });
};

export default function ConcessionCatalogFormModal({
  item,
  onClose,
  onSubmit,
  pending = false
}) {
  const [formState, setFormState] = useState(() => initialState(item));
  const [imageFile, setImageFile] = useState(null);
  const [filePreviewUrl, setFilePreviewUrl] = useState('');
  const [formError, setFormError] = useState('');
  const [previewFailed, setPreviewFailed] = useState(false);
  const [isDragActive, setIsDragActive] = useState(false);
  const [imageProcessing, setImageProcessing] = useState(false);
  const firstInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();
  const fileInputId = useId();
  const editing = Boolean(item);

  const previewUrl = useMemo(
    () => filePreviewUrl || (isSupportedImageSource(formState.imageUrl) ? formState.imageUrl : ''),
    [filePreviewUrl, formState.imageUrl]
  );

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    firstInputRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape' && !pending) onClose();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, pending]);

  useEffect(() => () => {
    if (filePreviewUrl) URL.revokeObjectURL(filePreviewUrl);
  }, [filePreviewUrl]);

  const updateField = (field, value) => {
    setFormState(previous => ({ ...previous, [field]: value }));
    setFormError('');
  };

  const handleImageFile = async file => {
    if (!file) return;

    const extension = file.name?.split('.').pop()?.toLowerCase();
    const normalizedType = file.type || IMAGE_TYPE_BY_EXTENSION[extension];
    if (!SUPPORTED_IMAGE_TYPES.includes(normalizedType)) {
      setFormError('Ảnh sản phẩm chỉ hỗ trợ định dạng JPG, PNG hoặc WEBP.');
      return;
    }
    if (file.size > MAX_IMAGE_SIZE_BYTES) {
      setFormError('Ảnh sản phẩm không được vượt quá 5 MB.');
      return;
    }

    // Some operating systems do not provide a MIME type for files dragged
    // directly from a folder. Normalize it so the multipart upload matches
    // the same contract as the click-to-select option.
    setImageProcessing(true);
    try {
      const typedFile = file.type === normalizedType
        ? file
        : new File([file], file.name, { type: normalizedType, lastModified: file.lastModified });
      const normalizedFile = await normalizeImageForUpload(typedFile);

      if (normalizedFile.size > MAX_IMAGE_SIZE_BYTES) {
        setFormError('Ảnh sau khi chuẩn hóa không được vượt quá 5 MB.');
        return;
      }
      if (filePreviewUrl) URL.revokeObjectURL(filePreviewUrl);
      setImageFile(normalizedFile);
      setFilePreviewUrl(URL.createObjectURL(normalizedFile));
      setPreviewFailed(false);
      setFormError('');
    } catch {
      setFormError('Không thể đọc ảnh này. Vui lòng dùng ảnh JPG, PNG hoặc WEBP hợp lệ.');
    } finally {
      setImageProcessing(false);
    }
  };

  const handleImageChange = event => {
    handleImageFile(event.target.files?.[0]);
    // Allow selecting the same file again after removing/rejecting it.
    event.target.value = '';
  };

  const handleDragOver = event => {
    event.preventDefault();
    if (!pending && !imageProcessing) setIsDragActive(true);
  };

  const handleDragLeave = event => {
    if (!event.currentTarget.contains(event.relatedTarget)) setIsDragActive(false);
  };

  const handleDrop = event => {
    event.preventDefault();
    setIsDragActive(false);
    if (!pending && !imageProcessing) handleImageFile(event.dataTransfer.files?.[0]);
  };

  const removeSelectedImage = () => {
    if (filePreviewUrl) URL.revokeObjectURL(filePreviewUrl);
    setImageFile(null);
    setFilePreviewUrl('');
    setPreviewFailed(false);
  };

  const handleSubmit = event => {
    event.preventDefault();

    const code = formState.code.trim().toUpperCase();
    const name = formState.name.trim();
    const price = Number(formState.price);
    const imageUrl = formState.imageUrl.trim();

    if (!/^[A-Z0-9_-]+$/.test(code)) {
      setFormError('Mã sản phẩm chỉ được gồm chữ, số, dấu gạch dưới hoặc gạch ngang.');
      return;
    }
    if (!name) {
      setFormError('Vui lòng nhập tên sản phẩm.');
      return;
    }
    if (!Number.isFinite(price) || price < 1000) {
      setFormError('Giá bán phải từ 1.000đ trở lên.');
      return;
    }
    if (!imageFile && imageUrl && !isSupportedImageSource(imageUrl) && imageUrl !== item?.imageUrl) {
      setFormError('Đường dẫn ảnh phải bắt đầu bằng http://, https:// hoặc /.');
      return;
    }

    onSubmit({
      payload: {
        code,
        name,
        type: formState.type,
        price,
        imageUrl: imageFile ? imageUrl : imageUrl || null,
        active: formState.active,
        sellable: formState.active && formState.sellable
      },
      image: imageFile
    });
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget && !pending) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-3xl border border-zinc-700 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <form onSubmit={handleSubmit}>
          <div className="flex items-start justify-between gap-4 border-b border-zinc-800 px-6 py-5">
            <div className="flex items-start gap-3">
              <div className="rounded-2xl bg-orange-500/10 p-3 text-orange-400">
                {editing ? <Pencil className="h-6 w-6" /> : <PackagePlus className="h-6 w-6" />}
              </div>
              <div>
                <h2 id={titleId} className="text-lg font-black text-white">
                  {editing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới'}
                </h2>
                <p id={descriptionId} className="mt-1 text-xs leading-5 text-zinc-500">
                  Quản lý thông tin hiển thị và trạng thái bán kèm của sản phẩm.
                </p>
              </div>
            </div>
            <button
              type="button"
              aria-label="Đóng"
              disabled={pending}
              onClick={onClose}
              className="rounded-xl p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white disabled:opacity-40"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="space-y-5 px-6 py-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-2 text-xs font-bold text-zinc-300">
                <span>Mã sản phẩm <span className="text-red-400">*</span></span>
                <input
                  ref={firstInputRef}
                  type="text"
                  required
                  disabled={editing || pending}
                  maxLength={50}
                  placeholder="Ví dụ: POP_CARAMEL_L"
                  value={formState.code}
                  onChange={event => updateField('code', event.target.value.toUpperCase())}
                  className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 font-mono text-sm text-white outline-none transition-colors focus:border-orange-500 disabled:cursor-not-allowed disabled:opacity-55"
                />
                {editing && (
                  <span className="block text-[11px] font-normal text-zinc-500">
                    Mã sản phẩm được giữ cố định để bảo toàn dữ liệu lịch sử.
                  </span>
                )}
              </label>

              <label className="space-y-2 text-xs font-bold text-zinc-300">
                <span>Phân loại <span className="text-red-400">*</span></span>
                <select
                  value={formState.type}
                  disabled={pending}
                  onChange={event => updateField('type', event.target.value)}
                  className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition-colors focus:border-orange-500"
                >
                  {typeOptions.map(option => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
            </div>

            <label className="block space-y-2 text-xs font-bold text-zinc-300">
              <span>Tên sản phẩm <span className="text-red-400">*</span></span>
              <input
                type="text"
                required
                disabled={pending}
                maxLength={255}
                placeholder="Ví dụ: Bắp rang caramel cỡ lớn"
                value={formState.name}
                onChange={event => updateField('name', event.target.value)}
                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition-colors focus:border-orange-500"
              />
            </label>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-2 text-xs font-bold text-zinc-300">
                <span>Giá bán (VND) <span className="text-red-400">*</span></span>
                <input
                  type="number"
                  required
                  min="1000"
                  step="1000"
                  disabled={pending}
                  placeholder="50000"
                  value={formState.price}
                  onChange={event => updateField('price', event.target.value)}
                  className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition-colors focus:border-orange-500"
                />
                <span className="block text-[11px] font-normal text-zinc-500">
                  {Number(formState.price) > 0
                    ? `${Number(formState.price).toLocaleString('vi-VN')}đ`
                    : 'Tối thiểu 1.000đ'}
                </span>
              </label>

              <div
                className={`space-y-2 rounded-xl transition-colors ${isDragActive ? 'rounded-xl ring-2 ring-orange-500/70' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                <span className="block text-xs font-bold text-zinc-300">Ảnh sản phẩm</span>
                <label
                  htmlFor={fileInputId}
                  className={`flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed bg-zinc-950 px-4 py-3 text-xs font-bold transition-colors ${isDragActive ? 'border-orange-500 text-orange-300' : 'border-zinc-600 text-zinc-300 hover:border-orange-500 hover:text-orange-300'}`}
                >
                  <ImagePlus className="h-4 w-4" />
                  {isDragActive ? 'Thả ảnh vào đây' : 'Chọn ảnh JPG, PNG hoặc WEBP'}
                  <input
                    ref={fileInputRef}
                    id={fileInputId}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    disabled={pending || imageProcessing}
                    onChange={handleImageChange}
                    className="sr-only"
                  />
                </label>
                <span className="block text-[11px] text-zinc-500">Hoặc kéo ảnh từ thư mục thả vào đây · Tối đa 5 MB.</span>
              </div>
            </div>

            <label className="block space-y-2 text-xs font-bold text-zinc-300">
              <span>Hoặc dùng đường dẫn ảnh HTTPS</span>
              <input
                type="text"
                inputMode="url"
                maxLength={500}
                disabled={pending || imageProcessing || Boolean(imageFile)}
                placeholder="https://cdn.example.com/popcorn.webp"
                value={formState.imageUrl}
                onChange={event => {
                  updateField('imageUrl', event.target.value);
                  setPreviewFailed(false);
                }}
                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm text-white outline-none transition-colors focus:border-orange-500 disabled:opacity-45"
              />
            </label>

            <div className="flex min-h-28 items-center gap-4 rounded-2xl border border-zinc-800 bg-zinc-950/70 p-4">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-zinc-700 bg-zinc-900">
                {previewUrl && !previewFailed ? (
                  <img
                    src={previewUrl}
                    alt={`Xem trước ${formState.name || 'sản phẩm'}`}
                    onError={() => setPreviewFailed(true)}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <ImagePlus className="h-7 w-7 text-zinc-600" />
                )}
              </div>
              <div className="min-w-0">
                <p className="text-sm font-bold text-white">
                  {imageFile?.name || (previewUrl ? 'Ảnh đang sử dụng' : 'Chưa có ảnh sản phẩm')}
                </p>
                <p className="mt-1 text-xs leading-5 text-zinc-500">
                  Ảnh vuông hoặc tỷ lệ 4:3 sẽ hiển thị rõ nhất trên màn hình đặt hàng.
                </p>
                {imageFile && (
                  <button
                    type="button"
                    disabled={pending || imageProcessing}
                    onClick={removeSelectedImage}
                    className="mt-2 inline-flex items-center gap-1.5 text-xs font-bold text-red-400 hover:text-red-300"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Bỏ ảnh vừa chọn
                  </button>
                )}
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <label className="flex cursor-pointer items-start gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
                <input
                  type="checkbox"
                  checked={formState.active}
                  disabled={pending}
                  onChange={event => {
                    const active = event.target.checked;
                    setFormState(previous => ({
                      ...previous,
                      active,
                      sellable: active ? previous.sellable : false
                    }));
                    setFormError('');
                  }}
                  className="mt-0.5 h-4 w-4 accent-orange-500"
                />
                <span>
                  <span className="block text-xs font-black text-white">Sản phẩm đang hoạt động</span>
                  <span className="mt-1 block text-[11px] leading-4 text-zinc-500">
                    Bỏ chọn khi sản phẩm ngừng kinh doanh hoặc đang bảo trì dữ liệu.
                  </span>
                </span>
              </label>

              <label className={`flex items-start gap-3 rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4 ${formState.active ? 'cursor-pointer' : 'opacity-45'}`}>
                <input
                  type="checkbox"
                  checked={formState.sellable}
                  disabled={pending || !formState.active}
                  onChange={event => updateField('sellable', event.target.checked)}
                  className="mt-0.5 h-4 w-4 accent-orange-500"
                />
                <span>
                  <span className="block text-xs font-black text-white">Hiển thị cho khách đặt</span>
                  <span className="mt-1 block text-[11px] leading-4 text-zinc-500">
                    Chỉ sản phẩm hoạt động và được phép bán mới xuất hiện ở checkout.
                  </span>
                </span>
              </label>
            </div>

            {formError && (
              <div role="alert" className="rounded-2xl border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-300">
                {formError}
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3 border-t border-zinc-800 bg-zinc-950/40 px-6 py-4">
            <button
              type="button"
              disabled={pending}
              onClick={onClose}
              className="rounded-xl border border-zinc-700 px-4 py-3 text-xs font-black uppercase tracking-wider text-zinc-300 transition-colors hover:bg-zinc-800 disabled:opacity-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending || imageProcessing}
              className="flex items-center justify-center gap-2 rounded-xl bg-orange-500 px-4 py-3 text-xs font-black uppercase tracking-wider text-white transition-colors hover:bg-orange-600 disabled:cursor-wait disabled:opacity-70"
            >
              {(pending || imageProcessing) && <LoaderCircle className="h-4 w-4 animate-spin" />}
              {imageProcessing ? 'Đang xử lý ảnh...' : pending ? 'Đang lưu...' : 'Lưu sản phẩm'}
            </button>
          </div>
        </form>
      </section>
    </div>,
    document.body
  );
}
