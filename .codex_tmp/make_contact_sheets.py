import math
import sys
from pathlib import Path
sys.path.insert(0, r'D:\Do_aN\.codex_tmp\pydeps')
import fitz

src = fitz.open(r'D:\Do_aN\.codex_tmp\Report3_Compact_QA.pdf')
out = Path(r'D:\Do_aN\.codex_tmp\qa_compact_sheets')
out.mkdir(parents=True, exist_ok=True)
hits = {marker: [] for marker in ('4.2 Mobile Screen', '5. Non-Functional Requirements')}
for i, page in enumerate(src):
    text = page.get_text('text')
    for marker in hits:
        if marker in text:
            hits[marker].append(i)
start = hits['4.2 Mobile Screen'][-1]
end = hits['5. Non-Functional Requirements'][-1]
per_sheet = 9
cols, rows = 3, 3
cell_w, cell_h = 650, 920

for sheet_no, base in enumerate(range(start, end + 1, per_sheet), 1):
    sheet = fitz.open()
    page = sheet.new_page(width=cols * cell_w, height=rows * cell_h)
    for offset, page_no in enumerate(range(base, min(base + per_sheet, end + 1))):
        row, col = divmod(offset, cols)
        rect = fitz.Rect(col * cell_w + 8, row * cell_h + 28, (col + 1) * cell_w - 8, (row + 1) * cell_h - 8)
        page.show_pdf_page(rect, src, page_no, keep_proportion=True)
        page.insert_text((col * cell_w + 12, row * cell_h + 20), f'PDF page {page_no + 1}', fontsize=12)
    pix = page.get_pixmap(matrix=fitz.Matrix(1.25, 1.25), alpha=False)
    pix.save(out / f'sheet-{sheet_no:02d}.png')
    sheet.close()
print('sheets', sheet_no)
