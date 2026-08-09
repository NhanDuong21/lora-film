import apiClient from '@/services/apiClient';

const adminRoomService = {
  // Get all seat types
  getSeatTypes: async () => {
    const response = await apiClient.get('/api/admin/seat-types');
    return response.data;
  },

  // Get admin seating layout and auditorium details
  getAdminSeatLayout: async (auditoriumPublicId) => {
    const response = await apiClient.get(`/api/admin/auditoriums/${auditoriumPublicId}/seat-layout`);
    return response.data;
  },

  // Create a new auditorium room
  createAuditorium: async (cinemaPublicId, roomData) => {
    const response = await apiClient.post(`/api/admin/cinemas/${cinemaPublicId}/auditoriums`, roomData);
    return response.data;
  },

  cloneAuditoriumLayout: async (cinemaPublicId, targetAuditoriumPublicId, sourceAuditoriumPublicId) => {
    const response = await apiClient.post(
      `/api/admin/cinemas/${cinemaPublicId}/auditoriums/${targetAuditoriumPublicId}/clone`,
      { sourceAuditoriumPublicId }
    );
    return response.data;
  },

  // Update auditorium room details
  updateAuditorium: async (auditoriumPublicId, roomData) => {
    const response = await apiClient.put(`/api/admin/auditoriums/${auditoriumPublicId}`, roomData);
    return response.data;
  },

  // Delete an auditorium room
  deleteAuditorium: async (auditoriumPublicId) => {
    const response = await apiClient.delete(`/api/admin/auditoriums/${auditoriumPublicId}`);
    return response.data;
  },

  // Bulk create or update seats in an auditorium room
  bulkCreateSeats: async (auditoriumPublicId, seatsData) => {
    const response = await apiClient.post(`/api/admin/auditoriums/${auditoriumPublicId}/seats/bulk`, seatsData);
    return response.data;
  },

  // Create a maintenance window for an auditorium
  createMaintenanceWindow: async (auditoriumPublicId, windowData) => {
    const response = await apiClient.post(`/api/admin/auditoriums/${auditoriumPublicId}/maintenance-windows`, windowData);
    return response.data;
  },

  // Cancel a maintenance window
  cancelMaintenanceWindow: async (maintenanceWindowId) => {
    const response = await apiClient.put(`/api/admin/maintenance-windows/${maintenanceWindowId}/cancel`);
    return response.data;
  },

  // Get all maintenance windows for an auditorium
  getMaintenanceWindows: async (auditoriumPublicId) => {
    const response = await apiClient.get(`/api/admin/auditoriums/${auditoriumPublicId}/maintenance-windows`);
    return response.data;
  },

  previewMaintenanceImpact: async (auditoriumPublicId, windowData) => {
    const response = await apiClient.post(
      `/api/admin/auditoriums/${auditoriumPublicId}/maintenance-windows/impact-preview`,
      windowData,
    );
    return response.data;
  }
};

export default adminRoomService;
