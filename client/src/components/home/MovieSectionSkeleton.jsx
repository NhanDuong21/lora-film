export default function MovieSectionSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 px-6 md:px-12 py-10">
      {Array.from({ length: 8 }).map((_, index) => (
        <div key={index} className="w-full flex flex-col space-y-4">
          {/* Card aspect-[2/3] matching real card */}
          <div className="w-full aspect-[2/3] rounded-2xl border border-zinc-800/80 movie-skeleton" />
          
          {/* Text block skeletons */}
          <div className="space-y-2 px-1">
            {/* Age badge + Title row */}
            <div className="flex gap-2">
              <div className="h-4 w-10 rounded movie-skeleton" />
              <div className="h-4 w-3/4 rounded movie-skeleton" />
            </div>
            {/* Genre */}
            <div className="h-3 w-1/2 rounded movie-skeleton" />
            {/* Duration/Release Date */}
            <div className="h-3 w-2/3 rounded movie-skeleton" />
          </div>
        </div>
      ))}
    </div>
  );
}
