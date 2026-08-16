import { useParams } from 'react-router-dom';
import MovieDetailPage from './MovieDetailPage';

export default function MovieDetailRoute() {
  const { movieId } = useParams();
  return <MovieDetailPage key={movieId} />;
}
