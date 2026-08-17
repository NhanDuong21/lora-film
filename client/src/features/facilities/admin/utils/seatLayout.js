const SEAT_TYPES = new Set(['STANDARD', 'VIP', 'COUPLE', 'DISABLED']);

export function calculateRowLabel(rowIndex, skipIO = false) {
  let letterCode = 65;
  for (let index = 0; index < rowIndex; index++) {
    letterCode++;
    if (skipIO && (letterCode === 73 || letterCode === 79)) letterCode++;
  }
  if (skipIO && (letterCode === 73 || letterCode === 79)) letterCode++;
  return String.fromCharCode(letterCode);
}

export function generateAutoSeatMatrix({
  rows,
  cols,
  verticalAisle = 'CENTER',
  horizontalAisle = true,
  exitLeft = true,
  exitRight = true,
  strategy = 'AUTO',
}) {
  const matrix = Array.from({ length: rows }, () =>
    Array.from({ length: cols }, () => ({ type: 'STANDARD' })),
  );

  if (verticalAisle === 'CENTER' && cols > 0) {
    const middleColumn = Math.floor(cols / 2);
    for (let row = 0; row < rows; row++) matrix[row][middleColumn].type = 'AISLE';
  } else if (verticalAisle === 'TWO' && cols > 3) {
    const firstAisle = Math.floor(cols / 3);
    const secondAisle = Math.floor((cols * 2) / 3);
    for (let row = 0; row < rows; row++) {
      matrix[row][firstAisle].type = 'AISLE';
      matrix[row][secondAisle].type = 'AISLE';
    }
  }

  if (horizontalAisle && rows > 5) {
    for (let column = 0; column < cols; column++) matrix[4][column].type = 'AISLE';
  }

  if (exitLeft && rows > 0 && cols > 0) matrix[0][0].type = 'EXIT';
  if (exitRight && rows > 0 && cols > 1) matrix[0][cols - 1].type = 'EXIT';

  if (strategy !== 'AUTO') return matrix;

  const standardThreshold = Math.floor(rows * 0.3);
  const coupleThreshold = Math.floor(rows * 0.85);

  for (let row = 0; row < rows; row++) {
    if (row >= coupleThreshold) {
      // A couple group must contain exactly two physically adjacent seats. Pair
      // each uninterrupted block independently and leave an odd remainder as VIP.
      let column = 0;
      while (column < cols) {
        if (matrix[row][column].type !== 'STANDARD') {
          column++;
          continue;
        }
        if (column + 1 < cols && matrix[row][column + 1].type === 'STANDARD') {
          matrix[row][column].type = 'COUPLE';
          matrix[row][column + 1].type = 'COUPLE';
          column += 2;
        } else {
          matrix[row][column].type = 'VIP';
          column++;
        }
      }
    } else if (row >= standardThreshold) {
      for (let column = 0; column < cols; column++) {
        if (matrix[row][column].type === 'STANDARD') matrix[row][column].type = 'VIP';
      }
    }
  }

  // Keep the existing placement rule: wheelchair spaces are selected only from
  // the standard zone, never by overwriting VIP/couple seats or walkways.
  let wheelchairSeats = 0;
  const wheelchairRow = horizontalAisle && rows > 5 ? 5 : 0;
  for (let column = 0; column < cols && wheelchairSeats < 2; column++) {
    if (matrix[wheelchairRow]?.[column]?.type === 'STANDARD') {
      matrix[wheelchairRow][column].type = 'DISABLED';
      wheelchairSeats++;
    }
  }

  return matrix;
}

export function buildSeatItems({ matrix, rows, cols, skipIO = false, typeMapping }) {
  const seats = [];

  for (let row = 0; row < rows; row++) {
    const rowLabel = calculateRowLabel(row, skipIO);
    const pairGroups = new Map();
    let pairNumber = 1;

    for (let column = 0; column < cols; column++) {
      if (matrix[row]?.[column]?.type !== 'COUPLE') continue;

      if (column + 1 < cols && matrix[row]?.[column + 1]?.type === 'COUPLE') {
        const pairGroup = `${rowLabel}_P${pairNumber++}`;
        pairGroups.set(column, pairGroup);
        pairGroups.set(column + 1, pairGroup);
        column++;
      } else {
        throw new Error(
          `Ghế đôi ở hàng ${rowLabel}, cột ${column + 1} chưa có ghế đôi liền kề để tạo thành cặp.`,
        );
      }
    }

    let seatNumber = 1;
    for (let column = 0; column < cols; column++) {
      const cell = matrix[row]?.[column];
      if (!cell || cell.type === 'AISLE' || cell.type === 'EXIT' || cell.type === 'EMPTY') continue;
      if (!SEAT_TYPES.has(cell.type)) {
        throw new Error(`Loại ô "${cell.type}" không hợp lệ tại hàng ${rowLabel}, cột ${column + 1}.`);
      }

      const seatTypePublicId = typeMapping[cell.type];
      if (!seatTypePublicId) {
        throw new Error(`Loại ghế "${cell.type}" chưa được cấu hình trong hệ thống.`);
      }

      seats.push({
        seatTypePublicId,
        rowLabel,
        seatNumber,
        seatCode: `${rowLabel}${seatNumber}`,
        positionRow: row + 1,
        positionColumn: column + 1,
        pairGroup: pairGroups.get(column) || null,
        status: 'ACTIVE',
      });
      seatNumber++;
    }
  }

  return seats;
}
