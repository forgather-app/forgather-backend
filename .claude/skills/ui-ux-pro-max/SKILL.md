---
name: ui-ux-pro-max
description: "UI/UX design intelligence with 67 styles, 96 palettes, 57 font pairings, 25 charts, 13 stacks. Use when user says 'design a page', 'UI 만들어줘', 'landing page', 'color palette', 'font pairing', or asks to build, create, design, review, fix, improve UI/UX code for website, dashboard, admin panel, e-commerce, SaaS, portfolio, mobile app (.html, .tsx, .vue, .svelte). Covers glassmorphism, minimalism, brutalism, neumorphism, dark mode, responsive, accessibility, animation, typography, gradient."
metadata:
  author: ui-ux-pro-max
  version: 1.0.0
---

# UI/UX Pro Max - Design Intelligence

Comprehensive design guide for web and mobile applications. Contains 67 styles, 96 color palettes, 57 font pairings, 99 UX guidelines, and 25 chart types across 13 technology stacks.

## When to Apply

Reference these guidelines when:
- Designing new UI components or pages
- Choosing color palettes and typography
- Reviewing code for UX issues
- Building landing pages or dashboards
- Implementing accessibility requirements

## Prerequisites

```bash
python3 --version || python --version
```

If not installed: `brew install python3` (macOS) | `sudo apt install python3` (Ubuntu) | `winget install Python.Python.3.12` (Windows)

## How to Use This Skill

When user requests UI/UX work (design, build, create, implement, review, fix, improve), follow this workflow:

### Step 1: Analyze User Requirements

Extract key information from user request:
- **Product type**: SaaS, e-commerce, portfolio, dashboard, landing page, etc.
- **Style keywords**: minimal, playful, professional, elegant, dark mode, etc.
- **Industry**: healthcare, fintech, gaming, education, etc.
- **Stack**: React, Vue, Next.js, or default to `html-tailwind`

### Step 2: Generate Design System (REQUIRED)

**Always start with `--design-system`** to get comprehensive recommendations with reasoning:

```bash
python3 skills/ui-ux-pro-max/scripts/search.py "<product_type> <industry> <keywords>" --design-system [-p "Project Name"]
```

This command:
1. Searches 5 domains in parallel (product, style, color, landing, typography)
2. Applies reasoning rules from `ui-reasoning.csv` to select best matches
3. Returns complete design system: pattern, style, colors, typography, effects
4. Includes anti-patterns to avoid

**With persistence (Master + Overrides pattern):**
```bash
python3 skills/ui-ux-pro-max/scripts/search.py "<query>" --design-system --persist -p "Project Name"
python3 skills/ui-ux-pro-max/scripts/search.py "<query>" --design-system --persist -p "Project Name" --page "dashboard"
```

### Step 3: Supplement with Detailed Searches (as needed)

```bash
python3 skills/ui-ux-pro-max/scripts/search.py "<keyword>" --domain <domain> [-n <max_results>]
```

| Need | Domain | Example |
|------|--------|---------|
| More style options | `style` | `--domain style "glassmorphism dark"` |
| Chart recommendations | `chart` | `--domain chart "real-time dashboard"` |
| UX best practices | `ux` | `--domain ux "animation accessibility"` |
| Alternative fonts | `typography` | `--domain typography "elegant luxury"` |
| Landing structure | `landing` | `--domain landing "hero social-proof"` |

### Step 4: Stack Guidelines (Default: html-tailwind)

```bash
python3 skills/ui-ux-pro-max/scripts/search.py "<keyword>" --stack html-tailwind
```

Available stacks: `html-tailwind`, `react`, `nextjs`, `vue`, `svelte`, `swiftui`, `react-native`, `flutter`, `shadcn`, `jetpack-compose`

## Output Formats

```bash
# ASCII box (default) - best for terminal display
python3 skills/ui-ux-pro-max/scripts/search.py "fintech crypto" --design-system

# Markdown - best for documentation
python3 skills/ui-ux-pro-max/scripts/search.py "fintech crypto" --design-system -f markdown
```

## Example Workflow

**User request:** "Build a landing page for a beauty spa service"

1. **Analyze**: Product=Beauty/Spa, Style=elegant/soft, Industry=Wellness, Stack=html-tailwind
2. **Design System**: `python3 skills/ui-ux-pro-max/scripts/search.py "beauty spa wellness service elegant" --design-system -p "Serenity Spa"`
3. **Supplement**: `python3 skills/ui-ux-pro-max/scripts/search.py "animation accessibility" --domain ux`
4. **Stack**: `python3 skills/ui-ux-pro-max/scripts/search.py "layout responsive form" --stack html-tailwind`
5. **Implement**: Synthesize design system + detailed searches and build the design

## Tips for Better Results

1. **Be specific with keywords** - "healthcare SaaS dashboard" > "app"
2. **Search multiple times** - Different keywords reveal different insights
3. **Combine domains** - Style + Typography + Color = Complete design system
4. **Always check UX** - Search "animation", "z-index", "accessibility" for common issues
5. **Use stack flag** - Get implementation-specific best practices
6. **Iterate** - If first search doesn't match, try different keywords

## References

- `references/quick-reference.md` - 우선순위별 UX 규칙 빠른 참조
- `references/search-domains.md` - 검색 도메인 및 스택 상세
- `references/common-rules.md` - 프로 UI 공통 규칙 및 Do/Don't
- `references/checklist.md` - 납품 전 최종 체크리스트

## Troubleshooting

### Python 미설치
**Cause:** python3 명령어 없음
**Solution:** `brew install python3` (macOS) | `sudo apt install python3` (Ubuntu)

### 검색 결과 없음
**Cause:** 키워드가 너무 일반적
**Solution:** 구체적 키워드 조합 사용 (예: "healthcare SaaS dashboard")

### CSV 파일 로드 실패
**Cause:** data 경로 변경 후 스크립트 미업데이트
**Solution:** `scripts/core.py`의 `DATA_DIR` 경로가 `assets/data`를 가리키는지 확인
