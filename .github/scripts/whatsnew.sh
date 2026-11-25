#!/usr/bin/env bash
# 배포 단위에 닿은 사용자용 변경만 추려 "바뀐 점" 을 만든다.
#
# 어느 변경이 어느 배포 단위에 닿는지는 Gradle 의존 그래프에서 유도한다 — 경로 목록을 두지 않아야
# 모듈 구조를 바꿀 때 이 스크립트를 따라 고칠 일이 없다.
#
# 사용법: whatsnew.sh <mobile|desktop> <git revision range>
set -euo pipefail

unit=${1:?배포 단위가 필요하다}
range=${2:?범위가 필요하다}

# 모듈 -> 그 모듈이 의존하는 모듈. 모듈마다 build.gradle.kts 가 하나씩 있다.
declare -A deps
while IFS= read -r file; do
  module=":$(dirname "$file" | tr '/' ':')"
  # 의존이 없는 모듈에서 grep 이 1 을 내므로 삼킨다.
  deps[$module]=$({ grep -oE 'project\(":[^"]+"\)' "$file" || true; } | sed -E 's@project\("(.*)"\)@\1@' | tr '\n' ' ')
done < <(ls ./*/build.gradle.kts ./*/*/build.gradle.kts 2>/dev/null | sed 's|^\./||')

case $unit in
  # ios/app 은 Xcode 프로젝트라 Gradle 모듈이 아니다. 그래서 그래프 밖에서 더한다.
  mobile) roots=":android:app :ios:shared"; extra="ios/app" ;;
  desktop) roots=":desktop"; extra="" ;;
  *) echo "::error::알 수 없는 배포 단위: $unit"; exit 1 ;;
esac

# 전이 폐쇄. 루트가 의존하는 모듈의 변경은 그 배포 단위의 변경이다.
seen=""
queue=$roots
while [[ -n ${queue// /} ]]; do
  next=""
  for module in $queue; do
    [[ " $seen " == *" $module "* ]] && continue
    seen="$seen $module"
    next="$next ${deps[$module]:-}"
  done
  queue=$next
done

paths=$extra
for module in $seen; do
  paths="$paths ${module#:}"
done
# shellcheck disable=SC2086  # 경로 목록을 git 에 각각의 인자로 넘긴다.
paths=$(echo $paths | tr ':' '/')

# feat·fix·hotfix 만 사용자 몫이다. 나머지 접두어는 사용자가 읽을 문장이 아니다.
# hotfix 는 "hotfix/<버전> <설명>" 이라 버전까지 뗀다. 끝의 PR 번호도 사용자에게는 뜻이 없다.
# shellcheck disable=SC2086
lines=$(git log --no-merges --format='%s' "$range" -- $paths |
  grep -E '^(feat|fix|hotfix)/' |
  sed -E 's@^(feat|fix)/@@; s@^hotfix/[0-9][0-9.]* *@@; s@ *-? *\(?#[0-9]+\)?$@@' |
  sed 's/^/- /' || true)

# 내부 정리만 들어간 릴리스가 있다. 그때도 "바뀐 점" 은 비어 있지 않아야 한다.
if [[ -z $lines ]]; then
  lines="- 내부 정리와 안정성 개선"
fi

printf '%s\n' "$lines"
