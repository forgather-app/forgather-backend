# Implementation Plan: Admin UI Monochrome (White & Black) Redesign

## Overview

현재 어드민 UI는 Cyan 계열(#0891B2, #22D3EE, #ECFEFF)의 밝은 색상 테마를 사용하고 있어 가시성이 떨어진다는 피드백이 있습니다. 이 계획서는 화이트와 블랙 조합의 모노크롬 테마로 색상 체계를 변경하는 마이그레이션 전략을 제시합니다.

## Requirements

- 현재 Cyan 기반 색상 체계를 White/Black 모노크롬 테마로 변경
- 텍스트와 배경의 대비를 높여 가시성 개선 (WCAG 4.5:1 이상)
- 기존 UI 구조와 레이아웃은 유지
- 버튼, 배지, 상태 표시 등의 의미적 색상(성공/위험/경고)은 유지
- design-system 문서와 실제 구현의 일관성 유지

---

## Current State Analysis

### 1. 현재 색상 팔레트

| Role | Current Hex | Current CSS Variable |
|------|-------------|---------------------|
| Primary | `#0891B2` (Cyan) | `--primary-color` |
| Primary Hover | `#0E7490` | `--primary-hover` |
| Secondary | `#22D3EE` (Light Cyan) | `--secondary-color` |
| Background | `#ECFEFF` (Very Light Cyan) | `--bg-color` |
| Surface | `#FFFFFF` | `--surface-color` |
| Text Primary | `#164E63` (Dark Cyan) | `--text-primary` |
| Text Secondary | `#475569` | `--text-secondary` |
| Text Muted | `#94A3B8` | `--text-muted` |
| Border | `#E2E8F0` | `--border-color` |

### 2. 색상이 정의된 파일 위치

#### Design System 문서
- `design-system/forgather-admin/MASTER.md` - 마스터 색상 팔레트
- `design-system/forgather-admin/pages/login.md` - 로그인 페이지 오버라이드
- `design-system/forgather-admin/pages/spaces.md` - 스페이스 페이지 오버라이드
- `design-system/forgather-admin/pages/hosts.md` - 호스트 페이지 오버라이드

#### CSS Variables (레거시 호환성)
- `src/main/resources/static/css/admin/common.css` - CSS 변수 정의

#### Tailwind Config (각 페이지에 인라인)
- `src/main/resources/templates/admin/layout/base.html` - 레이아웃 베이스
- `src/main/resources/templates/admin/login.html` - 로그인 페이지
- `src/main/resources/templates/admin/spaces/list.html` - 스페이스 목록
- `src/main/resources/templates/admin/hosts/list.html` - 호스트 목록

#### JS 파일 (동적 클래스 사용)
- `src/main/resources/static/js/admin/spaces.js` - Tailwind 클래스 동적 생성
- `src/main/resources/static/js/admin/hosts.js` - Tailwind 클래스 동적 생성

---

## Proposed Monochrome Color Palette

| Role | New Hex | Tailwind Class | Description |
|------|---------|----------------|-------------|
| Primary | `#171717` | neutral-900 | 주요 액센트 - 거의 블랙 |
| Primary Hover | `#262626` | neutral-800 | 호버 상태 |
| Primary Light | `#404040` | neutral-700 | 라이트 버전 |
| Secondary | `#525252` | neutral-600 | 보조 색상 |
| Background | `#FAFAFA` | neutral-50 | 페이지 배경 - 오프 화이트 |
| Surface | `#FFFFFF` | white | 카드/컴포넌트 배경 |
| Text Primary | `#171717` | neutral-900 | 주요 텍스트 |
| Text Secondary | `#525252` | neutral-600 | 보조 텍스트 |
| Text Muted | `#A3A3A3` | neutral-400 | 희미한 텍스트 |
| Border | `#E5E5E5` | neutral-200 | 경계선 |
| CTA/Accent | `#171717` | neutral-900 | CTA 버튼 (반전: 흰 텍스트) |

### Semantic Colors (유지)
| Role | Hex | Usage |
|------|-----|-------|
| Success | `#22C55E` | 성공 상태, Public 배지 |
| Danger | `#EF4444` | 에러, Private 배지, 삭제 |
| Warning | `#F59E0B` | 경고 |
| Info | `#3B82F6` | 정보 (Cyan에서 Blue로 변경) |

---

## Implementation Steps

### Phase 1: Design System 문서 업데이트

**Step 1. MASTER.md 색상 팔레트 업데이트**
- File: `design-system/forgather-admin/MASTER.md`
- Action: Color Palette 섹션의 모든 색상을 모노크롬 팔레트로 변경
- Priority: High

변경 전:
```markdown
| Primary | `#0891B2` | `--color-primary` |
| Secondary | `#22D3EE` | `--color-secondary` |
| Background | `#ECFEFF` | `--color-background` |
| Text | `#164E63` | `--color-text` |
```

변경 후:
```markdown
| Primary | `#171717` | `--color-primary` |
| Secondary | `#525252` | `--color-secondary` |
| Background | `#FAFAFA` | `--color-background` |
| Text | `#171717` | `--color-text` |
```

---

### Phase 2: CSS Variables 업데이트

**Step 2. common.css :root 변수 업데이트**
- File: `src/main/resources/static/css/admin/common.css`
- Action: `:root` 블록의 모든 색상 변수 업데이트

변경할 변수:
```css
:root {
    --primary-color: #171717;      /* was #0891B2 */
    --primary-hover: #262626;      /* was #0E7490 */
    --secondary-color: #525252;    /* was #22D3EE */
    --bg-color: #FAFAFA;           /* was #ECFEFF */
    --text-primary: #171717;       /* was #164E63 */
    --text-secondary: #525252;     /* was #475569 */
    --text-muted: #A3A3A3;         /* was #94A3B8 */
    --border-color: #E5E5E5;       /* was #E2E8F0 */
    --info-color: #3B82F6;         /* was #0891B2 (Blue로 변경) */
    /* success, danger, warning은 유지 */
}
```

---

### Phase 3: Tailwind Config 업데이트

**Step 3. base.html Tailwind config 업데이트**
- File: `src/main/resources/templates/admin/layout/base.html`
- Action: `tailwind.config` 객체의 colors 섹션 업데이트

```javascript
tailwind.config = {
    theme: {
        extend: {
            colors: {
                primary: {
                    DEFAULT: '#171717',
                    hover: '#262626',
                    light: '#404040',
                },
                secondary: {
                    DEFAULT: '#525252',
                    hover: '#404040',
                },
                'bg-color': '#FAFAFA',
                'text-primary': '#171717',
                'text-secondary': '#525252',
                'text-muted': '#A3A3A3',
                'border-color': '#E5E5E5',
            },
        },
    },
}
```

**Step 4. login.html Tailwind config 업데이트**
- File: `src/main/resources/templates/admin/login.html`
- Action: 동일한 Tailwind config 적용
- 추가: `bg-gradient-to-br from-primary to-cyan-600` → `bg-neutral-900` 또는 `bg-gradient-to-br from-neutral-900 to-neutral-700`

**Step 5. spaces/list.html Tailwind config 업데이트**
- File: `src/main/resources/templates/admin/spaces/list.html`
- Action: 동일한 Tailwind config 적용
- 추가: `bg-primary/10` → `bg-neutral-100`, `border-primary/30` → `border-neutral-300`

**Step 6. hosts/list.html Tailwind config 업데이트**
- File: `src/main/resources/templates/admin/hosts/list.html`
- Action: spaces/list.html과 동일한 변경

---

### Phase 4: HTML 템플릿 검토 (Review Only)

**Step 7. fragments.html 색상 클래스 검토**
- File: `src/main/resources/templates/admin/layout/fragments.html`
- Action: 새 색상과 조화되는지 확인
- Risk: Low

---

### Phase 5: JavaScript 동적 클래스 검토 (Review Only)

**Step 8. spaces.js, hosts.js 동적 클래스 검토**
- Files: `src/main/resources/static/js/admin/spaces.js`, `hosts.js`
- Action: Tailwind 클래스가 자동 적용되므로 별도 변경 불필요
- Risk: Low

---

## Testing Checklist

### 로그인 페이지
- [ ] 배경 그라데이션이 모노크롬으로 변경됨
- [ ] 버튼 hover 상태 확인
- [ ] 입력 필드 focus 상태 확인

### Spaces 목록 페이지
- [ ] 헤더 네비게이션 색상 확인
- [ ] 테이블 헤더 배경색 확인
- [ ] 테이블 행 hover 효과 확인
- [ ] 필터 버튼 색상 확인
- [ ] 페이지네이션 버튼 색상 확인
- [ ] Toast 알림 색상 확인
- [ ] 모달 스타일 확인

### Hosts 목록 페이지
- [ ] Spaces 페이지와 동일한 항목 테스트

### 접근성 테스트
- [ ] Chrome DevTools Lighthouse로 접근성 점수 확인
- [ ] 텍스트 대비 비율 4.5:1 이상 확인

---

## File Change Summary

| File | Change Type | Priority |
|------|-------------|----------|
| `design-system/forgather-admin/MASTER.md` | Color Palette Update | High |
| `src/main/resources/static/css/admin/common.css` | CSS Variables Update | High |
| `src/main/resources/templates/admin/layout/base.html` | Tailwind Config Update | High |
| `src/main/resources/templates/admin/login.html` | Tailwind Config + Gradient Class | High |
| `src/main/resources/templates/admin/spaces/list.html` | Tailwind Config + Header Classes | High |
| `src/main/resources/templates/admin/hosts/list.html` | Tailwind Config + Header Classes | High |
| `src/main/resources/templates/admin/layout/fragments.html` | Review Only | Low |
| `src/main/resources/static/js/admin/spaces.js` | Review Only | Low |
| `src/main/resources/static/js/admin/hosts.js` | Review Only | Low |
| `src/main/resources/static/css/admin/spaces.css` | No Change | None |

---

## Risks & Mitigations

### Risk 1: Tailwind Config 중복
- **Description**: 각 HTML 파일에 Tailwind config가 인라인으로 중복 정의되어 있음
- **Mitigation**: 모든 파일에 동일한 변경 적용

### Risk 2: CSS 변수와 Tailwind 색상 불일치
- **Description**: common.css의 CSS 변수와 Tailwind config 색상이 다를 수 있음
- **Mitigation**: 두 곳 모두 동일한 값으로 업데이트

### Risk 3: 동적 생성 요소의 색상 깨짐
- **Description**: JS에서 동적으로 생성하는 요소가 새 색상을 참조하지 못할 수 있음
- **Mitigation**: Tailwind 클래스는 자동으로 적용됨, CSS 변수 참조 부분만 common.css 업데이트로 해결

---

## Success Criteria

- [ ] 모든 페이지가 White/Black 모노크롬 테마로 변경됨
- [ ] 텍스트 가시성이 개선됨 (대비 비율 4.5:1 이상)
- [ ] 기존 기능이 모두 정상 동작함
- [ ] Design System 문서가 실제 구현과 일치함
- [ ] 모바일/데스크톱 반응형 레이아웃이 유지됨
