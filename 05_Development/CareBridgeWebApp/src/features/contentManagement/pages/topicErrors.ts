interface TopicApiError {
  response?: {
    status?: number;
    data?: {
      error?: string;
      message?: string;
    };
  };
}

export function getTopicMutationErrorMessage(error: unknown): string {
  const apiError = error as TopicApiError;
  const status = apiError.response?.status;
  const code = apiError.response?.data?.error;

  if (status === 403) {
    return 'Tài khoản hiện tại không có quyền thực hiện thao tác này.';
  }
  if (status === 409 || code === 'COM-016') {
    return 'Không thể xoá vì mục này còn chủ đề con, câu hỏi hoặc người theo dõi.';
  }
  if (code === 'COM-017') {
    return 'Không thể thay đổi loại sau khi phân loại đã được tạo.';
  }
  if (code === 'COM-015') {
    return 'Danh mục cha không hợp lệ. Chủ đề phải thuộc một Danh mục đang hiển thị.';
  }
  if (code === 'COM-009') {
    return 'Tên phân loại này đã tồn tại. Vui lòng chọn tên khác.';
  }

  return apiError.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại.';
}
