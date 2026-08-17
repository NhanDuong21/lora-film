import { useState, useEffect } from 'react';
import { useOutletContext, useNavigate } from 'react-router-dom';
import useAdminCinemas from '@/features/facilities/admin/hooks/useAdminCinemas';
import CinemaTable from '@/features/facilities/admin/components/CinemaTable';
import CinemaFormView from '@/features/facilities/admin/components/CinemaFormView';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

export default function AdminCinemaPage() {
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();
  const [view, setView] = useState('list'); // 'list' or 'create'

  const {
    cinemas,
    isLoading,
    searchTerm,
    setSearchTerm,
    cityFilter,
    setCityFilter,
    statusFilter,
    setStatusFilter,
    currentPage,
    setCurrentPage,
    pageSize,
    totalPages,
    totalElements,
    citiesList,
    fetchCinemas,
    handleStatusChange
  } = useAdminCinemas({ triggerToast });

  const handleCreateSubmit = async (formData) => {
    const res = await adminCinemaService.createCinema({
      name: formData.name,
      city: formData.city,
      district: formData.district,
      address: formData.address,
      latitude: formData.latitude,
      longitude: formData.longitude,
      timezone: formData.timezone,
      hotline: null,
      description: null,
    });

    if (res?.success && res?.data) {
      const createdCinema = res.data;
      triggerToast?.('Đã tạo bản nháp cụm rạp. Hãy tiếp tục hoàn thiện từng hạng mục thiết lập.');
      navigate(`/admin/cinemas/${createdCinema.publicId}?tab=setup&section=operating-hours`);
    }
  };

  useEffect(() => {
    if (view === 'list') {
      fetchCinemas();
    }
  }, [view, fetchCinemas]);

  if (view === 'create') {
    return (
      <CinemaFormView
        onCancel={() => setView('list')}
        onSubmit={handleCreateSubmit}
        triggerToast={triggerToast}
      />
    );
  }

  return (
    <CinemaTable
      cinemas={cinemas}
      isLoading={isLoading}
      searchTerm={searchTerm}
      setSearchTerm={setSearchTerm}
      cityFilter={cityFilter}
      setCityFilter={setCityFilter}
      statusFilter={statusFilter}
      setStatusFilter={setStatusFilter}
      currentPage={currentPage}
      setCurrentPage={setCurrentPage}
      pageSize={pageSize}
      totalPages={totalPages}
      totalElements={totalElements}
      citiesList={citiesList}
      onStatusChange={handleStatusChange}
      onOpenCreate={() => setView('create')}
      onEdit={(id) => {
        // Navigate to the detail page instead of opening an inline edit view
        navigate(`/admin/cinemas/${id}`);
      }}
    />
  );
}
