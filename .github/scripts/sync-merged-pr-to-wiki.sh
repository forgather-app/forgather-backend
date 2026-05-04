#!/usr/bin/env bash
set -euo pipefail

# GitHub Actions event payload 안의 문자열을 YAML frontmatter에 안전하게 넣기 위해
# JSON string 형태로 escape한다. YAML은 JSON 문자열 표기를 그대로 문자열로 해석할 수 있다.
json_string() {
  jq -Rnr --arg value "$1" '$value | tojson'
}

# UTC 기준 sync 시각.
now_iso() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

: "${GITHUB_EVENT_PATH:?GITHUB_EVENT_PATH is required}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${RAW_REL:?RAW_REL is required}"
: "${GIT_AUTHOR_NAME:?GIT_AUTHOR_NAME is required}"
: "${GIT_AUTHOR_EMAIL:?GIT_AUTHOR_EMAIL is required}"

if [ ! -d "wiki/.git" ]; then
  echo "ERROR: wiki repository checkout is missing at ./wiki" >&2
  exit 1
fi

cd wiki

# checkout 직후 최신 상태를 한 번 더 보장한다.
git pull --ff-only

source_repo="$GITHUB_REPOSITORY"
repo_slug="${source_repo//\//__}"

pr_number="$(jq -r '.pull_request.number' "$GITHUB_EVENT_PATH")"
title="$(jq -r '.pull_request.title // ""' "$GITHUB_EVENT_PATH")"
url="$(jq -r '.pull_request.html_url // ""' "$GITHUB_EVENT_PATH")"
author="$(jq -r '.pull_request.user.login // ""' "$GITHUB_EVENT_PATH")"
base_ref="$(jq -r '.pull_request.base.ref // ""' "$GITHUB_EVENT_PATH")"
merged_at="$(jq -r '.pull_request.merged_at // ""' "$GITHUB_EVENT_PATH")"
body="$(jq -r '.pull_request.body // ""' "$GITHUB_EVENT_PATH")"

# workflow의 job if가 이미 merge PR만 통과시키지만, 스크립트를 잘못 호출했을 때 대비 안전장치
if [ "$(jq -r '.pull_request.merged // false' "$GITHUB_EVENT_PATH")" != "true" ]; then
  echo "Pull request was closed without merge. Skipping."
  exit 0
fi

if [ -z "$(printf '%s' "$body" | tr -d '[:space:]')" ]; then
  body="내용 없음"
fi

raw_file="$RAW_REL/$repo_slug/pr-$pr_number.md"

mkdir -p "$(dirname "$raw_file")"

# 이벤트 재실행 등으로 이미 raw 파일이 있으면 다시 만들지 않는다.
if [ -f "$raw_file" ]; then
  echo "Raw file already exists: $raw_file"
  exit 0
fi

assignees_yaml="$(jq -r '[.pull_request.assignees[]?.login] | map(tojson) | join(", ")' "$GITHUB_EVENT_PATH")"
labels_yaml="$(jq -r '[.pull_request.labels[]?.name] | map(tojson) | join(", ")' "$GITHUB_EVENT_PATH")"

assignees_md="$(
  jq -r '
    [.pull_request.assignees[]?.login]
    | if length == 0 then "없음" else map("`" + . + "`") | join(", ") end
  ' "$GITHUB_EVENT_PATH"
)"

labels_md="$(
  jq -r '
    [.pull_request.labels[]?.name]
    | if length == 0 then "없음" else map("`" + . + "`") | join(", ") end
  ' "$GITHUB_EVENT_PATH"
)"

workers_md="$(
  jq -r '
    [.pull_request.assignees[]?.login] as $assignees
    | if ($assignees | length) > 0 then $assignees
      elif (.pull_request.user.login // "") != "" then [.pull_request.user.login]
      else ["확인 필요"]
      end
    | map("- `" + . + "`")
    | join("\n")
  ' "$GITHUB_EVENT_PATH"
)"

synced_at="$(now_iso)"
markdown_fence='````'

cat > "$raw_file" <<EOF
---
source: github-pr
repo: $(json_string "$source_repo")
pr_number: $pr_number
base_branch: $(json_string "$base_ref")
merged_at: $(json_string "$merged_at")
author: $(json_string "$author")
assignees: [$assignees_yaml]
labels: [$labels_yaml]
url: $(json_string "$url")
---

# $source_repo PR #$pr_number - $title

## 기본 정보

- repo: \`$source_repo\`
- PR number: \`$pr_number\`
- title: $title
- URL: $url
- author: \`${author:-확인 필요}\`
- assignees: $assignees_md
- merged_at: \`$merged_at\`
- base branch: \`$base_ref\`
- labels: $labels_md

## 작업자

$workers_md

## PR 제목

$title

## PR 본문

${markdown_fence}markdown
$body
${markdown_fence}

_last synced: ${synced_at}_
EOF

git config user.name "$GIT_AUTHOR_NAME"
git config user.email "$GIT_AUTHOR_EMAIL"

git add "$RAW_REL"

if git diff --cached --quiet; then
  echo "No changes to commit."
  exit 0
fi

git commit -m "docs: add PR raw for $source_repo#$pr_number"

if ! git push; then
  echo "Push failed. Trying pull --rebase then push once..."
  git pull --rebase
  git push
fi

echo "Synced merged PR to wiki raw: $source_repo#$pr_number"
