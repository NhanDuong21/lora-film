import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AutoScheduleTimeline from './AutoScheduleTimeline';
import {
  buildCandidateViewModels,
  TIMELINE_ZOOM_MODES,
} from '../utils/autoSchedulePreviewViewModel';

const item = ({
  id,
  serviceDate = '2026-07-24',
  start,
  end,
  occupancyEnd,
  auditoriumId = 'aud-1',
  auditoriumName = 'Phòng 1',
  moviePublicId = 'movie-1',
  selected = true,
  applyStatus = 'PENDING',
}) => ({
  itemPublicId: id,
  moviePublicId,
  movieTitle: id,
  movieVersionPublicId: `${moviePublicId}-version`,
  versionName: '2D',
  auditoriumPublicId: auditoriumId,
  auditoriumName,
  serviceDate,
  startTime: start,
  endTime: end,
  occupancyEndTime: occupancyEnd,
  validationStatus: 'VALID',
  applyStatus,
  selected,
});

const toViewModels = (items, selectedIds = items.filter(candidate => candidate.selected).map(candidate => candidate.itemPublicId)) => (
  buildCandidateViewModels(items, { selectedItemIds: new Set(selectedIds), timezone: 'UTC' })
);

const auditoriums = [
  { key: 'aud-1', publicId: 'aud-1', name: 'Phòng 1' },
  { key: 'aud-2', publicId: 'aud-2', name: 'Phòng 2' },
];

const TimelineHarness = ({ candidates, onOpenDetails = vi.fn() }) => {
  const [zoom, setZoom] = useState(TIMELINE_ZOOM_MODES.FIT);
  return (
    <AutoScheduleTimeline
      serviceDate="2026-07-24"
      candidates={candidates}
      auditoriums={auditoriums}
      zoomMode={zoom}
      onZoomChange={setZoom}
      onOpenDetails={onOpenDetails}
    />
  );
};

