import { describe, expect, it } from 'vitest';
import { nextSortDirection, sortRows } from './tableSorting';

describe('tableSorting', () => {
  it('sorts Vietnamese text alphabetically and keeps empty values last', () => {
    const rows = [{ name: 'Bé' }, { name: '' }, { name: 'Ánh' }];

    expect(sortRows(rows, 'asc', (row) => row.name).map((row) => row.name))
      .toEqual(['Ánh', 'Bé', '']);
  });

  it('sorts dates newest first for the default table order', () => {
    const rows = [{ updatedAt: 1 }, { updatedAt: 3 }, { updatedAt: 2 }];

    expect(sortRows(rows, 'desc', (row) => row.updatedAt).map((row) => row.updatedAt))
      .toEqual([3, 2, 1]);
  });

  it('toggles the selected column and starts a new column ascending', () => {
    expect(nextSortDirection('title', 'title', 'asc')).toBe('desc');
    expect(nextSortDirection('title', 'status', 'desc')).toBe('asc');
  });
});
