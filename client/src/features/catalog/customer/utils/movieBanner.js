const uniqueUrls = urls => [...new Set(urls.filter(Boolean))];

export const getMovieBannerUrls = media => uniqueUrls(
  [...(media || [])]
    .filter(item => item?.mediaType === 'BANNER'
      && item.url
      && (!item.status || item.status === 'ACTIVE'))
    .sort((left, right) => {
      if (Boolean(left.isPrimary) !== Boolean(right.isPrimary)) {
        return left.isPrimary ? -1 : 1;
      }
      return (left.displayOrder ?? Number.MAX_SAFE_INTEGER)
        - (right.displayOrder ?? Number.MAX_SAFE_INTEGER);
    })
    .map(item => item.url)
);

export const getUniqueBannerUrls = urls => uniqueUrls(urls || []);
