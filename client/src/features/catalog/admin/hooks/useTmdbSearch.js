export default function useTmdbSearch() {
  return {
    tmdbSearch: '', setTmdbSearch: () => {},
    tmdbSuggestions: [], setTmdbSuggestions: () => {},
    isTmdbSearching: false,
    showSuggestions: false, setShowSuggestions: () => {},
    tmdbLatestMovies: [],
    latestMoviesPageIndex: 0, setLatestMoviesPageIndex: () => {},
    isLatestMoviesLoading: false,
    slideOffset: 0, isTransitioning: false, slideLock: false,
    slideLeft: () => {}, slideRight: () => {}
  };
}