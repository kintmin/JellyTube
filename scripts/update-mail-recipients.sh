#!/usr/bin/env bash
#
# 로컬의 mail-recipients.txt 를 검사해 JELLY_TUBE_MAIL_RECIPIENTS 시크릿에 올린다.
# 시크릿은 다시 읽을 수 없으므로 받는 사람 목록의 원본은 이 파일이다(.gitignore 대상).
#
#   ./scripts/update-mail-recipients.sh
#
# 파일 형식 — 한 줄에 한 사람, # 뒤는 주석:
#   me@gmail.com=*              # 모든 알림
#   friend@gmail.com=ios,release
# 알림 종류는 start·android·ios·desktop·release, * 는 전부다.
# 시크릿에는 한 줄("a=*; b=ios")로 합쳐 올린다. 여러 줄 시크릿은 로그에서 줄마다 가려진다는 보장이 없다.

set -euo pipefail

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly FILE="$REPO_ROOT/mail-recipients.txt"
readonly EVENTS="start android ios desktop release *"

[[ -f "$FILE" ]] || { echo "$FILE 이 없다. 위 형식으로 만든 뒤 다시 실행한다." >&2; exit 1; }

entries=()
while IFS= read -r line || [[ -n "$line" ]]; do
  line=${line%%#*}
  line=${line//[[:space:]]/}
  [[ -z "$line" ]] && continue
  [[ "$line" == *@*=* ]] || { echo "형식이 틀렸다: $line" >&2; exit 1; }
  IFS=',' read -ra events <<< "${line#*=}"
  for event in "${events[@]}"; do
    [[ " $EVENTS " == *" $event "* ]] || { echo "모르는 알림 종류 '$event': $line" >&2; exit 1; }
  done
  entries+=("$line")
done < "$FILE"

(( ${#entries[@]} > 0 )) || { echo "받는 사람이 없다." >&2; exit 1; }

value=$(IFS=';'; echo "${entries[*]}")
value=${value//;/; }
printf '%s' "$value" | gh secret set JELLY_TUBE_MAIL_RECIPIENTS -R kintmin/JellyTube
echo "받는 사람 ${#entries[@]}명을 올렸다: $value"
