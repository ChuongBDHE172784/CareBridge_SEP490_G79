import ContentTypeListPage from './ContentTypeListPage';

export default function FaqListPage() {
  return (
    <ContentTypeListPage
      type="FAQ"
      title="Quản lý FAQ"
      subtitle="Quản lý các câu hỏi thường gặp và câu trả lời cho người dùng"
      createLabel="Tạo FAQ Mới"
      emptyLabel="Không có FAQ nào."
    />
  );
}
