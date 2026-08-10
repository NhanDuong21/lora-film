/**
 * Format genres array to a comma-separated string
 * @param {Array} genres - Array of genre objects or strings
 * @returns {string} Comma-separated list of genres or 'Đang cập nhật'
 */
export const formatGenres = (genres) => {
  if (!genres || !Array.isArray(genres) || genres.length === 0) {
    return "Đang cập nhật";
  }
  return genres
    .map((g) => {
      if (typeof g === "string") return g;
      if (g && typeof g === "object" && g.genreName) return g.genreName;
      return null;
    })
    .filter(Boolean)
    .join(", ");
};

/**
 * Format duration in minutes to Vietnamese format
 * @param {number|string} durationMinutes - Duration in minutes
 * @returns {string} Formatted duration (e.g., '112 phút')
 */
export const formatDuration = (durationMinutes) => {
  if (!durationMinutes) {
    return "Đang cập nhật";
  }
  return `${durationMinutes} phút`;
};

/**
 * Format ISO date string (YYYY-MM-DD) to Vietnamese date format (DD/MM/YYYY)
 * @param {string} dateString - Date string from backend (e.g., '2026-06-28')
 * @returns {string} Formatted date (e.g., '28/06/2026')
 */
export const formatDate = (dateString) => {
  if (!dateString) {
    return "Đang cập nhật";
  }
  
  // Try splitting by dash YYYY-MM-DD
  const parts = dateString.split("-");
  if (parts.length === 3) {
    const [year, month, day] = parts;
    return `${day}/${month}/${year}`;
  }
  
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  } catch {
    return dateString;
  }
};

/**
 * Normalizes and provides label/class styling for age ratings
 * Supported ratings: P, K, T13, T16, T18
 * @param {string} rating - Age rating code
 * @returns {Object} Normalized rating and styling metadata
 */
export const getAgeRatingLabel = (rating) => {
  const cleanRating = rating ? rating.trim().toUpperCase() : "P";
  
  const ratingDetails = {
    P: {
      label: "P",
      description: "Phổ biến rộng rãi",
      bgClass: "bg-green-500/10 text-green-500 border-green-500/20",
    },
    K: {
      label: "K",
      description: "Dưới 13 tuổi cần người đi cùng",
      bgClass: "bg-blue-500/10 text-blue-500 border-blue-500/20",
    },
    T13: {
      label: "T13",
      description: "Trên 13 tuổi",
      bgClass: "bg-yellow-500/10 text-yellow-500 border-yellow-500/20",
    },
    T16: {
      label: "T16",
      description: "Trên 16 tuổi",
      bgClass: "bg-brand-orange/10 text-brand-orange border-brand-orange/20",
    },
    T18: {
      label: "T18",
      description: "Trên 18 tuổi",
      bgClass: "bg-red-500/10 text-red-500 border-red-500/20",
    },
  };

  return ratingDetails[cleanRating] || {
    label: cleanRating,
    description: `Phân loại ${cleanRating}`,
    bgClass: "bg-amber-500/10 text-amber-500 border-amber-500/20",
  };
};

/**
 * Extract YouTube ID and return a valid YouTube embed URL
 * @param {string} url - YouTube watch, share, or embed URL
 * @returns {string} Formatted YouTube embed URL
 */
export const getYoutubeEmbedUrl = (url) => {
  if (!url) return "";
  if (url.includes("embed/")) return url;
  
  let videoId = "";
  try {
    if (url.includes("youtu.be/")) {
      const parts = url.split("youtu.be/");
      if (parts[1]) {
        videoId = parts[1].split("?")[0].split("&")[0];
      }
    } else if (url.includes("v=")) {
      const parts = url.split("v=");
      if (parts[1]) {
        videoId = parts[1].split("&")[0];
      }
    } else if (url.includes("youtube.com/watch")) {
      const urlObj = new URL(url);
      videoId = urlObj.searchParams.get("v");
    } else if (url.includes("youtube.com/v/")) {
      const parts = url.split("/v/");
      if (parts[1]) {
        videoId = parts[1].split("?")[0];
      }
    }
  } catch {
    // Fallback if parsing fails
  }
  
  return videoId ? `https://www.youtube.com/embed/${videoId}` : url;
};

/**
 * Format ISO datetime string to display datetime
 * @param {string} dateString - ISO string (e.g. '2026-06-28T15:30:00Z')
 * @returns {string} Formatted string (e.g. '15:30 28/06/2026')
 */
export const formatDateTime = (dateString) => {
  if (!dateString) return "Đang cập nhật";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes} ${day}/${month}/${year}`;
  } catch {
    return dateString;
  }
};

/**
 * Convert API timestamp to HTML datetime-local input value
 * @param {string} dateString - ISO string
 * @returns {string} 'YYYY-MM-DDTHH:mm' format
 */
export const toDateTimeLocalValue = (dateString) => {
  if (!dateString) return "";
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "";
    
    // Convert to local time string matching 'YYYY-MM-DDTHH:mm'
    const offset = date.getTimezoneOffset() * 60000;
    const localISOTime = (new Date(date - offset)).toISOString().slice(0, 16);
    return localISOTime;
  } catch {
    return "";
  }
};

/**
 * Convert HTML datetime-local input value to API ISO string
 * @param {string} localValue - 'YYYY-MM-DDTHH:mm' format
 * @returns {string} ISO string
 */
export const fromDateTimeLocalValue = (localValue) => {
  if (!localValue) return null;
  try {
    const date = new Date(localValue);
    if (isNaN(date.getTime())) return null;
    return date.toISOString();
  } catch {
    return null;
  }
};

