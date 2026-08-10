import { useState } from 'react';
import adminMovieService from '../services/adminMovieService';
import { getLifecycleErrorMessage } from '../utils/movieLifecycleErrorMessages';

export default function useMovieStatusTransition(moviePublicId, onSuccess) {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState(null);

  const transitionStatus = async (targetStatus, reason) => {
    setIsPending(true);
    setError(null);
    try {
      await adminMovieService.updateMovieStatus(moviePublicId, targetStatus, reason);
      if (onSuccess) {
        await onSuccess();
      }
      return true;
    } catch (err) {
      const errCode = err?.response?.data?.errorCode;
      const fallbackMsg = err?.response?.data?.message || err.message;
      setError(getLifecycleErrorMessage(errCode, fallbackMsg));
      return false;
    } finally {
      setIsPending(false);
    }
  };

  const resetError = () => setError(null);

  return {
    isPending,
    error,
    transitionStatus,
    resetError
  };
}
