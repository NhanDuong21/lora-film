import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import AutoScheduleCandidateDrawer from './AutoScheduleCandidateDrawer';
import { buildCandidateViewModels } from '../utils/autoSchedulePreviewViewModel';

const rawCandidate = (overrides = {}) => ({
  itemPublicId: 'item-1',
  moviePublicId: 'movie-1',
  movieTitle: 'Phim thử nghiệm',
  movieVersionPublicId: 'version-1',
  versionName: '2D phụ đề',
  auditoriumPublicId: 'aud-1',
  auditoriumName: 'Phòng 1',
  serviceDate: '2026-07-24',
  startTime: '2026-07-24T10:00:00Z',
  endTime: '2026-07-24T11:00:00Z',
  occupancyEndTime: '2026-07-24T11:15:00Z',
  validationStatus: 'VALID',
  applyStatus: 'PENDING',
  score: 92.5,
  rankingPosition: 2,
  scoreBreakdown: { demand: 40 },
  ...overrides,
});

const buildCandidate = (overrides, selected = false) => buildCandidateViewModels(
  [rawCandidate(overrides)],
  { selectedItemIds: new Set(selected ? ['item-1'] : []), timezone: 'UTC' },
)[0];

const editableCapabilities = {
  isEditable: true,
  canSelect: true,
};

const renderDrawer = ({
  candidate = buildCandidate(),
  capabilities = editableCapabilities,
  onToggleSelection = vi.fn(),
  canInspectOnTimeline = false,
  onShowDiagnostic = vi.fn(),
  onClearDiagnostic = vi.fn(),
  onClose = vi.fn(),
  returnFocusElement = null,
} = {}) => render(
  <MemoryRouter>
    <AutoScheduleCandidateDrawer
      candidate={candidate}
      timezone="UTC"
      capabilities={capabilities}
      selectionBlockedMessage=""
      onToggleSelection={onToggleSelection}
      canInspectOnTimeline={canInspectOnTimeline}
      onShowDiagnostic={onShowDiagnostic}
      onClearDiagnostic={onClearDiagnostic}
      onClose={onClose}
      returnFocusElement={returnFocusElement}
    />
  </MemoryRouter>,
);

describe('AutoScheduleCandidateDrawer', () => {
  it('is a named modal, moves focus inside, traps Tab, and closes with Escape', async () => {
    const onClose = vi.fn();
    renderDrawer({ onClose });

    const dialog = screen.getByRole('dialog', { name: 'Phim thử nghiệm' });
    const closeButton = screen.getByRole('button', { name: 'Đóng chi tiết ứng viên' });
    await waitFor(() => expect(closeButton).toHaveFocus());
    expect(dialog).toHaveAttribute('aria-modal', 'true');

    const technicalDetails = screen.getByText('Chi tiết kỹ thuật');
    technicalDetails.focus();
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(closeButton).toHaveFocus();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('restores focus to the opener when the drawer unmounts', async () => {
    const opener = document.createElement('button');
    document.body.appendChild(opener);
    opener.focus();
    const view = renderDrawer({ returnFocusElement: opener });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Đóng chi tiết ứng viên' })).toHaveFocus());

    view.unmount();
    expect(opener).toHaveFocus();
    opener.remove();
  });

  it('keeps selection on an explicit drawer control and does not dismiss from panel clicks', () => {
    const onClose = vi.fn();
    const onToggleSelection = vi.fn();
    renderDrawer({ onClose, onToggleSelection });

    fireEvent.mouseDown(screen.getByRole('dialog'));
    expect(onClose).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Chọn ứng viên' }));
    expect(onToggleSelection).toHaveBeenCalledWith('item-1', false);
  });

  it('shows created Showtime navigation and hides selection for read-only outcomes', () => {
    const candidate = buildCandidate({
      applyStatus: 'CREATED',
      createdShowtimePublicId: 'showtime-77',
    });
    renderDrawer({ candidate, capabilities: { isEditable: false, canSelect: false } });

    expect(screen.getByRole('link', { name: /showtime-77/i })).toHaveAttribute('href', '/admin/showtimes/showtime-77');
    expect(screen.queryByRole('button', { name: /Chọn ứng viên/i })).not.toBeInTheDocument();
    expect(screen.getByText('scoreBreakdown')).toBeInTheDocument();
  });

  it('can add or clear the single diagnostic overlay without toggling selection', () => {
    const onShowDiagnostic = vi.fn();
    const onClearDiagnostic = vi.fn();
    const onToggleSelection = vi.fn();
    const view = renderDrawer({
      canInspectOnTimeline: true,
      onShowDiagnostic,
      onClearDiagnostic,
      onToggleSelection,
    });

    fireEvent.click(screen.getByRole('button', { name: 'Xem trên timeline' }));
    expect(onShowDiagnostic).toHaveBeenCalledTimes(1);
    expect(onToggleSelection).not.toHaveBeenCalled();

    view.rerender(
      <MemoryRouter>
        <AutoScheduleCandidateDrawer
          candidate={{ ...buildCandidate(), diagnostic: true }}
          timezone="UTC"
          capabilities={editableCapabilities}
          selectionBlockedMessage=""
          onToggleSelection={onToggleSelection}
          canInspectOnTimeline
          onShowDiagnostic={onShowDiagnostic}
          onClearDiagnostic={onClearDiagnostic}
          onClose={vi.fn()}
          returnFocusElement={null}
        />
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Bỏ phủ chẩn đoán' }));
    expect(onClearDiagnostic).toHaveBeenCalledTimes(1);
  });
});
