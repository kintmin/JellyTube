#!/usr/bin/env bash
#
# iOS 가 쓰는 Python 런타임과 패키지를 내려받아 `ios/app/Vendor/` 에 채운다.
#
# 이 디렉터리는 `.gitignore` 되어 있다(내려받는 산출물이라 저장소에 두지 않는다).
# **클론 직후 iOS 빌드를 하려면 이 스크립트를 먼저 돌려야 한다.**
#
#   ./scripts/setup-ios-vendor.sh
#
# 패키지 목록은 `android/app/build.gradle.kts` 의 chaquopy `pip { install(...) }` 와
# 짝이다. 한쪽에 패키지를 더하면 다른 쪽도 더한다 — 안 그러면 한 플랫폼에서만 동작한다.

set -euo pipefail

# Python-Apple-support 의 배포 태그. `ios/app/Vendor/Python/VERSIONS` 가 기록하는 값과 같아야 한다.
readonly PYTHON_MINOR="3.11"
readonly PYTHON_BUILD="b9"

# Android 의 chaquopy pip 목록과 같아야 한다.
readonly PACKAGES=(yt-dlp pykakasi)

readonly REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly VENDOR_DIR="$REPO_ROOT/ios/app/Vendor/Python"
readonly APP_PACKAGES_DIR="$VENDOR_DIR/app-packages"
readonly SUPPORT_URL="https://github.com/beeware/Python-Apple-support/releases/download/${PYTHON_MINOR}-${PYTHON_BUILD}/Python-${PYTHON_MINOR}-iOS-support.${PYTHON_BUILD}.tar.gz"

install_runtime() {
    if [ -d "$VENDOR_DIR/Python.xcframework" ]; then
        echo "Python.xcframework 가 이미 있다. 건너뛴다."
        return
    fi

    echo "Python ${PYTHON_MINOR} ${PYTHON_BUILD} iOS 지원 번들을 내려받는다..."
    mkdir -p "$VENDOR_DIR"
    local archive
    archive="$(mktemp -t python-ios-support).tar.gz"
    curl -fsSL --retry 5 --retry-delay 10 -o "$archive" "$SUPPORT_URL"
    tar -xzf "$archive" -C "$VENDOR_DIR"
    rm -f "$archive"
}

install_packages() {
    echo "app-packages 를 채운다: ${PACKAGES[*]}"
    mkdir -p "$APP_PACKAGES_DIR"
    python3 -m pip install \
        --target "$APP_PACKAGES_DIR" \
        --no-compile \
        --upgrade \
        "${PACKAGES[@]}"

    # pip 는 **호스트(macOS)** 용 휠을 고른다. 섞여 들어온 네이티브 확장은 iOS 에서 못 쓰므로
    # 지운다 — 여기 쓰는 패키지는 전부 순수 파이썬 폴백이 있다.
    # 네이티브 확장이 꼭 필요한 패키지가 생기면 iOS 휠을 따로 받아야 한다.
    find "$APP_PACKAGES_DIR" \( -name "*.so" -o -name "*.dylib" -o -name "*.pyd" \) -delete

    # 앱 번들에 들어갈 필요가 없는 것들.
    rm -rf "$APP_PACKAGES_DIR/bin" "$APP_PACKAGES_DIR/share" "$APP_PACKAGES_DIR"/*-stubs
}

install_runtime
install_packages

echo "완료. ios/app/Vendor 준비됨:"
du -sh "$VENDOR_DIR"
