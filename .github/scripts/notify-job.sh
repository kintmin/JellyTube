#!/usr/bin/env bash
# job 이 끝날 때 결과·걸린 시간·링크를 알린다. JOB_STARTED_AT 은 job 첫 단계가 남긴다.
# 쓰는 법: notify-job.sh <이름> <job.status> [성공했을 때 붙일 링크]
set -euo pipefail

name=$1
status=$2
success_link=${3:-}

minutes=$(( ($(date +%s) - ${JOB_STARTED_AT:-$(date +%s)}) / 60 ))
run_url="https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID"
case "$status" in
  success) line="✅ $name 완료 (${minutes}분)"; link=${success_link:-$run_url} ;;
  cancelled) line="⏹️ $name 취소 (${minutes}분)"; link=$run_url ;;
  *) line="❌ $name 실패 (${minutes}분)"; link=$run_url ;;
esac

"$(dirname "$0")/send-mail.sh" dev "[JellyTube] 릴리스 ${GITHUB_SHA::7}" "$line
$link"
