import { cleanup, render, screen, fireEvent } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import RecommendationAudienceSelector from './RecommendationAudienceSelector';
import type { RecommendationTag } from '../models/content';

afterEach(cleanup);

const mockCatalog: RecommendationTag[] = [
  { id: 'bmi-normal', slug: 'rec-bmi-healthy-range', domain: 'BMI', label: 'Chỉ số BMI: Bình thường (18.5 - 22.9)' },
  { id: 'bmi-obesity', slug: 'rec-bmi-obesity', domain: 'BMI', label: 'Chỉ số BMI: Béo phì (≥ 25)' },
  { id: 'age-18-24', slug: 'rec-age-18-24', domain: 'AGE', label: 'Độ tuổi: 18 - 24' },
  { id: 'nutrition-iron', slug: 'rec-nutrition-iron-review', domain: 'NUTRITION', label: 'Dinh dưỡng: Đánh giá bổ sung Sắt' },
];

describe('RecommendationAudienceSelector', () => {
  it('renders group dropdown and item dropdown correctly', () => {
    render(
      <RecommendationAudienceSelector
        catalog={mockCatalog}
        selectedTagIds={[]}
        onChange={vi.fn()}
      />
    );

    expect(screen.getByLabelText('Nhóm tiêu chí')).toBeTruthy();
    expect(screen.getByLabelText('Giá trị cụ thể')).toBeTruthy();
    expect(screen.getByText('Chưa chọn tag đối tượng nào (Bài viết sẽ áp dụng cho tất cả người dùng trong giai đoạn).')).toBeTruthy();
  });

  it('allows selecting group and adding a tag', () => {
    const handleChange = vi.fn();
    render(
      <RecommendationAudienceSelector
        catalog={mockCatalog}
        selectedTagIds={[]}
        onChange={handleChange}
      />
    );

    // BMI group is selected by default
    const valueSelect = screen.getByLabelText('Giá trị cụ thể');
    fireEvent.change(valueSelect, { target: { value: 'bmi-normal' } });

    const addButton = screen.getByRole('button', { name: '+ Thêm' });
    fireEvent.click(addButton);

    expect(handleChange).toHaveBeenCalledWith(['bmi-normal']);
  });

  it('replaces exclusive group tag when another value in same group is added', () => {
    const handleChange = vi.fn();
    render(
      <RecommendationAudienceSelector
        catalog={mockCatalog}
        selectedTagIds={['bmi-normal']}
        onChange={handleChange}
      />
    );

    const valueSelect = screen.getByLabelText('Giá trị cụ thể');
    fireEvent.change(valueSelect, { target: { value: 'bmi-obesity' } });

    const addButton = screen.getByRole('button', { name: '+ Thêm' });
    fireEvent.click(addButton);

    expect(handleChange).toHaveBeenCalledWith(['bmi-obesity']);
  });

  it('allows removing selected tag via chip remove button', () => {
    const handleChange = vi.fn();
    render(
      <RecommendationAudienceSelector
        catalog={mockCatalog}
        selectedTagIds={['bmi-normal', 'age-18-24']}
        onChange={handleChange}
      />
    );

    const removeButton = screen.getByRole('button', { name: /Xoá tag Chỉ số BMI: Bình thường/i });
    fireEvent.click(removeButton);

    expect(handleChange).toHaveBeenCalledWith(['age-18-24']);
  });

  it('allows clearing all selected tags', () => {
    const handleChange = vi.fn();
    render(
      <RecommendationAudienceSelector
        catalog={mockCatalog}
        selectedTagIds={['bmi-normal', 'age-18-24']}
        onChange={handleChange}
      />
    );

    const clearButton = screen.getByRole('button', { name: 'Xoá tất cả' });
    fireEvent.click(clearButton);

    expect(handleChange).toHaveBeenCalledWith([]);
  });
});
