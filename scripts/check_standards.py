#!/usr/bin/env python3
"""Автоматический валидатор KnitTrac Coding Standards (Умная версия).

Проверяет только реальный код, игнорируя комментарии (//, /*, *).
"""
import re
import sys
from pathlib import Path

ROOT = Path("app/src")
FAIL = []
WARN = []

def is_comment(line: str) -> bool:
    """Проверяет, является ли строка комментарием."""
    stripped = line.strip()
    return stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*")

def check_file(path: Path):
    lines = path.read_text(encoding="utf-8").split("\n")
    
    for i, line in enumerate(lines, 1):
        if is_comment(line):
            continue  # Пропускаем комментарии полностью
            
        # 1. println в коде (а не в комментарии)
        if re.search(r'\bprintln\s*\(', line):
            FAIL.append((f"println в коде", f"{path}:{i}: {line.strip()[:70]}"))
            
        # 2. Магические строки тем в коде (а не в строковых литералах комментариев)
        if re.search(r'=\s*["\'](?:SYSTEM|LIGHT|DARK)["\']', line):
            FAIL.append((f"магическая строка темы", f"{path}:{i}: {line.strip()[:70]}"))
            
        # 3. Markdown ** в коде (вне строк и комментариев)
        # Ищем **, но не внутри строковых литералов "..." или '...'
        # Упрощенно: если есть ** и строка не является строковым литералом целиком
        if "**" in line and not line.strip().startswith('"') and not line.strip().endswith('"'):
            # Дополнительная проверка: это не часть строкового литерала
            if not re.search(r'".*\*\*.*"', line):
                FAIL.append((f"markdown ** в коде", f"{path}:{i}: {line.strip()[:70]}"))

        # WARN: backtick-имена тестов
        if re.search(r'fun\s+`[^`]+`', line):
            WARN.append((f"backtick-имя теста", f"{path}:{i}: {line.strip()[:70]}"))

# Проходим по всем .kt файлам
for path in ROOT.rglob("*.kt"):
    check_file(path)

# Вывод результатов
print(f"FAIL-нарушений: {len(FAIL)}")
for name, hit in FAIL:
    print(f"  [FAIL] {name}: {hit}")
    
print(f"WARN-техдолга: {len(WARN)}")
for name, hit in WARN[:40]:
    print(f"  [WARN] {name}: {hit}")
if len(WARN) > 40:
    print(f"  ... и ещё {len(WARN) - 40}")

if FAIL:
    print("\nИТОГ: стандарты НАРУШЕНЫ (FAIL). Коммит запрещён.")
    sys.exit(1)
else:
    print("\nИТОГ: FAIL-нарушений нет. Код соответствует стандартам.")
    sys.exit(0)
