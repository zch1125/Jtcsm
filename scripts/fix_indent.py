with open("D:\\Project\\Jtcsm\\scripts\\crawl_enhanced.py", "r", encoding="utf-8") as f:
    lines = f.readlines()

# Fix lines 287-288 (0-indexed: 286-287)
for i in [286, 287]:
    if lines[i].startswith("    ") and not lines[i].startswith("        "):
        lines[i] = "        " + lines[i].strip() + "\n"

with open("D:\\Project\\Jtcsm\\scripts\\crawl_enhanced.py", "w", encoding="utf-8") as f:
    f.writelines(lines)
print("Fixed")
