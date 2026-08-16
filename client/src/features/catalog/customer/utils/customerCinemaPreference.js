const STORAGE_KEY = 'lorafilm:preferred-cinema';

export const readPreferredCinema = () => {
  if (typeof window === 'undefined') return null;
  try {
    const value = window.sessionStorage.getItem(STORAGE_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
};

export const writePreferredCinema = cinema => {
  if (typeof window === 'undefined') return;
  try {
    if (!cinema?.publicId) {
      window.sessionStorage.removeItem(STORAGE_KEY);
      return;
    }
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(cinema));
  } catch {
    // The booking flow still works when storage is unavailable.
  }
};

