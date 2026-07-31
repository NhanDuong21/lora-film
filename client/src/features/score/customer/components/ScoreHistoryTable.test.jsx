import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ScoreHistoryTable from './ScoreHistoryTable';

const historyItem = {
  historyId: 1,
  transactionType: 'EARN',
  pointChange: 2,
  balanceAfter: 2,
  description: 'Earned points',
  occurredAt: '2026-07-30T07:19:00Z'
};

describe('ScoreHistoryTable pagination', () => {
  it('uses the page field returned by Score Service and can return to page one', () => {
    const onPageChange = vi.fn();

    render(
      <ScoreHistoryTable
        history={{
          content: [historyItem],
          page: 1,
          totalPages: 2,
          totalElements: 11
        }}
        isLoading={false}
        onPageChange={onPageChange}
      />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Trang 2 / 2');

    const previousButton = screen.getByRole('button', { name: /trang tr/i });
    const nextButton = screen.getByRole('button', { name: /trang sau/i });

    expect(previousButton).toBeEnabled();
    expect(nextButton).toBeDisabled();

    fireEvent.click(previousButton);

    expect(onPageChange).toHaveBeenCalledWith(0);
  });

  it('keeps compatibility with Spring Page responses that use number', () => {
    render(
      <ScoreHistoryTable
        history={{ content: [historyItem], number: 1, totalPages: 3 }}
        isLoading={false}
      />
    );

    expect(screen.getByRole('status')).toHaveTextContent('Trang 2 / 3');
  });
});
