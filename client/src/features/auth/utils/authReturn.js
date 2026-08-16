const STORAGE_KEY = 'pending_auth_return';

const isSafePath = value => typeof value === 'string'
  && value.startsWith('/')
  && !value.startsWith('//')
  && !value.startsWith('/login')
  && !value.startsWith('/register')
  && !value.startsWith('/oauth2/redirect');

export const rememberAuthReturn = location => {
  if (typeof sessionStorage === 'undefined') return;
  const path = location?.pathname
    ? `${location.pathname}${location.search || ''}${location.hash || ''}`
    : '';

  if (isSafePath(path)) {
    sessionStorage.setItem(STORAGE_KEY, path);
  }
};

export const consumeAuthReturn = () => {
  if (typeof sessionStorage === 'undefined') return undefined;
  const path = sessionStorage.getItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
  if (!isSafePath(path)) return undefined;

  const url = new URL(path, 'http://localhost');
  return {
    pathname: url.pathname,
    search: url.search,
    hash: url.hash,
  };
};
