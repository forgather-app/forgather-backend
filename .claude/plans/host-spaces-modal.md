# 호스트 스페이스 목록 모달 구현 계획

## 개요
호스트 관리 페이지에서 각 호스트의 "스페이스 개수"를 클릭하면 모달이 열리고, 해당 호스트의 전체 스페이스 목록을 스크롤하면서 확인할 수 있는 기능 구현

## 사용 API
- **엔드포인트**: `GET /admin/hosts/{hostId}/spaces`
- **컨트롤러**: `AdminHostController.getHostSpaces()`
- **응답 DTO**: `HostSpacesResponse`
  ```json
  {
    "hostId": 1,
    "hostName": "홍길동",
    "spaces": [
      {
        "id": 1,
        "code": "e3f6b97f19",
        "name": "졸업 전시",
        "isPublic": true,
        "createdAt": "2025-11-14T10:30:00",
        "updatedAt": "2025-11-15T12:00:00"
      }
    ]
  }
  ```

## 현재 상태 분석
- `hosts/list.html`: 테이블에 ID, 호스트명, 스페이스 개수, 생성일 컬럼 존재
- `hosts.js`: 스페이스 개수는 `spaceIds.length`로 단순 숫자만 표시 중 (클릭 불가)
- `api.js`: `getHostSpaces()` 메서드 없음
- 모달 패턴은 `spaces/list.html` + `spaces.js`에 이미 구현되어 있음 → 동일 패턴 활용

## 구현 단계

### Step 1: api.js에 getHostSpaces() 메서드 추가
**파일**: `src/main/resources/static/js/admin/api.js`

기존 `getHosts()` 메서드 아래에 추가:
```javascript
async getHostSpaces(hostId) {
    return this.get(`/hosts/${hostId}/spaces`, {});
}
```

### Step 2: hosts/list.html에 모달 HTML 추가
**파일**: `src/main/resources/templates/admin/hosts/list.html`

`<div id="toast">` 바로 위에 모달 HTML 삽입. spaces 페이지 모달 패턴을 그대로 따르되, 내용만 스페이스 목록용으로 변경:

- **모달 오버레이**: `id="hostSpacesModal"`, `display: none`, blur 배경
- **모달 컨테이너**: 최대 너비 600px, 최대 높이 85vh
- **모달 헤더**: "호스트명의 스페이스" 제목 + X 닫기 버튼
- **모달 바디**: 로딩/에러/콘텐츠 3가지 상태
  - 로딩: 스피너
  - 에러: 에러 메시지 + 재시도 버튼
  - 콘텐츠: 스페이스 목록 (스크롤 가능한 리스트)
- **모달 푸터**: 닫기 버튼
- **`<style>` 블록**: 모달 애니메이션 CSS (opacity + translateY 트랜지션)

스페이스 목록 아이템 구조 (각 스페이스):
- 스페이스명 + 공개/비공개 뱃지
- 스페이스 코드 (서브텍스트)
- 생성일

### Step 3: hosts.js에 모달 로직 추가
**파일**: `src/main/resources/static/js/admin/hosts.js`

#### 3-1. 테이블 렌더링 변경
스페이스 개수 셀을 클릭 가능한 링크 스타일로 변경:
```html
<td class="... cursor-pointer text-primary hover:underline"
    data-host-id="${host.id}"
    data-host-name="${escapeHtml(host.name)}"
    onclick="openHostSpacesModal(this)">
    ${spaceCount}
</td>
```
- 숫자 0이면 클릭 비활성화 (회색, 커서 기본)
- 숫자 1 이상이면 클릭 가능 (primary 색상, pointer 커서, 호버 시 밑줄)

#### 3-2. 모달 제어 함수
DOMContentLoaded 안에 추가:

