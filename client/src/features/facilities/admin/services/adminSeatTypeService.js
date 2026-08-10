import apiClient from '@/services/apiClient';

const adminSeatTypeService = {
  // Get all seat types
  getAllSeatTypes: async () => {
    const response = await apiClient.get('/api/admin/seat-types');
    return response.data;
  },

  // Create a new seat type
  createSeatType: async (seatTypeData) => {
    const response = await apiClient.post('/api/admin/seat-types', seatTypeData);
    return response.data;
  },

  // Update a seat type
  updateSeatType: async (seatTypePublicId, seatTypeData) => {
    const response = await apiClient.put(`/api/admin/seat-types/${seatTypePublicId}`, seatTypeData);
    return response.data;
  }
};

export default adminSeatTypeService;
