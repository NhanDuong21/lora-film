import { useState, useRef, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Search, X } from 'lucide-react';

export default function SearchableSelect({
  options = [], // { value, label, subtitle, badge, badgeColor, disabled }
  value,
  onChange,
  placeholder = 'Chọn một mục...',
  disabled = false,
  error = null,
  className = ''
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const containerRef = useRef(null);
  const dropdownRef = useRef(null);
  const [dropdownStyle, setDropdownStyle] = useState({});
  
  // Close on outside click
  useEffect(() => {
    const handleClickOutside = (event) => {
      const isOutsideContainer = containerRef.current && !containerRef.current.contains(event.target);
      const isOutsideDropdown = dropdownRef.current ? !dropdownRef.current.contains(event.target) : true;
      
      if (isOutsideContainer && isOutsideDropdown) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Handle portal positioning
  useEffect(() => {
    if (isOpen && containerRef.current) {
      const updatePosition = () => {
        if (!containerRef.current) return;
        const rect = containerRef.current.getBoundingClientRect();
        const spaceBelow = window.innerHeight - rect.bottom;
        const spaceAbove = rect.top;
        const dropdownHeight = 300; // estimated max height

        const shouldFlip = spaceBelow < dropdownHeight && spaceAbove > spaceBelow;

        setDropdownStyle({
          position: 'fixed',
          top: shouldFlip ? 'auto' : `${rect.bottom + 6}px`,
          bottom: shouldFlip ? `${window.innerHeight - rect.top + 6}px` : 'auto',
          left: `${rect.left}px`,
          width: `${rect.width}px`,
          zIndex: 50,
        });
      };

      updatePosition();
      window.addEventListener('scroll', updatePosition, true);
      window.addEventListener('resize', updatePosition);
      return () => {
        window.removeEventListener('scroll', updatePosition, true);
        window.removeEventListener('resize', updatePosition);
      };
    }
  }, [isOpen]);

  // Filter options
  const filteredOptions = useMemo(() => {
    if (!searchTerm) return options;
    const lower = searchTerm.toLowerCase();
    return options.filter(opt => 
      opt.label?.toLowerCase().includes(lower) || 
      opt.subtitle?.toLowerCase().includes(lower)
    );
  }, [options, searchTerm]);

  // Find selected option
  const selectedOption = useMemo(() => {
    if (!value) return null;
    return options.find(opt => opt.value === value) || null;
  }, [options, value]);

  const handleSelect = (val) => {
    onChange(val);
    setIsOpen(false);
    setSearchTerm('');
  };

  const handleClear = (e) => {
    e.stopPropagation();
    onChange('');
    setSearchTerm('');
  };

  return (
    <div className={`relative w-full ${className}`} ref={containerRef}>
      {/* Trigger Button */}
      <button
        type="button"
        disabled={disabled}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        className={`w-full flex items-center justify-between text-left px-3 py-2 bg-zinc-950 border ${error ? 'border-red-500' : 'border-zinc-800'} ${disabled ? 'opacity-50 cursor-not-allowed' : 'hover:border-zinc-700 cursor-pointer'} text-zinc-300 rounded-xl text-sm transition-colors focus:outline-none`}
      >
        <div className="flex-1 min-w-0 truncate pr-2">
          {selectedOption ? (
            <span className="text-white font-medium">{selectedOption.label}</span>
          ) : (
            <span className="text-zinc-500">{placeholder}</span>
          )}
        </div>
        <div className="flex items-center gap-1 shrink-0">
          {selectedOption && !disabled && (
            <div 
              onClick={handleClear}
              className="p-1 hover:bg-zinc-800 rounded-md transition-colors"
            >
              <X className="w-3.5 h-3.5 text-zinc-500 hover:text-zinc-300" />
            </div>
          )}
          <ChevronDown className={`w-4 h-4 text-zinc-500 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
        </div>
      </button>

      {/* Dropdown Menu */}
      {isOpen && createPortal(
        <div 
          ref={dropdownRef}
          style={dropdownStyle}
          className="bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl shadow-black/50 overflow-hidden animate-fade-in-up"
        >
          {/* Search Input */}
          <div className="p-2 border-b border-zinc-800 bg-zinc-900/90 sticky top-0 z-10 backdrop-blur-sm">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
              <input
                type="text"
                autoFocus
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Tìm kiếm..."
                className="w-full pl-9 pr-3 py-2 text-sm bg-zinc-950 border border-zinc-800 rounded-lg text-white placeholder-zinc-500 focus:outline-none focus:border-brand-orange/50 transition-colors"
              />
            </div>
          </div>

          {/* Options List */}
          <div className="max-h-60 overflow-y-auto custom-scrollbar p-1.5">
            {filteredOptions.length === 0 ? (
              <div className="p-3 text-center text-xs text-zinc-500">
                Không tìm thấy kết quả.
              </div>
            ) : (
              filteredOptions.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  disabled={opt.disabled}
                  onClick={() => handleSelect(opt.value)}
                  className={`w-full text-left px-3 py-2 rounded-lg transition-colors flex items-center justify-between gap-3 ${
                    opt.disabled ? 'opacity-50 cursor-not-allowed bg-zinc-950 text-zinc-500' :
                    value === opt.value ? 'bg-brand-orange/10 text-brand-orange' : 'hover:bg-zinc-800 text-zinc-300'
                  }`}
                >
                  <div className="flex-1 min-w-0">
                    <div className={`text-sm truncate font-medium ${opt.disabled ? 'text-zinc-500' : value === opt.value ? 'text-brand-orange' : 'text-zinc-200'}`}>
                      {opt.label}
                    </div>
                    {opt.subtitle && (
                      <div className={`text-[10px] truncate mt-0.5 ${opt.disabled ? 'text-zinc-600' : 'text-zinc-500'}`}>
                        {opt.subtitle}
                      </div>
                    )}
                  </div>
                  {opt.badge && (
                    <span className={`shrink-0 text-[9px] px-1.5 py-0.5 rounded font-black tracking-wider uppercase border opacity-70 ${opt.badgeColor || 'border-current'}`}>
                      {opt.badge}
                    </span>
                  )}
                </button>
              ))
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
