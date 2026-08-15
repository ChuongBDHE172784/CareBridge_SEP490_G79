import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Stethoscope } from 'lucide-react';

/**
 * Khung chung cho các trang pháp lý (Điều khoản, Chính sách bảo mật).
 *
 * Hai trang này phải đọc được khi CHƯA đăng nhập — người dùng bấm vào từ ô
 * chấp thuận ở màn đăng ký, nên chúng nằm ngoài mọi guard và không gọi API.
 *
 * `sections` vừa dựng mục lục vừa cấp id cho từng phần, nhờ đó một điều khoản
 * cụ thể có thể được dẫn chiếu trực tiếp bằng liên kết neo — cần thiết khi cơ
 * quan quản lý hoặc người dùng yêu cầu chỉ ra đúng điều mục đang áp dụng.
 */

export type LegalSection = {
  id: string;
  title: string;
  body: React.ReactNode;
};

type Props = {
  documentTitle: string;
  subtitle: string;
  sections: LegalSection[];
};

export default function LegalPageLayout({ documentTitle, subtitle, sections }: Props) {
  useEffect(() => {
    document.title = `${documentTitle} — CareBridge`;
  }, [documentTitle]);

  return (
    <div className="min-h-screen bg-background font-sans text-on-surface">
      <header className="border-b border-outline-variant bg-surface">
        <div className="mx-auto flex max-w-4xl items-center gap-3 px-6 py-5">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-primary/15 text-primary">
            <Stethoscope aria-hidden="true" size={20} />
          </span>
          <span className="text-lg font-bold">CareBridge</span>
          <Link
            to="/expert/register"
            className="ml-auto inline-flex items-center gap-2 rounded-full border border-outline-variant px-4 py-2 text-sm font-semibold text-on-surface-variant transition-colors hover:bg-surface-variant"
          >
            <ArrowLeft aria-hidden="true" size={16} />
            Quay lại đăng ký
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        <h1 className="m-0 text-3xl font-bold leading-tight">{documentTitle}</h1>
        <p className="mt-3 text-sm leading-relaxed text-on-surface-variant">{subtitle}</p>

        <nav aria-label="Mục lục" className="mt-8 rounded-2xl border border-outline-variant p-5">
          <h2 className="m-0 text-sm font-bold uppercase tracking-wide text-on-surface-variant">
            Mục lục
          </h2>
          <ol className="mt-3 grid gap-2 sm:grid-cols-2">
            {sections.map((section, index) => (
              <li key={section.id} className="text-sm">
                <a
                  href={`#${section.id}`}
                  className="text-primary transition-colors hover:underline"
                >
                  {index + 1}. {section.title}
                </a>
              </li>
            ))}
          </ol>
        </nav>

        <div className="mt-10 grid gap-10">
          {sections.map((section, index) => (
            <section key={section.id} id={section.id} className="scroll-mt-24">
              <h2 className="m-0 text-xl font-bold">
                {index + 1}. {section.title}
              </h2>
              <div className="mt-3 grid gap-3 text-sm leading-relaxed text-on-surface-variant">
                {section.body}
              </div>
            </section>
          ))}
        </div>

        <footer className="mt-12 border-t border-outline-variant pt-6 text-xs leading-relaxed text-on-surface-variant">
          <p className="m-0">
            Tài liệu này được xây dựng theo pháp luật Việt Nam. Khi có thay đổi về quy định pháp
            luật, CareBridge sẽ cập nhật và thông báo tới người dùng theo cách thức nêu trong tài
            liệu.
          </p>
        </footer>
      </main>
    </div>
  );
}

/** Đoạn văn thường, dùng lại trong cả hai trang pháp lý. */
export function P({ children }: { children: React.ReactNode }) {
  return <p className="m-0">{children}</p>;
}

/** Danh sách gạch đầu dòng thống nhất giữa các mục. */
export function Bullets({ items }: { items: React.ReactNode[] }) {
  return (
    <ul className="m-0 grid list-disc gap-2 pl-5">
      {items.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  );
}

/**
 * Trích dẫn văn bản pháp luật. Tách riêng để mọi chỗ dẫn luật đều hiển thị
 * giống nhau và dễ rà soát khi luật thay đổi.
 */
export function LawRef({ children }: { children: React.ReactNode }) {
  return <span className="font-medium text-on-surface">{children}</span>;
}
