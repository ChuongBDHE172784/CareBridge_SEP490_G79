import { describe, expect, it } from 'vitest';
import { getTopicMutationErrorMessage } from './topicErrors';

describe('getTopicMutationErrorMessage', () => {
  it('explains dependent conflicts in Vietnamese', () => {
    expect(getTopicMutationErrorMessage({ response: { status: 409, data: { error: 'COM-016' } } }))
      .toContain('chủ đề con, câu hỏi hoặc người theo dõi');
  });

  it('reuses the permission-denied message for 403 responses', () => {
    expect(getTopicMutationErrorMessage({ response: { status: 403 } }))
      .toBe('Tài khoản hiện tại không có quyền thực hiện thao tác này.');
  });

  it('surfaces immutable type errors from stale clients', () => {
    expect(getTopicMutationErrorMessage({ response: { data: { error: 'COM-017' } } }))
      .toContain('Không thể thay đổi loại');
  });
});
