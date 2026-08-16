import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PersonDetailPage from './PersonDetailPage';
import { getPerson } from '@/features/catalog/customer/services/peopleService';

vi.mock('@/features/catalog/customer/services/peopleService', () => ({
  getPerson: vi.fn(),
}));

describe('PersonDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getPerson.mockResolvedValue({
      id: 'person-1',
      slug: 'tom-hanks-person-1',
      name: 'Tom Hanks',
      biography: 'Tiểu sử nghệ sĩ.',
      roles: ['Diễn viên'],
      availableMovies: [{
        id: 'movie-1',
        slug: 'forrest-gump',
        title: 'Forrest Gump',
        availability: 'NOW_SHOWING',
        role: 'Diễn viên',
        characterName: 'Forrest',
      }],
      upcomingMovies: [],
      otherCredits: [],
    });
  });

  it('prioritizes movies currently available at LoraFilm', async () => {
    render(
      <MemoryRouter initialEntries={['/nghe-si/tom-hanks-person-1']}>
        <Routes>
          <Route path="/nghe-si/:personSlug" element={<PersonDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'Tom Hanks' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Đang có tại LoraFilm' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Xem suất chiếu' })).toHaveAttribute(
      'href',
      '/movies/forrest-gump#showtimes',
    );
  });
});
