/**
 * Normalizes Spring Boot Page response or standard array into a unified pagination object.
 * 
 * Expected Output:
 * {
 *   items: [],
 *   page: 0,
 *   size: 20,
 *   totalElements: 0,
 *   totalPages: 0,
 *   first: true,
 *   last: true
 * }
 */
export const normalizePagination = (response, fallbackSize = 20) => {
  // If response is null/undefined
  if (!response) {
    return {
      items: [],
      page: 0,
      size: fallbackSize,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    };
  }

  // Handle direct array
  if (Array.isArray(response)) {
    return {
      items: response,
      page: 0,
      size: response.length || fallbackSize,
      totalElements: response.length,
      totalPages: 1,
      first: true,
      last: true
    };
  }

  // Handle standard Spring Boot Page object (which might be inside response.data)
  // Prevent unwrapping if response.data is an array, as that means pagination metadata is at the response level
  const pageData = (response.data !== undefined && !Array.isArray(response.data)) ? response.data : response;

  if (pageData && Array.isArray(pageData.content)) {
    return {
      items: pageData.content,
      page: pageData.number || 0,
      size: pageData.size || fallbackSize,
      totalElements: pageData.totalElements || pageData.content.length,
      totalPages: pageData.totalPages || 1,
      first: pageData.first ?? (pageData.number === 0),
      last: pageData.last ?? true
    };
  }

  // Fallback if structure is unknown but has items/data array
  if (pageData && Array.isArray(pageData.items)) {
    return {
      items: pageData.items,
      page: pageData.page || 0,
      size: pageData.size || fallbackSize,
      totalElements: pageData.totalElements || pageData.items.length,
      totalPages: pageData.totalPages || 1,
      first: true,
      last: true
    };
  }

  // Handle LoraFilm custom PageResponse format where items are in .data and page is .pageNo
  if (pageData && Array.isArray(pageData.data)) {
    return {
      items: pageData.data,
      page: pageData.pageNo ?? pageData.page ?? 0,
      size: pageData.pageSize ?? pageData.size ?? fallbackSize,
      totalElements: pageData.totalElements ?? pageData.data.length,
      totalPages: pageData.totalPages ?? 1,
      first: pageData.first ?? ((pageData.pageNo ?? pageData.page) === 0),
      last: pageData.last ?? true
    };
  }

  // Final fallback
  return {
    items: [],
    page: 0,
    size: fallbackSize,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true
  };
};
