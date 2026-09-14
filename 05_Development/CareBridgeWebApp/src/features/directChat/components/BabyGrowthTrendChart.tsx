import React from 'react';

export interface BabyGrowthTrendPoint {
  date: string;
  value: number;
}

interface Props {
  points: BabyGrowthTrendPoint[];
  unit: string;
  color: string;
}

const WIDTH = 280;
const HEIGHT = 96;
const PAD_X = 12;
const PAD_Y = 14;

/** Inline-SVG trend line (no chart dependency), mirroring the mobile growth chart. */
export const BabyGrowthTrendChart: React.FC<Props> = ({ points, unit, color }) => {
  const times = points.map((p) => Date.parse(p.date));
  const values = points.map((p) => p.value);
  const minTime = Math.min(...times);
  const timeRange = Math.max(...times) - minTime;
  const minVal = Math.min(...values);
  const maxVal = Math.max(...values);
  const valueRange = Math.max(maxVal - minVal, 1);
  const center = (minVal + maxVal) / 2;
  const displayMin = center - valueRange / 2;

  const coords = points.map((p, i) => {
    const x =
      points.length === 1 || timeRange === 0
        ? WIDTH / 2
        : PAD_X + ((times[i] - minTime) / timeRange) * (WIDTH - PAD_X * 2);
    const y = HEIGHT - PAD_Y - ((p.value - displayMin) / valueRange) * (HEIGHT - PAD_Y * 2);
    return { x, y };
  });
  const polyline = coords.map((c) => `${c.x.toFixed(1)},${c.y.toFixed(1)}`).join(' ');
  const last = coords[coords.length - 1];

  return (
    <svg
      data-testid="baby-growth-chart"
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      className="w-full h-24"
      role="img"
      aria-label={`Biểu đồ xu hướng (${unit})`}
    >
      {coords.length > 1 && (
        <polyline points={polyline} fill="none" stroke={color} strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
      )}
      {coords.map((c, i) => (
        <circle key={i} cx={c.x} cy={c.y} r={i === coords.length - 1 ? 4 : 2.5} fill={color} />
      ))}
      {last && (
        <text
          x={Math.min(Math.max(last.x, 30), WIDTH - 30)}
          y={Math.max(last.y - 8, 10)}
          textAnchor="middle"
          fontSize={10}
          fontWeight={700}
          fill={color}
        >
          {`${values[values.length - 1].toFixed(1)} ${unit}`}
        </text>
      )}
    </svg>
  );
};
