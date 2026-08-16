import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import FeaturedPeopleSection from './FeaturedPeopleSection';
import { getPeople } from '@/features/catalog/customer/services/peopleService';

vi.mock('@/features/catalog/customer/services/peopleService', () => ({ getPeople: vi.fn() }));

describe('FeaturedPeopleSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getPeople
      .mockResolvedValueOnce({ content: [{ id: 'actor-1', slug: 'actor-1', name: 'Diễn viên A', roles: ['Diễn viên'] }] })
      .mockResolvedValueOnce({ content: [{ id: 'director-1', slug: 'director-1', name: 'Đạo diễn B', roles: ['Đạo diễn'] }] });
  });

  it('alternates people attached to now-showing movies', async () => {
    render(<MemoryRouter><FeaturedPeopleSection /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Diễn viên A' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Đạo diễn B' })).toBeInTheDocument();
    expect(getPeople).toHaveBeenCalledWith(expect.objectContaining({ availability: 'NOW_SHOWING' }));
  });
});
