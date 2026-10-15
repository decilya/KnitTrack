#!/usr/bin/env python3
"""Автоматический валидатор KnitTrac Coding Standards.

Проверки FAIL (код выхода 1) — гигиена, нарушать нельзя:
  1. TODO без номера issue.
  2. FIXME / HACK / XXX.
  3. println в production-коде.
  4. Double-bang (!! ) в production-коде.
  5. Магические строки темы "SYSTEM"/"LIGHT"/"DARK" в main.
  6. Непарный KDoc (/** против */) в любом .kt.
  7. Markdown-разметка ** внутри .kt.
  8. Heredoc-маркеры (_EOF) внутри .kt.
  9. Дубликат delegate preferencesDataStore (должен быть ровно 1).
 10. catch (e: Exception) в data/repository без парного catch CancellationException.
 11. catch (e: Exception) в data/repository без Timber.e.

Проверки WARN (печатаются, код выхода не меняют) — технический долг из раздела 23:
  W1. Backtick-имена тестов (пробелы/кириллица особенно).
  W2. System.currentTimeMillis/SystemClock вне data и platform/timer/SystemTimeProvider.
  W3. Классы с суффиксами Manager/Helper/Utils/Handler.

Запуск: python3 scripts/check_standards.py из корня репозитория.
"""
import re
import sys
from pathlib import Path

ROOT = Path("app/src")
MAIN = ROOT / "main"
FAIL = []
WARN = []


def scan(pattern, where, flags=0):
    rx = re.compile(pattern, flags)
    hits = []
    for path in where.rglob("*.kt"):
        for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if rx.search(line):
                hits.append(f"{path}:{i}: {line.strip()[:80]}")
    return hits


# ---------- FAIL-проверки ----------
FAIL += [("TODO без issue", h) for h in scan(r"//\s*TODO(?!\(#\d+\))", ROOT)]
FAIL += [("FIXME/HACK/XXX", h) for h in scan(r"\b(FIXME|HACK|XXX)\b", ROOT)]
FAIL += [("println в main", h) for h in scan(r"\bprintln\(", MAIN)]
FAIL += [("double-bang в main", h) for h in scan(r"\w!!", MAIN)]
FAIL += [("магическая строка темы", h) for h in scan(r'"(SYSTEM|LIGHT|DARK)"', MAIN)]
FAIL += [("markdown ** в kt", h) for h in scan(r"\*\*", ROOT)]
FAIL += [("heredoc-маркер в kt", h) for h in scan(r"_EOF\b", ROOT)]

for path in ROOT.rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    if text.count("/**") != text.count("*/"):
        FAIL.append(("непарный KDoc", f"{path}: open={text.count('/**')} close={text.count('*/')}"))

ds = scan(r"by preferencesDataStore\(", MAIN)
if len(ds) != 1:
    FAIL.append(("DataStore delegate должен быть один", f"найдено {len(ds)}: {ds}"))

for path in (MAIN / "data" / "repository").rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    n_exc = text.count("catch (e: Exception)")
    n_cancel = text.count("catch (e: CancellationException)")
    n_timber = text.count("Timber.e(")
    if n_exc and n_cancel != n_exc:
        FAIL.append(("CancellationException не первым catch", f"{path}: exc={n_exc} cancel={n_cancel}"))
    if n_exc and n_timber < n_exc:
        FAIL.append(("catch без Timber.e", f"{path}: exc={n_exc} timber={n_timber}"))

# ---------- WARN-проверки (техдолг) ----------
WARN += [("backtick-имя теста", h) for h in scan(r"fun\s+`[^`]+`", ROOT)]
allowed_time = ("data/", "SystemTimeProvider.kt")
for path in MAIN.rglob("*.kt"):
    if any(a in str(path) for a in allowed_time):
        continue
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"System\.currentTimeMillis\(|SystemClock\.", line):
            WARN.append(("System-время вне data/SystemTimeProvider", f"{path}:{i}"))
WARN += [("суффикс Manager/Helper/Utils/Handler", h)
         for h in scan(r"class \w+(Manager|Helper|Utils|Handler)\b", MAIN)]

# ---------- Отчёт ----------
print(f"FAIL-нарушений: {len(FAIL)}")
for name, hit in FAIL:
    print(f"  [FAIL] {name}: {hit}")
print(f"WARN-техдолга: {len(WARN)}")
for name, hit in WARN[:40]:
    print(f"  [WARN] {name}: {hit}")
if len(WARN) > 40:
    print(f"  ... и ещё {len(WARN) - 40}")

if FAIL:
    print("ИТОГ: стандарты НАРУШЕНЫ (FAIL). Коммит запрещён.")
    sys.exit(1)
print("ИТОГ: FAIL-нарушений нет. WARN — см. раздел 23 стандартов.")
sys.exit(0)
