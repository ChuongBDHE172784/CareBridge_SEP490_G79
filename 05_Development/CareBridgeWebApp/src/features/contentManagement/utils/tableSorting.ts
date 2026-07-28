import type { SortDirection } from '../components/SortableTableHeader';

export function nextSortDirection(
  activeKey: string,
  selectedKey: string,
  currentDirection: SortDirection,
): SortDirection {
  return activeKey === selectedKey && currentDirection === 'asc' ? 'desc' : 'asc';
}

export function sortRows<T>(
  rows: T[],
  direction: SortDirection,
  getValue: (row: T) => string | number | null | undefined,
): T[] {
  const collator = new Intl.Collator('vi', { numeric: true, sensitivity: 'base' });

  return [...rows].sort((left, right) => {
    const leftValue = getValue(left);
    const rightValue = getValue(right);
    const leftEmpty = leftValue === null || leftValue === undefined || leftValue === '';
    const rightEmpty = rightValue === null || rightValue === undefined || rightValue === '';
    if (leftEmpty && rightEmpty) return 0;
    if (leftEmpty) return 1;
    if (rightEmpty) return -1;

    const comparison = typeof leftValue === 'number' && typeof rightValue === 'number'
      ? leftValue - rightValue
      : collator.compare(String(leftValue), String(rightValue));
    return direction === 'asc' ? comparison : -comparison;
  });
}
