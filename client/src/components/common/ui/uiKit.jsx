import { useState, useEffect, useRef } from 'react';
import { Image as ImageIcon, Loader2, AlertCircle } from 'lucide-react';

export const LazyImage = ({ src, alt, className = '', containerClassName = '', ...props }) => {
  const [visible, setVisible] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!src) return;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        setVisible(true);
        observer.disconnect();
      }
    }, { rootMargin: '100px' });

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => observer.disconnect();
  }, [src]);

  return (
    <div ref={ref} className={`relative bg-zinc-900 overflow-hidden ${containerClassName}`}>
      {visible && src ? (
        <img
          src={src}
          alt={alt}
          className={`w-full h-full object-cover transition-opacity duration-300 opacity-100 ${className}`}
          loading="lazy"
          {...props}
        />
      ) : (
        <div className="w-full h-full flex items-center justify-center text-zinc-800 animate-pulse bg-zinc-900">
          <ImageIcon className="w-5 h-5" />
        </div>
      )}
    </div>
  );
};

export const Field = ({ label, required, error, children }) => (
  <div className="space-y-1">
    <label className="text-zinc-500 text-[10px] font-black uppercase tracking-wider block">
      {label}{required && <span className="text-[#ff7a1a] ml-0.5">*</span>}
    </label>
    {children}
    {error && <p className="text-red-400 text-[10px] mt-0.5">{error}</p>}
  </div>
);

export const Input = ({ className = '', ...props }) => (
  <input
    className={`w-full bg-[#050506] border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-[#ff7a1a]/40 transition-colors text-zinc-100 ${className}`}
    {...props}
  />
);

export const Select = ({ children, className = '', ...props }) => (
  <select
    className={`w-full bg-[#050506] border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-[#ff7a1a]/40 transition-colors text-zinc-100 outline-none ${className}`}
    {...props}
  >
    {children}
  </select>
);

export const Textarea = ({ className = '', ...props }) => (
  <textarea
    className={`w-full bg-[#050506] border border-zinc-800 rounded-xl py-2.5 px-3 text-xs focus:outline-none focus:border-[#ff7a1a]/40 transition-colors text-zinc-100 leading-relaxed ${className}`}
    {...props}
  />
);

export const LoadingState = ({ message = 'Đang tải dữ liệu...', className = '' }) => (
  <div className={`flex flex-col items-center justify-center py-12 text-zinc-500 ${className}`}>
    <Loader2 className="w-8 h-8 animate-spin mb-4 text-[#ff7a1a]" />
    <p className="text-sm">{message}</p>
  </div>
);

export const ErrorState = ({ message = 'Đã có lỗi xảy ra', onRetry = null, className = '' }) => (
  <div className={`flex flex-col items-center justify-center py-12 text-red-400/80 ${className}`}>
    <AlertCircle className="w-10 h-10 mb-4 opacity-50" />
    <p className="text-sm mb-4 text-center max-w-sm">{message}</p>
    {onRetry && (
      <button 
        onClick={onRetry}
        className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 rounded-lg text-xs transition-colors"
      >
        Thử lại
      </button>
    )}
  </div>
);

export const EmptyState = ({ message = 'Không có dữ liệu', icon: Icon = null, className = '' }) => (
  <div className={`flex flex-col items-center justify-center py-16 text-zinc-500 ${className}`}>
    {Icon ? <Icon className="w-12 h-12 mb-4 opacity-20" /> : <div className="w-12 h-12 mb-4 opacity-20 bg-zinc-900 rounded-full flex items-center justify-center border border-zinc-800" />}
    <p className="text-sm">{message}</p>
  </div>
);

export const AsyncState = ({ loading, error, onRetry, empty, emptyMessage, emptyIcon, children }) => {
  if (loading) return <LoadingState />;
  if (error) return <ErrorState message={typeof error === 'string' ? error : error?.message} onRetry={onRetry} />;
  if (empty) return <EmptyState message={emptyMessage} icon={emptyIcon} />;
  return children;
};
