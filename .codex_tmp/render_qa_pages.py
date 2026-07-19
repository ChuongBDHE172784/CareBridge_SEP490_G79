import sys
from pathlib import Path
sys.path.insert(0, r'D:\Do_aN\.codex_tmp\pydeps')
import fitz

pdf_path = Path(r'D:\Do_aN\.codex_tmp\Report3_Compact_QA.pdf')
out = Path(r'D:\Do_aN\.codex_tmp\qa_compact_pages')
out.mkdir(parents=True, exist_ok=True)
doc = fitz.open(pdf_path)

hits = {marker: [] for marker in (
    '4.2 Mobile Screen',
    '4.3 Web Screen',
    '5. Non-Functional Requirements',
)}
for i, page in enumerate(doc):
    text = page.get_text('text')
    for marker in hits:
        if marker in text:
            hits[marker].append(i)

start = hits['4.2 Mobile Screen'][-1]
web = hits['4.3 Web Screen'][-1]
end = hits['5. Non-Functional Requirements'][-1]
for i in range(start, end + 1):
    page = doc[i]
    pix = page.get_pixmap(matrix=fitz.Matrix(1.5, 1.5), alpha=False)
    pix.save(out / f'page-{i+1:03d}.png')

print('pages', len(doc), 'hits', hits, 'selected', {'4.2': start, '4.3': web, '5': end}, 'rendered', end - start + 1)
