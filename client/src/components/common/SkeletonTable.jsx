

export default function SkeletonTable({ rows = 5, columns = 4 }) {
  return (
    <div className="bg-brand-gray/40 border border-zinc-800/40 rounded-2xl overflow-hidden w-full">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-brand-dark/80 border-b border-zinc-800">
              {Array.from({ length: columns }).map((_, colIdx) => (
                <th key={colIdx} className="py-4 px-6">
                  <div className="h-4 bg-zinc-800/60 rounded animate-pulse w-24"></div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: rows }).map((_, rowIdx) => (
              <tr key={rowIdx} className="border-b border-zinc-800/50">
                {Array.from({ length: columns }).map((_, colIdx) => (
                  <td key={colIdx} className="py-4 px-6">
                    <div className="h-4 bg-zinc-800/40 rounded animate-pulse w-full max-w-[200px]"></div>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
