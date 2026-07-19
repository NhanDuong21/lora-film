import { useState, useEffect, useRef } from 'react';
import { Image as ImageIcon } from 'lucide-react';

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
