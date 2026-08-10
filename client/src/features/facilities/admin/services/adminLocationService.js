import axios from 'axios';

/**
 * Service to search for location suggestions using Nominatim OpenStreetMap API.
 */
export const searchLocationSuggestions = async ({ query, limit = 8, signal }) => {
  const trimmedQuery = query?.trim() || '';
  
  if (trimmedQuery.length < 2) {
    return { success: true, data: [] };
  }

    const response = await axios.get('https://nominatim.openstreetmap.org/search', {
      params: {
        q: trimmedQuery,
        format: 'json',
        addressdetails: 1,
        limit
      },
      headers: {
        'Accept-Language': 'vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7'
      },
      signal
    });

    const suggestions = response.data.map((item) => {
      const address = item.address || {};
      const city = address.city || address.province || address.state || address.municipality || address.town || '';
      const district = address.county || address.district || address.suburb || address.city_district || '';
      
      return {
        id: item.place_id.toString(),
        label: item.display_name,
        address: item.display_name,
        district: district,
        city: city,
        country: address.country || '',
        latitude: parseFloat(item.lat),
        longitude: parseFloat(item.lon)
      };
    });

    return { success: true, data: suggestions };
};
