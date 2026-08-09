import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data;

const managerCinemaService = {
  getAssignedCinemas: async () => unwrap(await apiClient.get('/api/manager/cinemas')) || [],

  getShowtimes: async params => unwrap(await apiClient.get('/api/manager/showtimes', { params })) || {
    data: [],
    pageNo: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
    last: true,
  },

  updateOperatingHours: async (cinemaPublicId, operatingHours) => unwrap(await apiClient.put(
    `/api/manager/cinemas/${cinemaPublicId}/operating-hours`,
    operatingHours,
  )),

  transitionShowtimeStatus: async (showtimePublicId, status, reason) => unwrap(await apiClient.put(
    `/api/manager/showtimes/${showtimePublicId}/status`,
    { status, reason: reason || null },
  )),

  getSeatControl: async showtimePublicId => unwrap(await apiClient.get(
    `/api/manager/showtimes/${showtimePublicId}/seat-control`,
  )),

  blockSeats: async (showtimePublicId, seatPublicIds, reason) => unwrap(await apiClient.post(
    `/api/manager/showtimes/${showtimePublicId}/blocked-seats`,
    { seatPublicIds, reason },
  )),

  releaseBlockedSeats: async (showtimePublicId, seatPublicIds, reason) => unwrap(await apiClient.put(
    `/api/manager/showtimes/${showtimePublicId}/blocked-seats/release`,
    { seatPublicIds, reason },
  )),

  getMaintenanceWindows: async cinemaPublicId => unwrap(await apiClient.get(
    `/api/manager/cinemas/${cinemaPublicId}/maintenance-windows`,
  )) || [],

  createMaintenanceWindow: async (cinemaPublicId, auditoriumPublicId, payload) => unwrap(await apiClient.post(
    `/api/manager/cinemas/${cinemaPublicId}/auditoriums/${auditoriumPublicId}/maintenance-windows`,
    payload,
  )),

  previewMaintenanceImpact: async (cinemaPublicId, auditoriumPublicId, payload) => unwrap(await apiClient.post(
    `/api/manager/cinemas/${cinemaPublicId}/auditoriums/${auditoriumPublicId}/maintenance-windows/impact-preview`,
    payload,
  )),

  cancelMaintenanceWindow: async (cinemaPublicId, maintenanceWindowId) => unwrap(await apiClient.put(
    `/api/manager/cinemas/${cinemaPublicId}/maintenance-windows/${maintenanceWindowId}/cancel`,
  )),

  resolveMaintenanceWindow: async (cinemaPublicId, maintenanceWindowId, payload) => unwrap(await apiClient.put(
    `/api/manager/cinemas/${cinemaPublicId}/maintenance-windows/${maintenanceWindowId}/resolve`,
    payload,
  )),

  extendMaintenanceWindow: async (cinemaPublicId, maintenanceWindowId, payload) => unwrap(await apiClient.put(
    `/api/manager/cinemas/${cinemaPublicId}/maintenance-windows/${maintenanceWindowId}/extend`,
    payload,
  )),

  getStaff: async cinemaPublicId => unwrap(await apiClient.get('/api/users/manager/staff', {
    params: { cinemaPublicId },
  })) || [],

  getShifts: async params => unwrap(await apiClient.get('/api/users/manager/shifts', { params })) || [],

  getAttendance: async params => unwrap(await apiClient.get('/api/users/manager/attendance', { params })) || [],

  getLeaveRequests: async params => unwrap(await apiClient.get('/api/users/manager/leave-requests', { params })) || [],

  createShift: async (cinemaPublicId, payload) => unwrap(await apiClient.post(
    '/api/users/manager/shifts', payload, { params: { cinemaPublicId } },
  )),

  cancelShift: async (cinemaPublicId, shiftId, payload) => unwrap(await apiClient.post(
    `/api/users/manager/shifts/${shiftId}/cancel`, payload, { params: { cinemaPublicId } },
  )),

  reviewLeave: async (cinemaPublicId, leaveId, payload) => unwrap(await apiClient.post(
    `/api/users/manager/leave-requests/${leaveId}/actions`, payload, { params: { cinemaPublicId } },
  )),

  getCinemaReport: async params => unwrap(await apiClient.get('/api/analytics/dashboard', { params })),
};

export default managerCinemaService;
