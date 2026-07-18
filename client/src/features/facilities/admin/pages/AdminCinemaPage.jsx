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
    handleDeleteCinema,
    handleStatusChange
  } = useAdminCinemas(triggerToast);

  const handleCreateSubmit = async (formData, operatingHours, media) => {
    const res = await adminCinemaService.createCinema({
      name: formData.name,
      city: formData.city,
      district: formData.district,
      address: formData.address,
      latitude: formData.latitude,
      longitude: formData.longitude,
      timezone: formData.timezone,
      hotline: formData.hotline || null,
      description: formData.description || null
    });

    if (res?.success && res?.data) {
      const createdCinema = res.data;
      
      // Save operating hours
      const operatingHoursPayload = operatingHours.map(oh => ({
        dayOfWeek: oh.dayOfWeek,
        openTime: oh.isClosed ? null : (oh.openTime.length === 5 ? `${oh.openTime}:00` : oh.openTime),
        closeTime: oh.isClosed ? null : (oh.closeTime.length === 5 ? `${oh.closeTime}:00` : oh.closeTime),
        isClosed: oh.isClosed
      }));

      try {
        await adminCinemaService.updateOperatingHours(createdCinema.publicId, operatingHoursPayload);
      } catch (ohErr) {
        console.error("Failed to save operating hours:", ohErr);
      }

      // Save media items
      const mediaRequests = [];
      
      if (media.logoUrl && media.logoUrl.trim()) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'LOGO',
            url: media.logoUrl,
            title: 'Logo',
            displayOrder: 1,
            isPrimary: true
          })
        );
      }
      
      if (media.bannerUrl && media.bannerUrl.trim()) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'BANNER',
            url: media.bannerUrl,
            title: 'Banner',
            displayOrder: 1,
            isPrimary: false
          })
        );
      }
      
      if (media.mapImageUrl && media.mapImageUrl.trim()) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'MAP',
            url: media.mapImageUrl,
            title: 'Map Layout',
            displayOrder: 1,
            isPrimary: false
          })
        );
      }
      
      if (Array.isArray(media.galleryUrls)) {
        media.galleryUrls.forEach((url, index) => {
          if (url && url.trim()) {
            mediaRequests.push(
              adminCinemaService.createCinemaMedia(createdCinema.publicId, {
                mediaType: 'GALLERY',
                url: url,
                title: `Gallery Image ${index + 1}`,
                displayOrder: index + 1,
                isPrimary: false
              })
            );
          }
        });
      }

      if (mediaRequests.length > 0) {
        try {
          await Promise.all(mediaRequests);
        } catch (mediaErr) {
          console.error("Failed to save cinema media items:", mediaErr);
        }
      }

      triggerToast?.('Đã thêm cụm rạp mới thành công!');
      // Navigate to detail page of the new cinema
      navigate(`/admin/cinemas/${createdCinema.publicId}`);
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
      onDelete={handleDeleteCinema}
      onStatusChange={handleStatusChange}
      onOpenCreate={() => setView('create')}
      onEdit={(id) => {
        // Navigate to the detail page instead of opening an inline edit view
        navigate(`/admin/cinemas/${id}`);
      }}
    />
  );
}
