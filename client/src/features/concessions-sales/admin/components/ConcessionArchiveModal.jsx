import { useEffect, useId, useRef } from 'react';
import { Archive, LoaderCircle, TriangleAlert, X } from 'lucide-react';
import { createPortal } from 'react-dom';

export default function ConcessionArchiveModal({
  item,
  onClose,
  onConfirm,
  pending = false
}) {
  const cancelButtonRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    cancelButtonRef.current?.focus();

    const handleKeyDown = event => {
      if (event.key === 'Escape' && !pending) onClose();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [onClose, pending]);

  return createPortal(
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onMouseDown={event => {
        if (event.target === event.currentTarget && !pending) onClose();
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="w-full max-w-md overflow-hidden rounded-3xl border border-red-500/25 bg-zinc-900 text-zinc-100 shadow-2xl shadow-black/70"
      >
        <div className="flex items-start justify-between gap-4 border-b border-zinc-800 px-6 py-5">
          <div className="flex items-start gap-3">
            <div className="rounded-2xl bg-red-500/10 p-3 text-red-400">
              <Archive className="h-6 w-6" />
            </div>
            <div>
              <h2 id={titleId} className="text-lg font-black text-white">Lưu trữ sản phẩm</h2>
              <p className="mt-1 text-xs font-semibold text-zinc-500">{item?.code} · {item?.name}</p>
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

        <div className="px-6 py-5">
          <div id={descriptionId} className="rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4">
            <div className="flex gap-3">
              <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" />
              <div>
                <p className="text-sm font-black text-amber-200">
                  Sản phẩm sẽ ngừng xuất hiện ở màn hình đặt hàng
                </p>
                <ul className="mt-2 list-disc space-y-1 pl-4 text-xs leading-5 text-zinc-400">
                  <li>Dữ liệu sản phẩm trong các đơn cũ vẫn được giữ nguyên.</li>
                  <li>Sản phẩm được chuyển vào bộ lọc “Đã lưu trữ”.</li>
                  <li>Admin có thể khôi phục sản phẩm sau này.</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 border-t border-zinc-800 bg-zinc-950/40 px-6 py-4">
          <button
            ref={cancelButtonRef}
            type="button"
            disabled={pending}
            onClick={onClose}
            className="rounded-xl border border-zinc-700 px-4 py-3 text-xs font-black uppercase tracking-wider text-zinc-300 transition-colors hover:bg-zinc-800 disabled:opacity-50"
          >
            Giữ sản phẩm
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={onConfirm}
            className="flex items-center justify-center gap-2 rounded-xl bg-red-500 px-4 py-3 text-xs font-black uppercase tracking-wider text-white transition-colors hover:bg-red-600 disabled:cursor-wait disabled:opacity-70"
          >
            {pending && <LoaderCircle className="h-4 w-4 animate-spin" />}
            {pending ? 'Đang lưu trữ...' : 'Xác nhận lưu trữ'}
          </button>
        </div>
      </section>
    </div>,
    document.body
  );
}
