import { describe, it, expect, vi, beforeAll, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RichTextEditor, { isRichTextEmpty } from './RichTextEditor';

// Vitest doesn't auto-run Testing Library's cleanup between tests the way Jest's
// testEnvironment integration does — without this, each test's render() piles up in the
// same jsdom document, making later queries ambiguous (e.g. two "Cỡ chữ" selects at once).
afterEach(() => cleanup());

// jsdom doesn't implement the pixel-geometry APIs ProseMirror's view uses to translate mouse
// coordinates into document positions (only relevant for real browsers). Stubbing them is the
// standard workaround for testing Tiptap/ProseMirror editors under jsdom.
beforeAll(() => {
  document.elementFromPoint = () => null;
  Range.prototype.getBoundingClientRect = () => ({
    x: 0, y: 0, width: 0, height: 0, top: 0, left: 0, right: 0, bottom: 0, toJSON: () => {},
  });
  Range.prototype.getClientRects = () => ({
    length: 0,
    item: () => null,
    [Symbol.iterator]: function* () {},
  }) as unknown as DOMRectList;
});

// RTE-TC-011..013 — see ContentRichTextEditor_Test-Spec.md §4.
describe('RichTextEditor', () => {
  // RTE-TC-011: Bold sinh đúng thẻ <strong>
  it('toggling Bold then typing wraps subsequent text in <strong>', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <RichTextEditor value="" onChange={onChange} onImageUpload={vi.fn()} />,
    );

    await user.click(screen.getByRole('button', { name: 'In đậm' }));
    const editable = document.querySelector('[contenteditable="true"]') as HTMLElement;
    await user.type(editable, 'Hello');

    await waitFor(() => {
      const lastHtml = onChange.mock.calls.at(-1)?.[0] as string | undefined;
      expect(lastHtml).toBeDefined();
      expect(lastHtml).toContain('<strong>');
    });
  });

  // RTE-TC-012: chọn cỡ chữ sinh style inline đúng
  it('setting font size then typing produces a font-size inline style', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    render(
      <RichTextEditor value="" onChange={onChange} onImageUpload={vi.fn()} />,
    );

    await user.selectOptions(screen.getByLabelText('Cỡ chữ'), '18px');
    const editable = document.querySelector('[contenteditable="true"]') as HTMLElement;
    await user.type(editable, 'Big');

    await waitFor(() => {
      const lastHtml = onChange.mock.calls.at(-1)?.[0] as string | undefined;
      expect(lastHtml).toBeDefined();
      expect(lastHtml).toContain('font-size: 18px');
    });
  });

  // RTE-TC-013: chèn ảnh gọi onImageUpload, URL trả về được chèn vào editor
  it('picking an image file calls onImageUpload and inserts the returned URL as <img src>', async () => {
    const onChange = vi.fn();
    const onImageUpload = vi.fn().mockResolvedValue('https://res.cloudinary.com/demo/x.jpg');
    render(
      <RichTextEditor value="" onChange={onChange} onImageUpload={onImageUpload} />,
    );

    const file = new File(['fake-bytes'], 'photo.png', { type: 'image/png' });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    // The input is visually hidden (Tailwind `.hidden`, triggered via the toolbar button
    // instead) — userEvent.upload()'s pointer-interaction checks don't play well with jsdom's
    // missing layout geometry, so drive the DOM `files`/change event directly instead.
    Object.defineProperty(fileInput, 'files', { value: [file] });
    fireEvent.change(fileInput);

    await waitFor(() => expect(onImageUpload).toHaveBeenCalledWith(file));
    await waitFor(() => {
      const lastHtml = onChange.mock.calls.at(-1)?.[0] as string | undefined;
      expect(lastHtml).toBeDefined();
      expect(lastHtml).toContain('src="https://res.cloudinary.com/demo/x.jpg"');
    });
  });
});

describe('isRichTextEmpty', () => {
  it('treats an empty paragraph as empty', () => {
    expect(isRichTextEmpty('<p></p>')).toBe(true);
  });

  it('treats whitespace-only content as empty', () => {
    expect(isRichTextEmpty('<p>   </p>')).toBe(true);
  });

  it('treats real text as non-empty', () => {
    expect(isRichTextEmpty('<p>Xin chào</p>')).toBe(false);
  });

  it('treats an image-only body as non-empty', () => {
    expect(isRichTextEmpty('<p><img src="https://res.cloudinary.com/x.jpg"></p>')).toBe(false);
  });
});
