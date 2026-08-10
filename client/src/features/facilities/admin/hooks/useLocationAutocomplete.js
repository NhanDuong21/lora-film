import { useState, useEffect, useRef, useCallback } from 'react';
import axios from 'axios';
import { searchLocationSuggestions } from '../services/adminLocationService';
import { normalizeApiError } from '../../../../utils/apiErrorHandler';

export const useLocationAutocomplete = (options = {}) => {
  const {
    debounceTime = 400,
    minLength = 2,
    limit = 8
  } = options;

  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  
  const abortControllerRef = useRef(null);
  const requestSequenceRef = useRef(0);

  const clearSuggestions = useCallback(() => {
    setSuggestions([]);
    setIsOpen(false);
    setError(null);
  }, []);

  const search = useCallback(async (searchQuery) => {
    const trimmedQuery = searchQuery?.trim() || '';
    
    if (trimmedQuery.length < minLength) {
      clearSuggestions();
      return;
    }

    // Abort previous request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();
    
    requestSequenceRef.current += 1;
    const currentSequence = requestSequenceRef.current;

    setIsLoading(true);
    setError(null);
    setIsOpen(true);

    try {
      const response = await searchLocationSuggestions({
        query: trimmedQuery,
        limit,
        signal: abortControllerRef.current.signal
      });

      // Stale response protection
      if (currentSequence !== requestSequenceRef.current) {
        return;
      }

      if (response && response.success && Array.isArray(response.data)) {
        setSuggestions(response.data);
      } else {
        setSuggestions([]);
      }
    } catch (err) {
      // Ignore if canceled
      if (axios.isCancel(err) || err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
        return;
      }
      
      // Stale response protection for errors too
      if (currentSequence !== requestSequenceRef.current) {
        return;
      }

      const normalized = normalizeApiError(err);
      
      // Map specific location errors
      let mappedError = normalized.message;
      if (normalized.code === 'LOCATION_API_RATE_LIMITED') {
        mappedError = 'Dịch vụ địa chỉ đang giới hạn yêu cầu. Vui lòng thử lại sau.';
      } else if (normalized.code === 'LOCATION_API_TIMEOUT') {
        mappedError = 'Dịch vụ địa chỉ phản hồi chậm. Bạn có thể nhập địa chỉ thủ công.';
      } else if (['LOCATION_API_UNAVAILABLE', 'LOCATION_API_NOT_CONFIGURED'].includes(normalized.code)) {
        mappedError = 'Không thể kết nối dịch vụ địa chỉ.';
      }

      setError(mappedError);
      setSuggestions([]);
    } finally {
      if (currentSequence === requestSequenceRef.current) {
        setIsLoading(false);
      }
    }
  }, [limit, minLength, clearSuggestions]);

  useEffect(() => {
    if (query.trim().length === 0) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      clearSuggestions();
      return;
    }

    const timerId = setTimeout(() => {
      search(query);
    }, debounceTime);

    return () => {
      clearTimeout(timerId);
    };
  }, [query, debounceTime, search, clearSuggestions]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  return {
    query,
    setQuery,
    suggestions,
    isLoading,
    error,
    isOpen,
    setIsOpen,
    clearSuggestions,
    retry: () => search(query)
  };
};
