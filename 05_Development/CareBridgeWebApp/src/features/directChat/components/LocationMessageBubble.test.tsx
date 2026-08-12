import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import LocationMessageBubble from './LocationMessageBubble';
import type { TimelineItem } from '../models/timelineItem';

describe('LocationMessageBubble', () => {
  it('renders shared coordinates and a Google Maps directions link', () => {
    const item: TimelineItem = {
      kind: 'MESSAGE',
      messageId: 'location-1',
      messageType: 'LOCATION',
      locationLatitude: 10.7769,
      locationLongitude: 106.7009,
      locationLabel: 'Cổng bệnh viện',
    };

    render(<LocationMessageBubble item={item} isOwn={false} />);

    expect(screen.getByText('Cổng bệnh viện')).toBeTruthy();
    expect(screen.getByTitle('Bản đồ Cổng bệnh viện').getAttribute('src')).toContain(
      '10.7769%2C106.7009',
    );
    expect(
      screen.getByRole('link', { name: /Dẫn đường bằng Google Maps/i }).getAttribute('href'),
    ).toContain('destination=10.7769%2C106.7009');
  });

  it('shows a safe fallback for malformed coordinates', () => {
    render(
      <LocationMessageBubble
        item={{
          kind: 'MESSAGE',
          messageType: 'LOCATION',
          locationLatitude: 120,
          locationLongitude: 106.7009,
        }}
        isOwn={false}
      />,
    );

    expect(screen.getByText('Không thể đọc dữ liệu vị trí này')).toBeTruthy();
  });
});
