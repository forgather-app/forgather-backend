#!/bin/bash

# =============================================================================
# Admin UI Convention Check Script
# Forgather 프로젝트의 어드민 UI 컨벤션 검증
# =============================================================================

FILE_PATH="$1"
CONTENT="$2"

# 색상 정의
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# 경고 카운터
WARNINGS=0

# =============================================================================
# Helper Functions
# =============================================================================

warn() {
    echo -e "${YELLOW}[CONVENTION WARNING]${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

error() {
    echo -e "${RED}[CONVENTION ERROR]${NC} $1"
    exit 1
}

info() {
    echo -e "${GREEN}[CONVENTION OK]${NC} $1"
}

# =============================================================================
# Check if file is in admin directory
# =============================================================================

is_admin_file() {
    [[ "$FILE_PATH" == *"/admin/"* ]] || [[ "$FILE_PATH" == *"/admin."* ]]
}

# =============================================================================
# CSS Convention Checks
# =============================================================================

check_css_conventions() {
    local content="$1"

    # 1. 하드코딩된 색상 값 체크 (CSS 변수 대신 직접 사용)
    if echo "$content" | grep -qE '#[0-9a-fA-F]{3,6}[^a-zA-Z0-9]'; then
        if [[ "$FILE_PATH" != *"common.css"* ]]; then
            warn "하드코딩된 색상 값 발견. CSS 변수 사용을 권장합니다. (예: var(--primary-color))"
        fi
    fi

    # 2. rgb/rgba 하드코딩 체크
    if echo "$content" | grep -qE 'rgb\([0-9]+,[[:space:]]*[0-9]+,[[:space:]]*[0-9]+\)'; then
        if [[ "$FILE_PATH" != *"common.css"* ]]; then
            warn "하드코딩된 RGB 색상 발견. CSS 변수 사용을 권장합니다."
        fi
    fi

    # 3. 하드코딩된 픽셀 간격 체크
    if echo "$content" | grep -qE '(margin|padding):[[:space:]]*[0-9]+px'; then
        warn "하드코딩된 간격 값 발견. CSS 변수 사용을 권장합니다. (예: var(--spacing-md))"
    fi

    # 4. !important 사용 체크
    if echo "$content" | grep -q '!important'; then
        warn "!important 사용 발견. 가능하면 선택자 특이성으로 해결하세요."
    fi
}

# =============================================================================
# JavaScript Convention Checks
# =============================================================================

check_js_conventions() {
    local content="$1"

    # 1. fetch 직접 사용 체크 (API 객체 대신)
    if echo "$content" | grep -qE 'fetch\([^)]+\)'; then
        if [[ "$FILE_PATH" != *"api.js"* ]]; then
            warn "fetch() 직접 사용 발견. API 전역 객체 사용을 권장합니다. (예: API.get(), API.post())"
        fi
    fi

    # 2. localStorage/sessionStorage 직접 사용 체크
    if echo "$content" | grep -qE '(localStorage|sessionStorage)\.(get|set)Item'; then
        warn "localStorage/sessionStorage 사용 발견. 세션 기반 인증에서는 불필요합니다."
    fi

    # 3. innerHTML에 사용자 입력 직접 삽입 체크 (XSS 위험)
    if echo "$content" | grep -qE '\.innerHTML[[:space:]]*=[[:space:]]*[^`]'; then
        if ! echo "$content" | grep -q 'escapeHtml'; then
            warn "innerHTML 직접 할당 발견. XSS 방지를 위해 escapeHtml() 사용을 권장합니다."
        fi
    fi

    # 4. console.log 체크
    if echo "$content" | grep -qE 'console\.(log|debug)\('; then
        warn "console.log/debug 발견. 프로덕션 코드에서는 제거하거나 console.error/warn만 사용하세요."
    fi

    # 5. var 키워드 사용 체크
    if echo "$content" | grep -qE '\bvar\b'; then
        warn "var 키워드 발견. let 또는 const 사용을 권장합니다."
    fi
}

# =============================================================================
# HTML/Thymeleaf Convention Checks
# =============================================================================

check_html_conventions() {
    local content="$1"

    # 1. 필수 스크립트 포함 여부 체크
    if echo "$content" | grep -q '<script.*\.js'; then
        if ! echo "$content" | grep -q 'auth\.js'; then
            warn "auth.js 스크립트가 누락되었습니다. 인증 처리를 위해 필수입니다."
        fi

        if ! echo "$content" | grep -q 'api\.js'; then
            warn "api.js 스크립트가 누락되었습니다. API 호출을 위해 필수입니다."
        fi
    fi

    # 2. common.css 포함 여부 체크
    if echo "$content" | grep -q '<link.*\.css'; then
        if ! echo "$content" | grep -q 'common\.css'; then
            warn "common.css가 누락되었습니다. 공통 스타일을 위해 필수입니다."
        fi
    fi

    # 3. DOCTYPE 선언 체크
    if echo "$content" | grep -qE '<html'; then
        if ! echo "$content" | grep -qi '<!DOCTYPE html>'; then
            warn "DOCTYPE 선언이 누락되었습니다."
        fi
    fi

    # 4. lang 속성 체크
    if echo "$content" | grep -qE '<html[^>]*>'; then
        if ! echo "$content" | grep -qE '<html[^>]*lang='; then
            warn "html 태그에 lang 속성이 누락되었습니다. (예: lang=\"ko\")"
        fi
    fi

    # 5. viewport meta 태그 체크
    if echo "$content" | grep -q '<head>'; then
        if ! echo "$content" | grep -q 'viewport'; then
            warn "viewport meta 태그가 누락되었습니다. 반응형 디자인을 위해 필수입니다."
        fi
    fi
}

# =============================================================================
# Main Logic
# =============================================================================

# 어드민 파일인지 확인
if ! is_admin_file; then
    exit 0
fi

# 파일 확장자에 따라 검사 수행
case "$FILE_PATH" in
    *.css)
        check_css_conventions "$CONTENT"
        ;;
    *.js)
        check_js_conventions "$CONTENT"
        ;;
    *.html)
        check_html_conventions "$CONTENT"
        ;;
esac

# 결과 요약
if [ $WARNINGS -gt 0 ]; then
    echo ""
    echo -e "${YELLOW}총 ${WARNINGS}개의 컨벤션 경고가 있습니다.${NC}"
    echo "이 경고는 참고용이며, 파일 생성/수정을 차단하지 않습니다."
fi

exit 0
