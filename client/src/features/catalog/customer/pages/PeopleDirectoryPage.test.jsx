import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import PeopleDirectoryPage from './PeopleDirectoryPage';
import { getPeople } from '@/features/catalog/customer/services/peopleService';

vi.mock('@/features/catalog/customer/services/peopleService', () => ({
  getPeople: vi.fn(),
}));

describe('PeopleDirectoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getPeople.mockResolvedValue({
      content: [{
        id: '11111111-1111-1111-1111-111111111111',
        slug: 'tom-hanks-11111111-1111-1111-1111-111111111111',
        name: 'Tom Hanks',
        roles: ['Diễn viên'],
        knownFor: ['Forrest Gump'],
      }],
      totalElements: 1,
      isLast: true,
    });
  });

  it('renders real API people as accessible profile links', async () => {
    render(<MemoryRouter><PeopleDirectoryPage role="ACTOR" /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Tom Hanks' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Tom Hanks/ })).toHaveAttribute(
      'href',
      '/nghe-si/tom-hanks-11111111-1111-1111-1111-111111111111',
    );
    expect(getPeople).toHaveBeenCalledWith(expect.objectContaining({
      role: 'ACTOR',
      availability: 'ALL',
      sort: 'POPULAR',
    }));
  });
});
