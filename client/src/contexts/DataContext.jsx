/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from 'react';

const DataContext = createContext();

export function DataProvider({ children }) {
  // TODO: Connect to live API endpoint GET /api/v1/movies
  const [movies, setMovies] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/events
  const [events, setEvents] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/cinemas
  const [cinemas, setCinemas] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/showtimes
  const [showtimes, setShowtimes] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/seats
  const [seats, setSeats] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/bookings
  const [bookings, setBookings] = useState([]);

  // TODO: Connect to live API endpoint GET /api/v1/dashboard/stats
  const [dashboardStats, setDashboardStats] = useState(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Keep these for compatibility with legacy components
  const [theaters, setTheaters] = useState([]);
  const [actors, setActors] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [concessions, setConcessions] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [employees, setEmployees] = useState([]);

  return (
    <DataContext.Provider value={{
      movies, setMovies,
      theaters, setTheaters,
      cinemas, setCinemas,
      events, setEvents,
      actors, setActors,
      showtimes, setShowtimes,
      tickets, setTickets,
      concessions, setConcessions,
      customers, setCustomers,
      employees, setEmployees,
      seats, setSeats,
      bookings, setBookings,
      dashboardStats, setDashboardStats,
      loading, setLoading,
      error, setError
    }}>
      {children}
    </DataContext.Provider>
  );
}

export function useData() {
  const context = useContext(DataContext);
  if (!context) {
    throw new Error('useData must be used within a DataProvider');
  }
  return context;
}
