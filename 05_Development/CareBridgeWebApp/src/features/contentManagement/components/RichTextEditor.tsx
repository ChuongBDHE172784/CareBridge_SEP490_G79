import { useEffect, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { TextStyleKit } from '@tiptap/extension-text-style';
import TextAlign from '@tiptap/extension-text-align';
import { Table } from '@tiptap/extension-table';
import { TableRow } from '@tiptap/extension-table-row';
import { TableCell } from '@tiptap/extension-table-cell';
import { TableHeader } from '@tiptap/extension-table-header';
import { ImageWithLayout } from './imageWithLayout';
import '../richContentBody.css';
import './RichTextEditor.css';

const FONT_SIZES = ['12px', '14px', '16px', '18px', '24px', '32px'];
const FONT_FAMILIES = [
  { label: 'Mặc định', value: '' },
  { label: 'Serif', value: 'Georgia, serif' },
  { label: 'Sans-serif', value: 'Arial, sans-serif' },
  { label: 'Monospace', value: '"Courier New", monospace' },
];
const IMAGE_WIDTH_PRESETS = ['25', '50', '75', '100'];
const IMAGE_ALIGN_OPTIONS: { value: string; label: string }[] = [
  { value: 'left', label: 'Trái' },
  { value: 'center', label: 'Giữa' },
  { value: 'right', label: 'Phải' },
];
const TEXT_ALIGN_OPTIONS: { value: string; label: string; icon: string }[] = [
  { value: 'left', label: 'Căn trái', icon: 'format_align_left' },
  { value: 'center', label: 'Căn giữa', icon: 'format_align_center' },
  { value: 'right', label: 'Căn phải', icon: 'format_align_right' },
  { value: 'justify', label: 'Căn đều', icon: 'format_align_justify' },
];

interface RichTextEditorProps {
  value: string;
  onChange: (html: string) => void;
  onImageUpload: (file: File) => Promise<string>;
  placeholder?: string;
}

export default function RichTextEditor({ value, onChange, onImageUpload, placeholder }: RichTextEditorProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const editor = useEditor({
    extensions: [
      StarterKit,
      TextStyleKit.configure({ backgroundColor: false, lineHeight: false }),
      ImageWithLayout,
      TextAlign.configure({ types: ['paragraph', 'heading'] }),
      Table.configure({
        resizable: true,
      }),
      TableRow,
      TableHeader,
      TableCell,
    ],
    content: value,
    shouldRerenderOnTransaction: true,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
    editorProps: {
      attributes: {
        class: 'rich-text-editor-content rich-content-body',
      },
    },
  });

  useEffect(() => {
    if (!editor) return;
    if (value !== editor.getHTML()) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
  }, [value, editor]);

  if (!editor) return null;

  const handleImageButtonClick = () => fileInputRef.current?.click();

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const url = await onImageUpload(file);
    editor.chain().focus().setImage({ src: url }).run();
    let imagePos: number | null = null;
    editor.state.doc.descendants((node, pos) => {
      if (imagePos === null && node.type.name === 'image' && node.attrs.src === url) {
        imagePos = pos;
        return false;
      }
      return true;
    });
    if (imagePos !== null) {
      editor.commands.setNodeSelection(imagePos);
    }
  };

  const handleAddLink = () => {
    const previousUrl = editor.getAttributes('link').href;
    const url = window.prompt('Nhập URL liên kết (https://...):', previousUrl);
    if (url === null) return;
    if (url === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run();
      return;
    }
    editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
  };

  const handleInsertTable = () => {
    editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run();
  };

  return (
    <div className="rich-text-editor border border-outline-variant rounded-2xl overflow-hidden">
      <div className="rich-text-editor-toolbar flex flex-wrap items-center gap-1 p-2 border-b border-outline-variant bg-surface-container-low">
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBold().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('bold') ? 'is-active' : ''}`}
          aria-label="In đậm"
          title="In đậm"
        >
          <span className="material-symbols-outlined text-lg">format_bold</span>
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleItalic().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('italic') ? 'is-active' : ''}`}
          aria-label="In nghiêng"
          title="In nghiêng"
        >
          <span className="material-symbols-outlined text-lg">format_italic</span>
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleUnderline().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('underline') ? 'is-active' : ''}`}
          aria-label="Gạch chân"
          title="Gạch chân"
        >
          <span className="material-symbols-outlined text-lg">format_underlined</span>
        </button>

        <span className="rich-text-toolbar-divider" />

        <select
          aria-label="Kiểu đoạn văn"
          className="rich-text-toolbar-select"
          value={
            editor.isActive('heading', { level: 1 }) ? 'h1'
              : editor.isActive('heading', { level: 2 }) ? 'h2'
              : editor.isActive('heading', { level: 3 }) ? 'h3'
              : 'paragraph'
          }
          onChange={(e) => {
            const v = e.target.value;
            if (v === 'paragraph') editor.chain().focus().setParagraph().run();
            else editor.chain().focus().toggleHeading({ level: Number(v.replace('h', '')) as 1 | 2 | 3 }).run();
          }}
        >
          <option value="paragraph">Văn bản thường</option>
          <option value="h1">Tiêu đề 1</option>
          <option value="h2">Tiêu đề 2</option>
          <option value="h3">Tiêu đề 3</option>
        </select>

        <select
          aria-label="Phông chữ"
          className="rich-text-toolbar-select"
          value={editor.getAttributes('textStyle').fontFamily ?? ''}
          onChange={(e) => {
            const v = e.target.value;
            if (v) editor.chain().focus().setFontFamily(v).run();
            else editor.chain().focus().unsetFontFamily().run();
          }}
        >
          {FONT_FAMILIES.map((f) => (
            <option key={f.label} value={f.value}>{f.label}</option>
          ))}
        </select>

        <select
          aria-label="Cỡ chữ"
          className="rich-text-toolbar-select"
          value={editor.getAttributes('textStyle').fontSize ?? ''}
          onChange={(e) => {
            const v = e.target.value;
            if (v) editor.chain().focus().setFontSize(v).run();
            else editor.chain().focus().unsetFontSize().run();
          }}
        >
          <option value="">Chọn cỡ chữ</option>
          {FONT_SIZES.map((size) => (
            <option key={size} value={size}>{size}</option>
          ))}
        </select>

        <input
          aria-label="Màu chữ"
          title="Màu chữ"
          type="color"
          className="rich-text-toolbar-color"
          value={editor.getAttributes('textStyle').color ?? '#000000'}
          onChange={(e) => editor.chain().focus().setColor(e.target.value).run()}
        />

        <span className="rich-text-toolbar-divider" />

        {TEXT_ALIGN_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => editor.chain().focus().setTextAlign(opt.value).run()}
            className={`rich-text-toolbar-btn ${editor.isActive({ textAlign: opt.value }) ? 'is-active' : ''}`}
            aria-label={opt.label}
            title={opt.label}
          >
            <span className="material-symbols-outlined text-lg">{opt.icon}</span>
          </button>
        ))}

        <span className="rich-text-toolbar-divider" />

        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('bulletList') ? 'is-active' : ''}`}
          aria-label="Danh sách gạch đầu dòng"
          title="Danh sách gạch đầu dòng"
        >
          <span className="material-symbols-outlined text-lg">format_list_bulleted</span>
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('orderedList') ? 'is-active' : ''}`}
          aria-label="Danh sách đánh số"
          title="Danh sách đánh số"
        >
          <span className="material-symbols-outlined text-lg">format_list_numbered</span>
        </button>
        <button
          type="button"
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
          className={`rich-text-toolbar-btn ${editor.isActive('blockquote') ? 'is-active' : ''}`}
          aria-label="Trích dẫn"
          title="Trích dẫn"
        >
          <span className="material-symbols-outlined text-lg">format_quote</span>
        </button>

        <span className="rich-text-toolbar-divider" />

        <button
          type="button"
          onClick={handleAddLink}
          className={`rich-text-toolbar-btn ${editor.isActive('link') ? 'is-active' : ''}`}
          aria-label="Chèn liên kết"
          title="Chèn liên kết"
        >
          <span className="material-symbols-outlined text-lg">link</span>
        </button>

        <button
          type="button"
          onClick={handleInsertTable}
          className={`rich-text-toolbar-btn ${editor.isActive('table') ? 'is-active' : ''}`}
          aria-label="Chèn bảng"
          title="Chèn bảng"
        >
          <span className="material-symbols-outlined text-lg">table_chart</span>
        </button>

        <button
          type="button"
          onClick={handleImageButtonClick}
          className="rich-text-toolbar-btn"
          aria-label="Chèn ảnh"
          title="Chèn ảnh"
        >
          <span className="material-symbols-outlined text-lg">image</span>
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          className="hidden"
          onChange={handleFileSelected}
        />

        {editor.isActive('image') && (
          <>
            <span className="rich-text-toolbar-divider" />
            {IMAGE_WIDTH_PRESETS.map((pct) => (
              <button
                key={pct}
                type="button"
                onClick={() => editor.chain().focus().updateAttributes('image', { widthPct: pct }).run()}
                className={`rich-text-toolbar-btn ${editor.getAttributes('image').widthPct === pct ? 'is-active' : ''}`}
                aria-label={`Kích thước ảnh ${pct}%`}
                title={`${pct}%`}
              >
                {pct}%
              </button>
            ))}
            {IMAGE_ALIGN_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => editor.chain().focus().updateAttributes('image', { align: opt.value }).run()}
                className={`rich-text-toolbar-btn ${editor.getAttributes('image').align === opt.value ? 'is-active' : ''}`}
                aria-label={`Căn ảnh ${opt.label}`}
                title={`Căn ảnh ${opt.label}`}
              >
                {opt.label}
              </button>
            ))}
          </>
        )}
      </div>

      <EditorContent editor={editor} placeholder={placeholder} />
    </div>
  );
}
