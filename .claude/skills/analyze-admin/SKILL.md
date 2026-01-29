# 어드민 UI 패턴 분석

프로젝트의 기존 어드민 UI 패턴을 분석합니다.

## 분석 대상

- `templates/admin/` - Thymeleaf 템플릿
- `static/css/admin/` - CSS (특히 common.css의 변수)
- `static/js/admin/` - JS (API, Auth, PaginationUtil 객체)

## 분석 항목

1. CSS 변수 목록 및 사용 패턴
2. 전역 JS 객체 메서드
3. Thymeleaf fragment 구조
4. 파일 네이밍 규칙

## 출력 형식

Markdown 테이블로 정리하여 출력:

```markdown
## CSS Variables
| Variable | Value | Usage |
|----------|-------|-------|

## Global JS Objects
| Object | Methods | Description |
|--------|---------|-------------|

## Thymeleaf Fragments
| Fragment | Location | Usage |
|----------|----------|-------|
```

## 활용

- 새 페이지 생성 전 기존 패턴 파악
- 코드 리뷰 시 컨벤션 준수 확인
- 팀 온보딩 자료로 활용