1. **DOM 요소 캐싱**: modal, modalLoading, modalError, modalContent 등
2. **openHostSpacesModal(element)**:
   - `data-host-id`, `data-host-name` 추출
   - 모달 제목에 호스트명 설정
   - `display: flex` → `requestAnimationFrame` → `.show` 클래스
   - `body.modal-open` 추가
   - `loadHostSpaces(hostId)` 호출
3. **closeHostSpacesModal()**:
   - `.show` 제거 → `setTimeout(300ms)` → `display: none`
   - `body.modal-open` 제거
   - 모달 콘텐츠 초기화
4. **loadHostSpaces(hostId)**:
   - 로딩 상태 표시
   - `API.getHostSpaces(hostId)` 호출
   - 성공: `renderHostSpaces(spaces)` 호출
   - 실패: 에러 메시지 표시
5. **renderHostSpaces(spaces)**:
   - 빈 목록: "스페이스가 없습니다" 메시지
   - 있으면: 스페이스 카드 리스트 렌더링
     - 각 아이템: 이름, 공개/비공개 뱃지, 코드, 생성일

#### 3-3. 모달 닫기 이벤트
- X 버튼 클릭
- 오버레이(배경) 클릭
- ESC 키
- 푸터 닫기 버튼

### Step 4: hosts.css에 모달 관련 동적 클래스 추가 (필요시)
**파일**: `src/main/resources/static/css/admin/hosts.css`

현재 비어있음. 모달 애니메이션 CSS는 `list.html`의 `<style>` 블록에 인라인으로 작성 (spaces 페이지와 동일 패턴)

뱃지 스타일이 필요하면 CSS에 추가하거나 Tailwind 인라인 클래스로 처리.

## 디자인 상세

### 스페이스 개수 셀 (테이블)
| 상태 | 스타일 |
|------|--------|
| 0개 | `text-text-muted`, 기본 커서, 클릭 불가 |
| 1개 이상 | `text-primary font-semibold cursor-pointer hover:underline`, 클릭 시 모달 열림 |

### 모달 스페이스 리스트 아이템
```
┌─────────────────────────────────────────────┐
│  [스페이스명]                    [공개 뱃지] │
│  코드: e3f6b97f19                           │
│  생성: 2025.11.14 10:30                     │
├─────────────────────────────────────────────┤
│  [다른 스페이스명]             [비공개 뱃지] │
│  코드: a1b2c3d4e5                           │
│  생성: 2025.10.20 15:00                     │
└─────────────────────────────────────────────┘
```

- 뱃지: 공개(`bg-success/10 text-success`) / 비공개(`bg-danger/10 text-danger`)
- 리스트: `max-h-[60vh] overflow-y-auto` 스크롤
- 각 아이템 사이 구분선: `divide-y divide-border-color`

### 모달 애니메이션
- 열기: opacity 0→1, translateY(20px)→0, scale(0.95)→1 (0.3s ease)
- 닫기: 역방향 (0.3s ease) → 300ms 후 display:none
- 배경: `bg-black/60` + `backdrop-filter: blur(4px)`

## 수정 파일 목록
| 파일 | 변경 내용 |
|------|-----------|
| `static/js/admin/api.js` | `getHostSpaces(hostId)` 메서드 추가 |
| `templates/admin/hosts/list.html` | 모달 HTML + 모달 CSS 애니메이션 추가 |
| `static/js/admin/hosts.js` | 테이블 셀 클릭 + 모달 열기/닫기/API호출/렌더링 |

## 구현 시 주의사항
- XSS 방지: `escapeHtml()` 필수 적용 (호스트명, 스페이스명, 코드)
- 기존 spaces 모달 패턴과 동일한 UX 유지 (애니메이션, 닫기 트리거)
- 모달 열린 상태에서 body 스크롤 방지 (`body.modal-open`)
- 접근성: `role="dialog"`, `aria-modal="true"`, `aria-labelledby`
- ui-ux-pro-max 스킬 사용하여 기존 디자인 시스템 일관성 유지
