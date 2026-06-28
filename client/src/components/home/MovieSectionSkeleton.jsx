export default function MovieSectionSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 px-6 md:px-12 py-10">
      {Array.from({ length: 8 }).map((_, index) => (
        <div key={index} className="w-full flex flex-col space-y-4 animate-pulse">
          {/* Card aspect-[2/3] matching real card */}
          <div className="w-full aspect-[2/3] rounded-2xl bg-zinc-900 border border-zinc-800/80 flex items-center justify-center">
            {/* Visual element placeholder */}
            <div className="w-12 h-12 rounded-full bg-zinc-800" />
          </div>
          
          {/* Text block skeletons */}
          <div className="space-y-2">
            {/* Age badge + Title row */}
            <div className="flex gap-2">
              <div className="h-4 w-10 bg-zinc-800 rounded" />
              <div className="h-4 w-3/4 bg-zinc-800 rounded" />
            </div>
            {/* Genre */}
            <div className="h-3 w-1/2 bg-zinc-800 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}
