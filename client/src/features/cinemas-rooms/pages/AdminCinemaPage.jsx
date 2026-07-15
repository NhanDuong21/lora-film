import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import useAdminCinemas from '@/features/cinemas-rooms/hooks/useAdminCinemas';
import CinemaTable from '@/features/cinemas-rooms/components/CinemaTable';
import CinemaFormView from '@/features/cinemas-rooms/components/CinemaFormView';
import CinemaEditView from '@/features/cinemas-rooms/components/CinemaEditView';
import adminCinemaService from '@/features/cinemas-rooms/services/adminCinemaService';

export default function AdminCinemaPage() {
  const { triggerToast } = useOutletContext() || {};
  const [view, setView] = useState('list'); // 'list', 'create', or 'edit'
  const [selectedCinemaId, setSelectedCinemaId] = useState(null);

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
      
      // 1. Save operating hours
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

      // 2. Save media items
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
      setView('list');
      fetchCinemas();
    }
  };

  const handleEditSubmit = async (formData, operatingHours, media) => {
    // 1. Update general info
    const res = await adminCinemaService.updateCinema(selectedCinemaId, {
      name: formData.name,
      city: formData.city,
      district: formData.district,
      address: formData.address,
      latitude: formData.latitude,
      longitude: formData.longitude,
      timezone: formData.timezone,
      hotline: formData.hotline || null,
      description: formData.description || null,
      status: formData.status
    });

    if (res?.success) {
      try {
        await adminCinemaService.updateCinemaStatus(selectedCinemaId, formData.status);
      } catch (statusErr) {
        console.error("Failed to save cinema status update:", statusErr);
      }
      // 2. Update operating hours
      const operatingHoursPayload = operatingHours.map(oh => ({
        dayOfWeek: oh.dayOfWeek,
        openTime: oh.isClosed ? null : (oh.openTime.length === 5 ? `${oh.openTime}:00` : oh.openTime),
        closeTime: oh.isClosed ? null : (oh.closeTime.length === 5 ? `${oh.closeTime}:00` : oh.closeTime),
        isClosed: oh.isClosed
      }));

      try {
        await adminCinemaService.updateOperatingHours(selectedCinemaId, operatingHoursPayload);
      } catch (ohErr) {
        console.error("Failed to save operating hours:", ohErr);
      }

      // 3. Reconcile Media (Delete old media, then upload new)
      if (Array.isArray(media.originalMedia) && media.originalMedia.length > 0) {
        try {
          await Promise.all(media.originalMedia.map(m => adminCinemaService.deleteCinemaMedia(m.publicId)));
        } catch (delErr) {
          console.error("Failed to clear original media:", delErr);
        }
      }

      const mediaRequests = [];
      
      if (media.logoUrl && media.logoUrl.trim()) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(selectedCinemaId, {
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
          adminCinemaService.createCinemaMedia(selectedCinemaId, {
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
          adminCinemaService.createCinemaMedia(selectedCinemaId, {
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
              adminCinemaService.createCinemaMedia(selectedCinemaId, {
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
          console.error("Failed to save updated media items:", mediaErr);
        }
      }

      triggerToast?.('Đã cập nhật thông tin cụm rạp thành công!');
      setView('list');
      fetchCinemas();
    }
  };

  React.useEffect(() => {
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

  if (view === 'edit') {
    return (
      <CinemaEditView
        cinemaPublicId={selectedCinemaId}
        onCancel={() => setView('list')}
        onSubmit={handleEditSubmit}
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
        setSelectedCinemaId(id);
        setView('edit');
      }}
    />
  );
}
