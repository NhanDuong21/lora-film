import { useState, useEffect, useRef } from 'react';
import { Image as ImageIcon, Loader2, AlertCircle } from 'lucide-react';

export const LazyImage = ({ src, alt, className = '', containerClassName = '', ...props }) => {
  const [visible, setVisible] = useState(false);
  const [failedSrc, setFailedSrc] = useState('');
  const ref = useRef(null);

  useEffect(() => {
    if (!src) return undefined;
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

  const hasError = Boolean(src && failedSrc === src);

  return (
    <div ref={ref} className={`relative bg-zinc-900 overflow-hidden ${containerClassName}`}>
      {visible && src && !hasError ? (
        <img
          src={src}
          alt={alt}
          className={`w-full h-full object-cover transition-opacity duration-300 opacity-100 ${className}`}
          loading="lazy"
          onError={() => setFailedSrc(src)}
          {...props}
        />
      ) : (
        <div className={`w-full h-full flex flex-col items-center justify-center gap-2 bg-zinc-900 px-3 text-center ${hasError ? 'text-zinc-500' : 'text-zinc-800 animate-pulse'}`}>
          <ImageIcon className="w-5 h-5" />
          {hasError && <span className="text-[10px] font-medium">Không tải được ảnh</span>}
        </div>
      )}
    </div>
  );
};

export const Field = ({ label, required, error, children }) => (
  <div className="space-y-1.5">
    <label className="text-zinc-400 text-xs font-semibold block">
      {label}{required && <span className="text-brand-orange ml-1">*</span>}
    </label>
    {children}
    {error && (
      <p className="text-red-500 text-xs mt-1 flex items-center gap-1">
        <AlertCircle className="w-3 h-3" /> {error}
      </p>
    )}
  </div>
);

export const Input = ({ label, className = '', ...props }) => {
  const input = (
    <input
      className={`w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-sm focus-ring text-zinc-100 placeholder:text-zinc-500 transition-all ${className}`}
      {...props}
    />
  );
  return label ? (
    <label className="block space-y-1.5">
      <span className="block text-xs font-semibold text-zinc-400">{label}</span>
      {input}
    </label>
  ) : input;
};

export const Select = ({ label, children, className = '', ...props }) => {
  const select = (
    <select
      className={`w-full bg-zinc-900 border border-zinc-800 rounded-xl h-10 px-4 text-sm focus-ring text-zinc-100 outline-none transition-all ${className}`}
      {...props}
    >
      {children}
    </select>
  );
  return label ? (
    <label className="block space-y-1.5">
      <span className="block text-xs font-semibold text-zinc-400">{label}</span>
      {select}
    </label>
  ) : select;
};

export const Textarea = ({ label, className = '', ...props }) => {
  const textarea = (
    <textarea
      className={`w-full bg-zinc-900 border border-zinc-800 rounded-xl p-4 text-sm focus-ring text-zinc-100 leading-relaxed placeholder:text-zinc-500 transition-all ${className}`}
      {...props}
    />
  );
  return label ? (
    <label className="block space-y-1.5">
      <span className="block text-xs font-semibold text-zinc-400">{label}</span>
      {textarea}
    </label>
  ) : textarea;
};

export const SkeletonLoader = ({ className = '', type = 'rectangle' }) => {
  const baseClass = "animate-pulse bg-zinc-800/50";
  if (type === 'circle') {
    return <div className={`rounded-full ${baseClass} ${className}`} />;
  }
  return <div className={`rounded-xl ${baseClass} ${className}`} />;
};

export const LoadingState = ({ message = 'Đang tải dữ liệu...', className = '' }) => (
  <div role="status" aria-live="polite" className={`space-y-4 py-8 text-zinc-500 ${className}`}>
    <div className="flex items-center justify-center gap-3">
      <Loader2 className="w-6 h-6 animate-spin text-brand-orange" />
      <p className="text-sm font-medium">{message}</p>
    </div>
    <div className="space-y-3" aria-hidden="true">
      <SkeletonLoader className="h-12 w-full" />
      <SkeletonLoader className="h-12 w-full" />
      <SkeletonLoader className="h-12 w-4/5" />
    </div>
  </div>
);

export const ErrorState = ({ message = 'Đã có lỗi xảy ra', onRetry = null, className = '' }) => (
  <div className={`flex flex-col items-center justify-center py-12 text-red-500 ${className}`}>
    <div className="w-12 h-12 rounded-full bg-red-500/10 flex items-center justify-center mb-4">
      <AlertCircle className="w-6 h-6 text-red-500" />
    </div>
    <p className="text-sm mb-5 text-center max-w-sm font-medium text-zinc-300">{message}</p>
    {onRetry && (
      <button 
        onClick={onRetry}
        className="px-5 py-2.5 bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-200 rounded-xl text-xs font-bold uppercase tracking-wide transition-colors hover-scale"
      >
        Thử lại
      </button>
    )}
  </div>
);

export const EmptyState = ({ message = 'Không có dữ liệu', description = 'Không tìm thấy kết quả nào phù hợp với yêu cầu của bạn.', icon: Icon = null, action = null, className = '' }) => (
  <div className={`flex flex-col items-center justify-center py-16 px-4 text-center ${className}`}>
    <div className="w-16 h-16 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center mb-5">
      {Icon ? <Icon className="w-8 h-8 text-zinc-500" /> : <div className="w-8 h-8 opacity-20 bg-zinc-800 rounded-full" />}
    </div>
    <h3 className="text-lg font-bold text-zinc-200 mb-2">{message}</h3>
    <p className="text-sm text-zinc-500 max-w-sm mb-6">{description}</p>
    {action && (
      <div>{action}</div>
    )}
  </div>
);

export const StatusBadge = ({ status, label }) => {
  const getBadgeStyle = () => {
    switch(status?.toUpperCase()) {
      case 'PENDING':
      case 'DRAFT':
      case 'WAITING':
      case 'ONBOARDING':
      case 'ON_LEAVE':
        return 'bg-amber-500/10 border-amber-500/30 text-amber-500';
      case 'CONFIRMED':
      case 'COMPLETED':
      case 'ACTIVE':
      case 'PUBLISHED':
        return 'bg-emerald-500/10 border-emerald-500/30 text-emerald-500';
      case 'CANCELLED':
      case 'FAILED':
      case 'ERROR':
      case 'LOCKED':
      case 'BLOCKED':
      case 'SUSPENDED':
        return 'bg-red-500/10 border-red-500/30 text-red-500';
      case 'EXPIRED':
      case 'ARCHIVED':
      case 'INACTIVE':
      case 'DELETED':
      case 'RESIGNED':
        return 'bg-zinc-500/10 border-zinc-500/30 text-zinc-400';
      case 'REFUNDED':
        return 'bg-purple-500/10 border-purple-500/30 text-purple-400';
      case 'PREPARING':
        return 'bg-sky-500/10 border-sky-500/30 text-sky-400';
      default:
        return 'bg-zinc-800 border-zinc-700 text-zinc-300';
    }
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold border whitespace-nowrap ${getBadgeStyle()}`}>
      {label || status}
    </span>
  );
};

export const AsyncState = ({ loading, isLoading, error, onRetry, empty, emptyMessage, emptyDescription, emptyIcon, emptyAction, children }) => {
  if (loading ?? isLoading) return <LoadingState />;
  if (error) return <ErrorState message={typeof error === 'string' ? error : error?.message} onRetry={onRetry} />;
  if (empty) return <EmptyState message={emptyMessage} description={emptyDescription} icon={emptyIcon} action={emptyAction} />;
  return children;
};

