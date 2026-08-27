import type { TimelineItem } from '../models/timelineItem';

interface LocationMessageBubbleProps {
  item: TimelineItem;
  isOwn: boolean;
  onRecall?: (messageId: string) => void;
}

function validCoordinate(latitude?: number, longitude?: number): boolean {
  return latitude !== undefined
    && longitude !== undefined
    && Number.isFinite(latitude)
    && Number.isFinite(longitude)
    && latitude >= -90
    && latitude <= 90
    && longitude >= -180
    && longitude <= 180;
}

export default function LocationMessageBubble({ item, isOwn, onRecall }: LocationMessageBubbleProps) {
  const latitude = item.locationLatitude;
  const longitude = item.locationLongitude;
  const label = item.locationLabel?.trim() || 'Vị trí được chia sẻ';

  if (!validCoordinate(latitude, longitude)) {
    return (
      <div className="flex items-center gap-2 text-sm">
        <span className="material-symbols-outlined">location_off</span>
        Không thể đọc dữ liệu vị trí này
      </div>
    );
  }

  const coordinate = `${latitude},${longitude}`;
  const mapUrl = `https://maps.google.com/maps?q=${encodeURIComponent(coordinate)}&z=15&output=embed`;
  const directionsUrl = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(coordinate)}&travelmode=driving`;

  return (
    <article className="group/location relative w-[min(320px,68vw)] overflow-hidden rounded-2xl bg-white text-[#5A463F] shadow-[0_12px_32px_rgba(90,70,63,0.12)]">
      <div className="relative h-36 overflow-hidden bg-[#F2EAE4]">
        <iframe
          title={`Bản đồ ${label}`}
          src={mapUrl}
          className="h-full w-full border-0"
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
        />
        <span className="pointer-events-none absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1.5 text-[11px] font-bold text-[#845143] shadow-sm backdrop-blur">
          <span className="material-symbols-outlined text-sm">location_on</span>
          Vị trí trực tiếp
        </span>
      </div>

      <div className="p-4">
        <h3 className="m-0 truncate text-sm font-bold text-[#5A463F]">{label}</h3>
        <p className="mb-3 mt-1 text-[11px] text-[#9C857C]">
          {latitude!.toFixed(6)}, {longitude!.toFixed(6)}
        </p>
        <a
          href={directionsUrl}
          target="_blank"
          rel="noreferrer"
          className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-full bg-[#C98C7B] px-4 text-sm font-bold text-white no-underline transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#B67868] active:scale-95"
        >
          <span className="material-symbols-outlined text-lg">navigation</span>
          Dẫn đường bằng Google Maps
        </a>
      </div>

      {isOwn && item.messageId && onRecall && (
        <button
          type="button"
          onClick={() => onRecall(item.messageId!)}
          className="absolute right-3 top-3 hidden h-8 w-8 items-center justify-center rounded-full border-0 bg-[#5A463F]/85 text-white shadow-md group-hover/location:flex"
          title="Thu hồi vị trí"
        >
          <span className="material-symbols-outlined text-base">undo</span>
        </button>
      )}
    </article>
  );
}
