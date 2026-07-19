import { useState, useEffect } from 'react';
import { useOutletContext, useNavigate } from 'react-router-dom';
import useAdminCinemas from '@/features/facilities/admin/hooks/useAdminCinemas';
import CinemaTable from '@/features/facilities/admin/components/CinemaTable';
import CinemaFormView from '@/features/facilities/admin/components/CinemaFormView';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';

export default function AdminCinemaPage() {
  const { triggerToast, triggerConfirm } = useOutletContext() || {};
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
  } = useAdminCinemas({ triggerConfirm, triggerToast });

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

    const normalizeTimeForApi = (value) => {
      if (!value) return null;
      if (value === "24:00" || value === "24:00:00") {
        return "23:59:59";
      }
      return value.length === 5 ? `${value}:00` : value;
    };

    if (res?.success && res?.data) {
      const createdCinema = res.data;
      
      // Save operating hours
      const operatingHoursPayload = operatingHours.map(oh => ({
        dayOfWeek: oh.dayOfWeek,
        openTime: oh.isClosed ? null : normalizeTimeForApi(oh.openTime),
        closeTime: oh.isClosed ? null : normalizeTimeForApi(oh.closeTime),
        isClosed: oh.isClosed
      }));

      try {
        await adminCinemaService.updateOperatingHours(createdCinema.publicId, operatingHoursPayload);
      } catch (ohErr) {
        console.error("Failed to save operating hours:", ohErr);
      }

      // Save media items
      const mediaRequests = [];
      
      const uploadAndGetUrl = async (mediaValue, type) => {
        if (!mediaValue) return null;
        if (typeof mediaValue === 'string') return mediaValue; // already a URL
        try {
          const res = await adminCinemaService.uploadCinemaMedia(mediaValue, type, createdCinema.publicId);
          return res.secureUrl;
        } catch (e) {
          console.error(`Failed to upload ${type}:`, e);
          return null;
        }
      };

      const logoUrl = await uploadAndGetUrl(media.logoUrl, 'LOGO');
      if (logoUrl) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'LOGO',
            url: logoUrl,
            title: 'Logo',
            displayOrder: 1,
            isPrimary: true
          })
        );
      }
      
      const bannerUrl = await uploadAndGetUrl(media.bannerUrl, 'BANNER');
      if (bannerUrl) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'BANNER',
            url: bannerUrl,
            title: 'Banner',
            displayOrder: 1,
            isPrimary: false
          })
        );
      }
      
      const mapUrl = await uploadAndGetUrl(media.mapImageUrl, 'MAP');
      if (mapUrl) {
        mediaRequests.push(
          adminCinemaService.createCinemaMedia(createdCinema.publicId, {
            mediaType: 'MAP',
            url: mapUrl,
            title: 'Map Layout',
            displayOrder: 1,
            isPrimary: false
          })
        );
      }
      
      if (Array.isArray(media.galleryUrls)) {
        for (let i = 0; i < media.galleryUrls.length; i++) {
          const galleryUrl = await uploadAndGetUrl(media.galleryUrls[i], 'GALLERY');
          if (galleryUrl) {
            mediaRequests.push(
              adminCinemaService.createCinemaMedia(createdCinema.publicId, {
                mediaType: 'GALLERY',
                url: galleryUrl,
                title: `Gallery Image ${i + 1}`,
                displayOrder: i + 1,
                isPrimary: false
              })
            );
          }
        }
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