describe('AutoScheduleTimeline Milestone C', () => {
  it('builds a dynamic whole-hour axis through occupancy end and keeps overnight offsets beyond 24:00', () => {
    const candidates = toViewModels([
      item({
        id: 'Overnight',
        start: '2026-07-24T23:30:00Z',
        end: '2026-07-25T01:20:00Z',
        occupancyEnd: '2026-07-25T01:45:00Z',
      }),
    ]);

    render(<TimelineHarness candidates={candidates} />);

    expect(screen.getByText('23:00')).toBeInTheDocument();
    expect(screen.getByText('24:00')).toBeInTheDocument();
    expect(screen.getByText('25:00')).toBeInTheDocument();
    expect(screen.getByText('26:00')).toBeInTheDocument();
    expect(screen.queryByText('08:00')).not.toBeInTheDocument();
    expect(screen.getByTestId('timeline-candidate-Overnight')).toBeInTheDocument();
  });

  it('uses one aligned width for the ruler and every auditorium track in Fit and fixed zoom modes', () => {
    const candidates = toViewModels([
      item({ id: 'A', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z' }),
    ]);
    render(<TimelineHarness candidates={candidates} />);

    const root = screen.getByTestId('timeline-ruler').parentElement.parentElement;
    expect(root.parentElement).toHaveAttribute('data-zoom-mode', TIMELINE_ZOOM_MODES.FIT);
    expect(screen.getByTestId('timeline-ruler').style.width).toBe(screen.getByTestId('timeline-track-aud-1').style.width);
    expect(screen.getByTestId('timeline-track-aud-1').style.width).toBe(screen.getByTestId('timeline-track-aud-2').style.width);

    fireEvent.click(screen.getByRole('button', { name: '30 px/giờ' }));
    expect(root.parentElement).toHaveAttribute('data-zoom-mode', TIMELINE_ZOOM_MODES.COMPACT);
    expect(root.parentElement).toHaveAttribute('data-pixels-per-hour', '30.00');
    fireEvent.click(screen.getByRole('button', { name: '60 px/giờ' }));
    expect(root.parentElement).toHaveAttribute('data-pixels-per-hour', '60.00');
    fireEvent.click(screen.getByRole('button', { name: '120 px/giờ' }));
    expect(root.parentElement).toHaveAttribute('data-pixels-per-hour', '120.00');
  });

  it('separates the solid runtime from the hatched cleaning buffer', () => {
    const candidates = toViewModels([
      item({ id: 'Segments', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z' }),
    ]);
    render(<TimelineHarness candidates={candidates} />);

    expect(screen.getByTestId('runtime-segment-Segments')).toHaveStyle({ width: '80%' });
    expect(screen.getByTestId('cleaning-segment-Segments')).toHaveStyle({ width: '20%' });
    expect(screen.getByTestId('cleaning-segment-Segments').style.backgroundImage).toContain('repeating-linear-gradient');
    const legend = screen.getByRole('list', { name: 'Chú giải sơ đồ phòng chiếu' });
    expect(legend).toHaveTextContent('Thời lượng phim');
    expect(legend).toHaveTextContent('Thời gian dọn phòng');
    expect(legend).toHaveTextContent('Suất đã chọn');
    expect(legend).toHaveTextContent('Suất đang kiểm tra');
    expect(legend).toHaveTextContent('Không hợp lệ / trùng lịch');
  });

  it('keeps a stable movie palette and pairs state colors with visible markers', () => {
    const candidates = toViewModels([
      item({ id: 'First', moviePublicId: 'same-movie', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:10:00Z' }),
      item({ id: 'Second', moviePublicId: 'same-movie', auditoriumId: 'aud-2', auditoriumName: 'Phòng 2', start: '2026-07-24T12:00:00Z', end: '2026-07-24T13:00:00Z', occupancyEnd: '2026-07-24T13:10:00Z', applyStatus: 'CREATED' }),
    ]);
    render(<TimelineHarness candidates={candidates} />);

    const first = screen.getByTestId('timeline-candidate-First');
    const second = screen.getByTestId('timeline-candidate-Second');
    expect(first).toHaveAttribute('data-palette-index', second.getAttribute('data-palette-index'));
    expect(first).toHaveAttribute('data-state-marker', 'Đã chọn');
    expect(second).toHaveAttribute('data-state-marker', 'Đã tạo');
    expect(screen.getByText(/Đã tạo/)).toBeInTheDocument();
  });

  it('renders candidates as semantic buttons that only open details and preserves empty auditorium rows', () => {
    const onOpenDetails = vi.fn();
    const candidates = toViewModels([
      item({ id: 'Open me', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z' }),
    ]);
    render(<TimelineHarness candidates={candidates} onOpenDetails={onOpenDetails} />);

    const block = screen.getByRole('button', { name: /Open me.*Mở chi tiết/i });
    block.focus();
    fireEvent.click(block);
    expect(onOpenDetails).toHaveBeenCalledWith(candidates[0], block);
    expect(screen.getByTestId('timeline-track-aud-2')).toHaveTextContent('Chưa có suất đã chọn');
  });

  it('marks one diagnostic overlay with dashed styling and text', () => {
    const [candidate] = toViewModels([
      item({ id: 'Diagnostic', selected: false, applyStatus: 'CONFLICT', start: '2026-07-24T10:00:00Z', end: '2026-07-24T11:00:00Z', occupancyEnd: '2026-07-24T11:15:00Z' }),
    ], []);
    render(<TimelineHarness candidates={[{ ...candidate, diagnostic: true }]} />);

    const block = screen.getByTestId('timeline-candidate-Diagnostic');
    expect(block).toHaveAttribute('data-diagnostic', 'true');
    expect(block).toHaveAttribute('data-state-marker', 'Chẩn đoán');
    expect(block).toHaveClass('border-dashed');
  });
});
