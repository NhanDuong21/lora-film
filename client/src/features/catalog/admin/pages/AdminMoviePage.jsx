import React, { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import useAdminMovies from '@/features/catalog/admin/hooks/useAdminMovies';
import MovieTable from '@/features/catalog/admin/components/MovieTable';
import MovieDetailView from '@/features/catalog/customer/components/MovieDetailView';
import MovieFormModal from '@/features/catalog/admin/components/MovieFormModal';

export default function AdminMoviePage() {
  const { triggerToast } = useOutletContext() || {};

  // Orchestrator States
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedMovie, setSelectedMovie] = useState(null);

  // Hook for movie list management
  const adminMovies = useAdminMovies(triggerToast);

  const handleOpenAdd = () => {
    setSelectedMovie(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (movie) => {
    setSelectedMovie(movie);
    setIsFormOpen(true);
  };

  const handleOpenDetail = (movie) => {
    setSelectedMovie(movie);
    setIsDetailOpen(true);
  };

  if (isDetailOpen && selectedMovie) {
    return (
      <MovieDetailView
        movie={selectedMovie}
        onClose={() => setIsDetailOpen(false)}
        onOpenEdit={(movie) => {
          setIsDetailOpen(false);
          handleOpenEdit(movie);
        }}
      />
    );
  }

  if (isFormOpen) {
    return (
      <MovieFormModal
        selectedMovie={selectedMovie}
        genresList={adminMovies.genresList}
        setGenresList={adminMovies.setGenresList}
        triggerToast={triggerToast}
        onClose={() => setIsFormOpen(false)}
        onRefreshList={adminMovies.fetchMovies}
      />
    );
  }

  return (
    <MovieTable
      movies={adminMovies.movies}
      isLoading={adminMovies.isLoading}
      currentPage={adminMovies.currentPage}
      setCurrentPage={adminMovies.setCurrentPage}
      pageSize={adminMovies.pageSize}
      setPageSize={adminMovies.setPageSize}
      statusFilter={adminMovies.statusFilter}
      setStatusFilter={adminMovies.setStatusFilter}
      searchTerm={adminMovies.searchTerm}
      setSearchTerm={adminMovies.setSearchTerm}
      totalElements={adminMovies.totalElements}
      totalPages={adminMovies.totalPages}
      onOpenAdd={handleOpenAdd}
      onOpenDetail={handleOpenDetail}
      onOpenEdit={handleOpenEdit}
      onDelete={adminMovies.handleDelete}
    />
  );
}
