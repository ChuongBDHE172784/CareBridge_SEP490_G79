import ContentTypeListPage from './ContentTypeListPage';

export default function ArticleListPage() {
  return (
    <ContentTypeListPage
      type="ARTICLE"
      title="Quản lý Bài viết"
      subtitle="Quản lý các bài viết nội dung cho mẹ và gia đình"
      createLabel="Tạo Bài viết Mới"
      emptyLabel="Không có bài viết nào."
    />
  );
}
