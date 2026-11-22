#!/usr/bin/env bash
# 릴리스 메일을 MAIL_RECIPIENTS 에서 이 종류를 받기로 한 사람에게 한 명씩 보낸다.
# 쓰는 법: send-mail.sh <알림 종류> <제목> <본문>
# MAIL_RECIPIENTS 형식: "a@gmail.com=dev; b@gmail.com=mobile"
#   dev     전부 — 개발용 진행 알림(시작, 빌드마다의 결과, 릴리스 생성)과 아래 새 버전 소식
#   mobile  Android·iOS 새 버전 소식
#   desktop Windows·macOS 새 버전 소식
#   *       mobile 과 desktop
set -euo pipefail

readonly FROM="kintmin.ci@gmail.com"
event=$1
subject=$2
body=$3

[[ -n "${MAIL_PASSWORD:-}" && -n "${MAIL_RECIPIENTS:-}" ]] || {
  echo "::warning::JELLY_TUBE_MAIL_PASSWORD 나 JELLY_TUBE_MAIL_RECIPIENTS 시크릿이 없어 메일을 보내지 않는다"
  exit 0
}
# Google 은 앱 비밀번호를 4자리씩 띄워 보여 준다. 띄어쓰기째 등록해도 되게 지운다.
password=${MAIL_PASSWORD// /}

send() {
  local to=$1 mail
  mail=$(mktemp)
  {
    printf 'From: JellyTube CI <%s>\r\n' "$FROM"
    printf 'To: %s\r\n' "$to"
    printf 'Subject: =?UTF-8?B?%s?=\r\n' "$(printf '%s' "$subject" | base64 | tr -d '\n')"
    printf 'MIME-Version: 1.0\r\n'
    printf 'Content-Type: text/plain; charset=UTF-8\r\n'
    printf 'Content-Transfer-Encoding: base64\r\n'
    printf '\r\n'
    printf '%s' "$body" | base64
  } > "$mail"
  # 알림이 실패해도 릴리스는 막지 않는다. 주소는 공개 로그에 남기지 않는다.
  curl --silent --show-error --ssl-reqd --url "smtps://smtp.gmail.com:465" \
    --user "$FROM:$password" --mail-from "$FROM" --mail-rcpt "$to" --upload-file "$mail" \
    || echo "::warning::알림 메일 하나를 보내지 못했다"
  rm -f "$mail"
}

IFS=';' read -ra entries <<< "$MAIL_RECIPIENTS"
for entry in "${entries[@]}"; do
  entry=${entry//[[:space:]]/}
  [[ "$entry" == *=* ]] || continue
  to=${entry%%=*}
  events=",${entry#*=},"
  if [[ "$events" == *",dev,"* || "$events" == *",$event,"* || ( "$event" != dev && "$events" == *",*,"* ) ]]; then
    send "$to"
  fi
done
