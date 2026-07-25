const LOCAL_DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/;

const zonedParts = (instant, timezone) => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    calendar: 'gregory',
    numberingSystem: 'latn',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(instant));
  const values = Object.fromEntries(
    parts.filter(part => part.type !== 'literal').map(part => [part.type, Number(part.value)]),
  );
  return [values.year, values.month, values.day, values.hour, values.minute];
};

export const cinemaLocalDateTimeToInstant = (value, timezone) => {
  const match = LOCAL_DATE_TIME_PATTERN.exec(value || '');
  if (!match || !timezone?.trim()) {
    throw new Error('Cinema-local date/time and timezone are required');
  }
  const desired = match.slice(1).map(Number);
  const desiredAsUtc = Date.UTC(
    desired[0], desired[1] - 1, desired[2], desired[3], desired[4],
  );
  if (!Number.isFinite(desiredAsUtc)) {
    throw new Error('Cinema-local date/time is invalid');
  }

  let candidate = desiredAsUtc;
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const observed = zonedParts(candidate, timezone);
    const observedAsUtc = Date.UTC(
      observed[0], observed[1] - 1, observed[2], observed[3], observed[4],
    );
    candidate += desiredAsUtc - observedAsUtc;
  }

  const resolvedParts = zonedParts(candidate, timezone);
  if (!resolvedParts.every((part, index) => part === desired[index])) {
    throw new Error('Cinema-local date/time does not exist in the configured timezone');
  }
  return new Date(candidate).toISOString();
};
