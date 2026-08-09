import { useMemo } from 'react';
import MaintenanceWorkspace from '@/features/facilities/shared/components/MaintenanceWorkspace';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';

export default function AuditoriumMaintenanceTab({
  roomId,
  auditorium,
  triggerToast,
}) {
  const rooms = useMemo(() => auditorium ? [auditorium] : [], [auditorium]);

  return (
    <div className="mx-auto max-w-7xl pb-20">
      <MaintenanceWorkspace
        rooms={rooms}
        initialRoomId={roomId}
        loadWindows={async () => {
          const response = await adminRoomService.getMaintenanceWindows(roomId);
          return response?.success && Array.isArray(response.data) ? response.data : [];
        }}
        createWindow={async (_selectedRoomId, payload) => {
          const response = await adminRoomService.createMaintenanceWindow(roomId, payload);
          if (!response?.success) throw new Error(response?.message || 'Không thể lưu lịch bảo trì.');
          return response.data;
        }}
        cancelWindow={async item => {
          const response = await adminRoomService.cancelMaintenanceWindow(item.id);
          if (!response?.success) throw new Error(response?.message || 'Không thể hủy lịch bảo trì.');
          return response.data;
        }}
        previewImpact={async (_selectedRoomId, payload) => {
          const response = await adminRoomService.previewMaintenanceImpact(roomId, payload);
          if (!response?.success || !response.data) {
            throw new Error(response?.message || 'Không thể kiểm tra phạm vi ảnh hưởng.');
          }
          return response.data;
        }}
        viewerRole="admin"
        onNotify={triggerToast}
      />
    </div>
  );
}
