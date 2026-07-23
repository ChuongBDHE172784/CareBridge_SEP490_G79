import { useEffect, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { TextStyleKit } from '@tiptap/extension-text-style';
import TextAlign from '@tiptap/extension-text-align';
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
// ADR-RTE-008: preset sizes only (no freeform drag-resize) — smallest scoped change, no new
// npm dependency. Values match HtmlContentSanitizer.java WIDTH_PCT_ENUM exactly.
const IMAGE_WIDTH_PRESETS = ['25', '50', '75', '100'];
const IMAGE_ALIGN_OPTIONS: { value: string; label: string }[] = [
  { value: 'left', label: 'Trái' },
  { value: 'center', label: 'Giữa' },
  { value: 'right', label: 'Phải' },
];
// ADR-RTE-009
const TEXT_ALIGN_OPTIONS: { value: string; label: string; icon: string }[] = [
  { value: 'left', label: 'Căn trái', icon: 'format_align_left' },
  { value: 'center', label: 'Căn giữa', icon: 'format_align_center' },
  { value: 'right', label: 'Căn phải', icon: 'format_align_right' },
  { value: 'justify', label: 'Căn đều', icon: 'format_align_justify' },
];

/**
 * Rich text HTML always contains wrapper markup even when the user hasn't typed anything
 * (e.g. an empty editor is "<p></p>"), so a plain `.trim().length > 0` check on the raw HTML
 * (as the old plain-textarea `body` field used) would incorrectly treat empty content as valid.
 */
export function isRichTextEmpty(html: string): boolean {
  if (html.includes('<img')) return false;
  return html.replace(/<[^>]*>/g, '').trim().length === 0;
}

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
      StarterKit, // includes Bold/Italic/Underline/lists/blockquote/headings out of the box in v3
      // Bundles TextStyle + Color + FontFamily + FontSize (official Tiptap v3 extension —
      // see ContentRichTextEditor_TDS.md ADR-RTE-002 addendum: this replaced the custom
      // font-size mark extension originally planned, since Tiptap now ships one natively).
      // backgroundColor/lineHeight disabled: the backend sanitizer (ADR-RTE-005) only
      // allows color/font-size/font-family, so exposing more here would silently vanish
      // on save.
      TextStyleKit.configure({ backgroundColor: false, lineHeight: false }),
      ImageWithLayout,
      // ADR-RTE-009: style-based, same mechanism as color/font-size/font-family above.
      TextAlign.configure({ types: ['paragraph', 'heading'] }),
    ],
    content: value,
    // Tiptap v3 default: useEditor no longer re-renders the component on every transaction
    // (selection change, formatting toggle, etc.) — without this, toolbar active-state
    // highlighting (bold/italic/heading/font-size, and the image resize/align toolbar which
    // only appears once a selection is a NodeSelection on an image) would never update after
    // the initial render, since node-selection changes alone don't call onUpdate/onChange.
    shouldRerenderOnTransaction: true,
    onUpdate: ({ editor }) => onChange(editor.getHTML()),
    editorProps: {
      attributes: {
        class: 'rich-text-editor-content rich-content-body',
      },
    },
  });

  // Keep the editor in sync when `value` is replaced from outside (e.g. loading an
  // existing article into EditContentPage) without fighting the user's own typing.
  useEffect(() => {
    if (!editor) return;
    if (value !== editor.getHTML()) {
      editor.commands.setContent(value, { emitUpdate: false });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, editor]);

  if (!editor) return null;

  const handleImageButtonClick = () => fileInputRef.current?.click();

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const url = await onImageUpload(file);
    editor.chain().focus().setImage({ src: url }).run();
    // Select the newly-inserted image node immediately so the resize/align toolbar (ADR-RTE-008)
    // is usable right away, without requiring a separate click on the image first. Insertion can
    // shift/split surrounding nodes (e.g. the editor's initial empty paragraph), so locate the
    // node we just inserted by matching its src rather than assuming a pre-computed position.
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
