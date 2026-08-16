import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ScrollToTop from './ScrollToTop';

function NavigationHarness() {
  const location = useLocation();
  const navigate = useNavigate();
  return (
    <div>
      <p>{location.pathname}</p>
      <button type="button" onClick={() => navigate('/other')}>Đi tiếp</button>
      <button type="button" onClick={() => navigate(-1)}>Quay lại</button>
    </div>
  );
}

describe('ScrollToTop', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    Object.defineProperty(window, 'scrollX', { configurable: true, writable: true, value: 0 });
    Object.defineProperty(window, 'scrollY', { configurable: true, writable: true, value: 0 });
    vi.stubGlobal('requestAnimationFrame', callback => window.setTimeout(() => callback(Date.now()), 0));
    vi.stubGlobal('cancelAnimationFrame', timer => window.clearTimeout(timer));
    window.scrollTo = vi.fn(({ left = 0, top = 0 }) => {
      window.scrollX = left;
      window.scrollY = top;
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('restores the previous scroll position for browser Back navigation', async () => {
    render(
      <MemoryRouter initialEntries={[{ pathname: '/movie', key: 'movie-location' }]}>
        <ScrollToTop />
        <NavigationHarness />
      </MemoryRouter>
    );

    window.scrollY = 640;
    fireEvent.scroll(window);
    await vi.waitFor(() => {
      expect(window.sessionStorage.getItem('lorafilm:scroll-position:movie-location')).toContain('640');
    });

    fireEvent.click(screen.getByRole('button', { name: 'Đi tiếp' }));
    expect(await screen.findByText('/other')).toBeInTheDocument();
    expect(window.scrollY).toBe(0);

    fireEvent.click(screen.getByRole('button', { name: 'Quay lại' }));
    expect(await screen.findByText('/movie')).toBeInTheDocument();
    await vi.waitFor(() => expect(window.scrollY).toBe(640));
  });
});
