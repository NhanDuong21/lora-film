import { useRef, useEffect, useState } from 'react';
import { useLocationAutocomplete } from '../hooks/useLocationAutocomplete';

export const CinemaLocationAutocomplete = ({
  value,
  onChange,
  onSelect,
  placeholder = 'Tìm kiếm địa chỉ, ví dụ: FPT University...',
  disabled = false,
  error: externalError,
  id = 'address'
}) => {
  const containerRef = useRef(null);
  
  const {
    query,
    setQuery,
    suggestions,
    isLoading,
    error: apiError,
    isOpen,
    setIsOpen
  } = useLocationAutocomplete({ minLength: 2, limit: 8 });

  const [activeIndex, setActiveIndex] = useState(-1);

  // Sync internal query with external value if external value changes (e.g. initial load)
  useEffect(() => {
    if (value !== undefined && value !== query) {
      setQuery(value || '');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setActiveIndex(-1);
  }, [suggestions]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [setIsOpen]);

  const handleChange = (e) => {
    const newValue = e.target.value;
    setQuery(newValue);
    if (onChange) {
      onChange(newValue);
    }
  };

  const handleSelect = (suggestion) => {
    setQuery(suggestion.address || suggestion.label || '');
    setIsOpen(false);
    if (onSelect) {
      onSelect(suggestion);
    }
  };

  const handleKeyDown = (e) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' && suggestions.length > 0) {
        setIsOpen(true);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex((prev) => (prev > 0 ? prev - 1 : 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (activeIndex >= 0 && activeIndex < suggestions.length) {
          handleSelect(suggestions[activeIndex]);
        }
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        break;
      case 'Tab':
        setIsOpen(false);
        break;
      default:
        break;
    }
  };

  const hasError = externalError || apiError;
  const showDropdown = isOpen && query.trim().length >= 2;

  return (
    <div className="relative w-full" ref={containerRef}>
      <input
        type="text"
        id={id}
        name={id}
        value={query}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onFocus={() => {
          if (query.trim().length >= 2) {
            setIsOpen(true);
          }
        }}
        placeholder={placeholder}
        disabled={disabled}
        className={`w-full px-4 py-2 bg-zinc-800 border ${
          hasError ? 'border-red-500' : 'border-zinc-700'
        } rounded-lg text-white focus:outline-none focus:border-brand-coral transition-colors`}
        role="combobox"
        aria-expanded={isOpen}
        aria-controls="location-suggestions"
        aria-autocomplete="list"
        autoComplete="off"
      />

      {showDropdown && (
        <div 
          id="location-suggestions"
          className="absolute z-50 w-full mt-1 bg-zinc-800 border border-zinc-700 rounded-lg shadow-xl max-h-60 overflow-y-auto"
          role="listbox"
        >
          {isLoading && (
            <div className="px-4 py-3 text-sm text-zinc-400 italic">
              Đang tìm địa chỉ...
            </div>
          )}
          
          {!isLoading && apiError && (
            <div className="px-4 py-3 text-sm text-red-400">
              {apiError}
            </div>
          )}
          
          {!isLoading && !apiError && suggestions.length === 0 && query.trim().length >= 2 && (
            <div className="px-4 py-3 text-sm text-zinc-400">
              Không tìm thấy địa chỉ phù hợp.
              <br/>
              <span className="text-xs">Bạn vẫn có thể nhập thủ công.</span>
            </div>
          )}
          
          {!isLoading && !apiError && suggestions.length > 0 && (
            <ul className="py-1">
              {suggestions.map((item, index) => {
                const label = item.label || item.address;
                return (
                  <li
                    key={item.id || index}
                    role="option"
                    aria-selected={activeIndex === index}
                    className={`px-4 py-2 cursor-pointer text-sm transition-colors ${
                      activeIndex === index ? 'bg-zinc-700 text-white' : 'text-zinc-300 hover:bg-zinc-700 hover:text-white'
                    }`}
                    onClick={() => handleSelect(item)}
                    onMouseEnter={() => setActiveIndex(index)}
                  >
                    <div className="font-medium truncate">{label}</div>
                    {(item.district || item.city) && (
                      <div className="text-xs text-zinc-500 truncate">
                        {[item.district, item.city].filter(Boolean).join(', ')}
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
};
