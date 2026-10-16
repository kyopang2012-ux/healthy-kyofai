"""Turn www/index.html into the Claude artifact version.

The artifact host wraps the page in its own <html>/<head>/<body> and pads the
top safe area itself, so this strips those parts and loads ExcelJS from cdnjs.
Usage: python3 tools/make_artifact.py OUTPUT.html
"""
import pathlib
import sys

src = pathlib.Path(__file__).resolve().parent.parent / "www" / "index.html"
s = src.read_text(encoding="utf-8")

pairs = [
    ('<!doctype html>\n<html lang="zh-HK">\n<head>\n<meta charset="utf-8">\n'
     '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n', ""),
    ("<title>Healthy KyoFai</title>\n",
     '<title>Healthy KyoFai</title>\n<meta name="claude:viewport-fit-sides" content="cover">\n'),
    ('<script defer src="vendor/exceljs.min.js" onerror="var s=document.createElement(\'script\');'
     's.src=\'https://cdnjs.cloudflare.com/ajax/libs/exceljs/4.4.0/exceljs.min.js\';document.head.appendChild(s);"></script>',
     '<script defer src="https://cdnjs.cloudflare.com/ajax/libs/exceljs/4.4.0/exceljs.min.js"></script>'),
    ("html{padding-top:env(safe-area-inset-top, 0px)}\n", ""),
    ("</style>\n</head>\n<body>\n", "</style>\n"),
    ("</script>\n</body>\n</html>", "</script>"),
]
for a, b in pairs:
    if a not in s:
        raise SystemExit(f"pattern not found: {a[:70]!r}")
    s = s.replace(a, b)

pathlib.Path(sys.argv[1]).write_text(s, encoding="utf-8")
print("wrote", sys.argv[1])
