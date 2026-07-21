import { useState, useEffect, useMemo } from 'react';
import { Save } from 'lucide-react';
import RoomForm from '@/features/facilities/admin/components/RoomForm';

export default function AuditoriumOverviewTab({ auditorium, onUpdate }) {
  const [roomName, setRoomName] = useState('');
  const [screenType, setScreenType] = useState('STANDARD');
  const [soundType, setSoundType] = useState('STANDARD');
  const [cleaningBuffer, setCleaningBuffer] = useState(15);
  const [status, setStatus] = useState('DRAFT');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const computedCapacity = useMemo(() => {
    let capacity = 0;
    if (auditorium?.rows && Array.isArray(auditorium.rows)) {
      auditorium.rows.forEach(r => {
        if (r.seats && Array.isArray(r.seats)) {
          capacity += r.seats.filter(s => s.status !== 'INACTIVE').length;
        }
      });
    }
    return capacity;
  }, [auditorium]);

  useEffect(() => {
    if (auditorium) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setRoomName(auditorium.auditoriumName || '');
       
      setScreenType(auditorium.screenType || 'STANDARD');
       
      setSoundType(auditorium.soundType || 'STANDARD');
       
      setCleaningBuffer(auditorium.cleaningBufferMinutes || 15);
       
      setStatus(auditorium.auditoriumStatus || 'DRAFT');
    }
  }, [auditorium]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    await onUpdate({
      name: roomName,
      screenType,
      soundType,
      capacity: computedCapacity,
      cleaningBufferMinutes: cleaningBuffer,
      status
    });
    
    setIsSubmitting(false);
  };

  const availableStatuses = [
    { value: 'DRAFT', label: 'Bản nháp (DRAFT)' },
    { value: 'ACTIVE', label: 'Hoạt động (ACTIVE)' },
    { value: 'MAINTENANCE', label: 'Bảo trì (MAINTENANCE)' },
    { value: 'INACTIVE', label: 'Ngưng hoạt động (INACTIVE)' }
  ];

  return (
    <div className="max-w-2xl space-y-6 pb-20">
      <form onSubmit={handleSubmit} className="space-y-6">
        <RoomForm
          roomName={roomName}
          setRoomName={setRoomName}
          screenType={screenType}
          setScreenType={setScreenType}
          soundType={soundType}
          setSoundType={setSoundType}
          cleaningBuffer={cleaningBuffer}
          setCleaningBuffer={setCleaningBuffer}
          capacity={computedCapacity}
          status={status}
          setStatus={setStatus}
          availableStatuses={availableStatuses}
        />
        <div className="flex justify-end pt-4">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 bg-brand-orange hover:bg-opacity-90 text-white px-8 py-3 rounded-xl font-bold uppercase tracking-wider text-xs transition-colors shadow-lg shadow-brand-orange/20 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {isSubmitting ? 'ĐANG LƯU...' : 'LƯU THAY ĐỔI'}
          </button>
        </div>
      </form>
    </div>
  );
}
