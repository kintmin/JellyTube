#!/usr/bin/env bash
#
# 연결된 기기에서 Baseline Profile 과 Startup Profile 을 뽑아 android/app/src/release/generated/ 에 둔다.
#
#   ./scripts/generate-baseline-profile.sh
#
# 생성기만 돌린다. 같이 도는 StartupBenchmark 는 생성 중에 skip 되는데 AGP 가 그것을 실패로 센다.
# 무선 adb(시리얼에 ':')에서는 다 통과해도 AGP 가 결과를 못 찾아 실패로 끝난다. 그때는 결과 XML 로
# 판정하고, 통과했으면 기기 실행을 건너뛰고 프로필을 옮기는 단계만 다시 돌린다.

set -euo pipefail

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly GENERATOR="com.kintmin.jellytube.benchmark.BaselineProfileGenerator"
readonly DEVICE_TASK=":android:benchmark:connectedNonMinifiedReleaseAndroidTest"
readonly RESULTS="$REPO_ROOT/android/benchmark/build/outputs/androidTest-results/connected/nonMinifiedRelease"

case "$(uname -s)" in
  MINGW* | MSYS* | CYGWIN*) gradle=("$REPO_ROOT/gradlew.bat") ;;
  *) gradle=("$REPO_ROOT/gradlew") ;;
esac

cd "$REPO_ROOT"
rm -rf "$RESULTS"

if "${gradle[@]}" :android:app:generateBaselineProfile \
  "-Pandroid.testInstrumentationRunnerArguments.class=$GENERATOR"; then
  exit 0
fi

suites=$(cat "$RESULTS"/TEST-*.xml 2>/dev/null | grep -oE '<testsuite [^>]*>' || true)
[[ -n "$suites" ]] || { echo "생성기가 돌지 않았다. 위 Gradle 로그를 본다." >&2; exit 1; }
if grep -qE 'failures="[1-9]|errors="[1-9]' <<< "$suites"; then
  echo "생성기가 실패했다: $RESULTS" >&2
  exit 1
fi

echo "생성기는 통과했다. 기기 실행을 건너뛰고 프로필을 옮긴다."
"${gradle[@]}" :android:app:generateBaselineProfile -x "$DEVICE_TASK"
