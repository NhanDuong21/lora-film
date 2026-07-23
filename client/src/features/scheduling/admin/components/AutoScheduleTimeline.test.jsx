import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AutoScheduleTimeline from './AutoScheduleTimeline';
import { buildSelectedItemsIndex } from '../utils/autoSchedulePreviewSelection';

const item = ({ id, start, end, occupancyEnd, selected = false }) => ({
  itemPublicId: id,
  movieTitle: id,
  auditoriumPublicId: 'aud-1',
  auditoriumName: 'Phòng 1',
  startTime: start,
  endTime: end,
  occupancyEndTime: occupancyEnd,
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  selected,
});

describe('AutoScheduleTimeline', () => {
  it('keeps the 08:00–24:00 axis and reports entirely out-of-range items', () => {
    const visible = item({
      id: 'Visible', start: '2026-07-24T10:00:00Z', end: '2026-07-24T12:00:00Z', occupancyEnd: '2026-07-24T12:15:00Z',
    });
    const beforeAxis = item({
      id: 'Before axis', start: '2026-07-24T01:00:00Z', end: '2026-07-24T03:00:00Z', occupancyEnd: '2026-07-24T03:15:00Z',
    });
    const items = [visible, beforeAxis];

    render(
      <AutoScheduleTimeline
        groupedItems={{ '2026-07-24': { 'Phòng 1': items } }}
        selectedItemIds={new Set()}
        selectedItemsIndex={buildSelectedItemsIndex(items, new Set())}
        handleToggleSelection={vi.fn()}
        isSelectionBusy={false}
        canSelect
        timezone="UTC"
      />,
    );

    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('24:00')).toBeInTheDocument();
    expect(screen.queryByText('00:00')).not.toBeInTheDocument();
    expect(screen.getByText('Visible')).toBeInTheDocument();
    expect(screen.queryByText('Before axis')).not.toBeInTheDocument();
    expect(screen.getByText(/1 suất nằm ngoài khung 08:00–24:00/)).toBeInTheDocument();
    expect(screen.getByTitle(/Visible/)).toHaveStyle({ left: '12.5%', width: '12.5%' });
  });

  it('uses occupancy conflicts from selected items outside the current date group', () => {
    const selectedPreviousDate = item({
      id: 'Previous', start: '2026-07-24T23:30:00Z', end: '2026-07-25T00:30:00Z', occupancyEnd: '2026-07-25T10:15:00Z', selected: true,
    });
    const candidate = item({
      id: 'Candidate', start: '2026-07-25T10:00:00Z', end: '2026-07-25T11:00:00Z', occupancyEnd: '2026-07-25T11:15:00Z',
    });
    const allItems = [selectedPreviousDate, candidate];

    render(
      <AutoScheduleTimeline
        groupedItems={{ '2026-07-25': { 'Phòng 1': [candidate] } }}
        selectedItemIds={new Set(['Previous'])}
        selectedItemsIndex={buildSelectedItemsIndex(allItems, new Set(['Previous']))}
        handleToggleSelection={vi.fn()}
        isSelectionBusy={false}
        canSelect
        timezone="UTC"
      />,
    );

    expect(screen.getByTitle(/Xung đột khoảng chiếm phòng/)).toHaveClass('cursor-not-allowed');
  });
});
