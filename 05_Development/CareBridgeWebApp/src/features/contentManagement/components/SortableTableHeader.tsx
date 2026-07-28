export type SortDirection = 'asc' | 'desc';

interface SortButtonProps {
  label: string;
  active: boolean;
  direction: SortDirection;
  onClick: () => void;
}

export function SortButton({ label, active, direction, onClick }: SortButtonProps) {
  const icon = active ? (direction === 'asc' ? 'arrow_upward' : 'arrow_downward') : 'unfold_more';

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={`Sắp xếp theo ${label}`}
      className={`inline-flex items-center gap-1 rounded-md px-1 py-0.5 text-[11px] font-semibold uppercase tracking-[0.05em] transition-colors hover:bg-surface-container-high focus:outline-none focus:ring-2 focus:ring-primary/40 ${
        active ? 'text-primary' : 'text-outline'
      }`}
    >
      <span>{label}</span>
      <span aria-hidden="true" className="material-symbols-outlined text-[15px] leading-none">{icon}</span>
    </button>
  );
}

interface SortableTableHeaderProps extends SortButtonProps {
  className?: string;
}

export function SortableTableHeader({ className = '', ...props }: SortableTableHeaderProps) {
  return (
    <th
      scope="col"
      aria-sort={props.active ? (props.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
      className={`px-2 py-3 text-left ${className}`}
    >
      <SortButton {...props} />
    </th>
  );
}
