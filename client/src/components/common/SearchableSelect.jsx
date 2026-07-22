import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Search, X } from 'lucide-react';

const firstEnabledIndex = options => options.findIndex(option => !option.disabled);

export default function SearchableSelect({
  options = [],
  value,
  onChange,
  placeholder = 'Chọn một mục...',
  disabled = false,
  error = null,
  className = '',
  id,
  ariaLabel,
  ariaLabelledBy,
  ariaDescribedBy,
  ariaInvalid,
}) {
  const generatedId = useId();
  const inputId = id || `searchable-select-${generatedId}`;
  const listboxId = `${inputId}-listbox`;
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef(null);
  const dropdownRef = useRef(null);
  const inputRef = useRef(null);
  const optionRefs = useRef([]);
  const [dropdownStyle, setDropdownStyle] = useState({});

  const selectedOption = useMemo(
    () => options.find(option => option.value === value) || null,
    [options, value],
  );

  const filteredOptions = useMemo(() => {
    if (!searchTerm) return options;
    const lower = searchTerm.toLocaleLowerCase('vi');
    return options.filter(option =>
      option.label?.toLocaleLowerCase('vi').includes(lower)
      || option.subtitle?.toLocaleLowerCase('vi').includes(lower),
    );
  }, [options, searchTerm]);

  const closeDropdown = () => {
    setIsOpen(false);
    setSearchTerm('');
    setActiveIndex(-1);
  };

  const openDropdown = () => {
    if (disabled) return;
    setIsOpen(true);
    setSearchTerm('');
    const selectedIndex = filteredOptions.findIndex(option => option.value === value && !option.disabled);
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : firstEnabledIndex(filteredOptions));
  };

  useEffect(() => {
    const handleClickOutside = event => {
      const outsideContainer = containerRef.current && !containerRef.current.contains(event.target);
      const outsideDropdown = !dropdownRef.current || !dropdownRef.current.contains(event.target);
      if (outsideContainer && outsideDropdown) closeDropdown();
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (!isOpen || !containerRef.current) return undefined;

    const updatePosition = () => {
      if (!containerRef.current) return;
      const rect = containerRef.current.getBoundingClientRect();
      const spaceBelow = window.innerHeight - rect.bottom;
      const spaceAbove = rect.top;
      const shouldFlip = spaceBelow < 300 && spaceAbove > spaceBelow;
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
  }, [isOpen]);

  useEffect(() => {
    if (isOpen && activeIndex >= 0) {
      optionRefs.current[activeIndex]?.scrollIntoView?.({ block: 'nearest' });
    }
  }, [activeIndex, isOpen]);

  const moveActive = direction => {
    if (filteredOptions.length === 0) return;
    let next = activeIndex;
    for (let checked = 0; checked < filteredOptions.length; checked += 1) {
      next = (next + direction + filteredOptions.length) % filteredOptions.length;
      if (!filteredOptions[next].disabled) {
        setActiveIndex(next);
        return;
      }
    }
  };

  const handleSelect = option => {
    if (option.disabled) return;
    onChange(option.value);
    closeDropdown();
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  const handleKeyDown = event => {
    if (disabled) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!isOpen) {
        openDropdown();
      } else {
        moveActive(event.key === 'ArrowDown' ? 1 : -1);
      }
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      if (!isOpen) openDropdown();
      else if (activeIndex >= 0) handleSelect(filteredOptions[activeIndex]);
      return;
    }
    if (event.key === 'Escape' && isOpen) {
      event.preventDefault();
      closeDropdown();
    }
    if (event.key === 'Tab' && isOpen) closeDropdown();
  };

  const handleSearch = event => {
    const nextTerm = event.target.value;
    if (!isOpen) setIsOpen(true);
    setSearchTerm(nextTerm);
    const nextOptions = !nextTerm
      ? options
      : options.filter(option => {
          const lower = nextTerm.toLocaleLowerCase('vi');
          return option.label?.toLocaleLowerCase('vi').includes(lower)
            || option.subtitle?.toLocaleLowerCase('vi').includes(lower);
        });
    setActiveIndex(firstEnabledIndex(nextOptions));
  };

  const activeOptionId = isOpen && activeIndex >= 0
    ? `${listboxId}-option-${activeIndex}`
    : undefined;
  const displayValue = isOpen ? searchTerm : (selectedOption?.label || '');

  return (
    <div className={`relative w-full ${className}`} ref={containerRef}>
      <div className="relative">
        <input
          ref={inputRef}
          id={inputId}
          role="combobox"
          type="text"
          autoComplete="off"
          disabled={disabled}
          value={displayValue}
          placeholder={placeholder}
          aria-label={ariaLabel || (!ariaLabelledBy ? placeholder : undefined)}
          aria-labelledby={ariaLabelledBy}
          aria-describedby={ariaDescribedBy}
          aria-invalid={ariaInvalid ?? Boolean(error)}
          aria-autocomplete="list"
          aria-expanded={isOpen}
          aria-controls={listboxId}
          aria-activedescendant={activeOptionId}
          onClick={() => (isOpen ? undefined : openDropdown())}
          onChange={handleSearch}
          onKeyDown={handleKeyDown}
          className={`min-h-11 w-full rounded-xl border bg-zinc-950 py-2 pl-3 pr-16 text-sm text-zinc-200 outline-none transition-colors placeholder:text-zinc-500 focus:ring-2 focus:ring-brand-orange/60 ${error ? 'border-red-500' : 'border-zinc-800 hover:border-zinc-700'} ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
        />
        <div className="absolute inset-y-0 right-2 flex items-center gap-1">
          {selectedOption && !disabled && (
            <button
              type="button"
              aria-label={`Xóa lựa chọn ${selectedOption.label}`}
              onClick={() => {
                onChange('');
                closeDropdown();
                inputRef.current?.focus();
              }}
              className="rounded-md p-1 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-zinc-200 focus:outline-none focus:ring-2 focus:ring-brand-orange/60"
            >
              <X className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
          )}
          <ChevronDown className={`h-4 w-4 text-zinc-500 transition-transform ${isOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
        </div>
      </div>

      {isOpen && createPortal(
        <div
          ref={dropdownRef}
          style={dropdownStyle}
          className="overflow-hidden rounded-xl border border-zinc-800 bg-zinc-900 shadow-2xl shadow-black/50"
        >
          <div className="flex items-center gap-2 border-b border-zinc-800 px-3 py-2 text-xs text-zinc-500">
            <Search className="h-3.5 w-3.5" aria-hidden="true" />
            Nhập để tìm kiếm
          </div>
          <div id={listboxId} role="listbox" aria-label={ariaLabel || placeholder} className="max-h-60 overflow-y-auto p-1.5 custom-scrollbar">
            {filteredOptions.length === 0 ? (
              <div className="p-3 text-center text-xs text-zinc-500">Không tìm thấy kết quả.</div>
            ) : filteredOptions.map((option, index) => (
              <button
                ref={element => { optionRefs.current[index] = element; }}
                id={`${listboxId}-option-${index}`}
                key={option.value}
                type="button"
                role="option"
                aria-selected={value === option.value}
                aria-disabled={Boolean(option.disabled)}
                disabled={option.disabled}
                onMouseMove={() => !option.disabled && setActiveIndex(index)}
                onMouseDown={event => event.preventDefault()}
                onClick={() => handleSelect(option)}
                className={`flex w-full min-h-11 items-center justify-between gap-3 rounded-lg px-3 py-2 text-left outline-none transition-colors ${
                  option.disabled
                    ? 'cursor-not-allowed bg-zinc-950 text-zinc-500 opacity-50'
                    : activeIndex === index
                      ? 'bg-zinc-800 text-white ring-1 ring-brand-orange/40'
                      : value === option.value
                        ? 'bg-brand-orange/10 text-brand-orange'
                        : 'text-zinc-300 hover:bg-zinc-800'
                }`}
              >
                <span className="min-w-0 flex-1">
                  <span className={`block truncate text-sm font-medium ${value === option.value ? 'text-brand-orange' : 'text-zinc-200'}`}>
                    {option.label}
                  </span>
                  {option.subtitle && <span className="mt-0.5 block truncate text-[10px] text-zinc-500">{option.subtitle}</span>}
                </span>
                {option.badge && (
                  <span className={`shrink-0 rounded border px-1.5 py-0.5 text-[9px] font-black uppercase tracking-wider opacity-80 ${option.badgeColor || 'border-current'}`}>
                    {option.badge}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>,
        document.body,
      )}
    </div>
  );
}
