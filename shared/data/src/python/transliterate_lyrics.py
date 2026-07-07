# -*- coding: utf-8 -*-
"""
일본어(한자/가나 혼용) 가사를 한국어 발음(한글 음차)으로 변환한다.

파이프라인:
  1) pykakasi 로 각 조각의 히라가나 읽기('hira')를 얻는다. (한자 읽기 = 사전 기반, 유지보수되는 부분)
  2) 히라가나 -> 한글 은 아래 자체 표(외부 라이브러리 의존 없음)로 변환한다.

표기 스타일: "소리 나는 대로(단순)".
  - 위치 무관하게 か행=카/키/쿠/케/코, 장음(う/お, ー)은 그대로 우/오 로 표기.
  - 촉음(っ) = ㅅ 받침, 발음(ん) = ㄴ 받침.
  예) とうきょう -> 토우쿄우, さっぽろ -> 삿포로, きみのなまえ -> 키미노 나마에
"""

import pykakasi

# pykakasi 인스턴스는 사전 로딩 비용이 크므로 재사용한다.
_kakasi = pykakasi.kakasi()

# 한글 조합용 자모 테이블
_CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
_JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
_JONG = [
    "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ",
    "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ",
    "ㅌ", "ㅍ", "ㅎ",
]

_HANGUL_BASE = 0xAC00


def _compose(cho, jung, jong=""):
    # 초성/중성/종성 자모로 완성형 한글 한 글자를 만든다.
    code = (_CHO.index(cho) * 21 + _JUNG.index(jung)) * 28 + _JONG.index(jong)
    return chr(_HANGUL_BASE + code)


def _is_hangul(ch):
    return "가" <= ch <= "힣"


def _add_final(syllable, jong):
    # 완성형 한글 음절에 종성(받침)을 붙인다. 이미 받침이 있으면 덮어쓴다.
    code = ord(syllable) - _HANGUL_BASE
    cho = code // 588
    jung = (code % 588) // 28
    return chr(_HANGUL_BASE + (cho * 588) + (jung * 28) + _JONG.index(jong))


def _vowel_of(syllable):
    # 완성형 한글 음절의 중성 자모를 돌려준다. (장음 처리에 사용)
    code = ord(syllable) - _HANGUL_BASE
    return _JUNG[(code % 588) // 28]


# 히라가나 모라 -> (초성, 중성). 'u' 열은 s/z/ts 계열만 ㅡ, 나머지는 ㅜ.
_BASE = {
    "あ": ("ㅇ", "ㅏ"), "い": ("ㅇ", "ㅣ"), "う": ("ㅇ", "ㅜ"), "え": ("ㅇ", "ㅔ"), "お": ("ㅇ", "ㅗ"),
    "か": ("ㅋ", "ㅏ"), "き": ("ㅋ", "ㅣ"), "く": ("ㅋ", "ㅜ"), "け": ("ㅋ", "ㅔ"), "こ": ("ㅋ", "ㅗ"),
    "が": ("ㄱ", "ㅏ"), "ぎ": ("ㄱ", "ㅣ"), "ぐ": ("ㄱ", "ㅜ"), "げ": ("ㄱ", "ㅔ"), "ご": ("ㄱ", "ㅗ"),
    "さ": ("ㅅ", "ㅏ"), "し": ("ㅅ", "ㅣ"), "す": ("ㅅ", "ㅡ"), "せ": ("ㅅ", "ㅔ"), "そ": ("ㅅ", "ㅗ"),
    "ざ": ("ㅈ", "ㅏ"), "じ": ("ㅈ", "ㅣ"), "ず": ("ㅈ", "ㅡ"), "ぜ": ("ㅈ", "ㅔ"), "ぞ": ("ㅈ", "ㅗ"),
    "た": ("ㅌ", "ㅏ"), "ち": ("ㅊ", "ㅣ"), "つ": ("ㅊ", "ㅡ"), "て": ("ㅌ", "ㅔ"), "と": ("ㅌ", "ㅗ"),
    "だ": ("ㄷ", "ㅏ"), "ぢ": ("ㅈ", "ㅣ"), "づ": ("ㅈ", "ㅡ"), "で": ("ㄷ", "ㅔ"), "ど": ("ㄷ", "ㅗ"),
    "な": ("ㄴ", "ㅏ"), "に": ("ㄴ", "ㅣ"), "ぬ": ("ㄴ", "ㅜ"), "ね": ("ㄴ", "ㅔ"), "の": ("ㄴ", "ㅗ"),
    "は": ("ㅎ", "ㅏ"), "ひ": ("ㅎ", "ㅣ"), "ふ": ("ㅎ", "ㅜ"), "へ": ("ㅎ", "ㅔ"), "ほ": ("ㅎ", "ㅗ"),
    "ば": ("ㅂ", "ㅏ"), "び": ("ㅂ", "ㅣ"), "ぶ": ("ㅂ", "ㅜ"), "べ": ("ㅂ", "ㅔ"), "ぼ": ("ㅂ", "ㅗ"),
    "ぱ": ("ㅍ", "ㅏ"), "ぴ": ("ㅍ", "ㅣ"), "ぷ": ("ㅍ", "ㅜ"), "ぺ": ("ㅍ", "ㅔ"), "ぽ": ("ㅍ", "ㅗ"),
    "ま": ("ㅁ", "ㅏ"), "み": ("ㅁ", "ㅣ"), "む": ("ㅁ", "ㅜ"), "め": ("ㅁ", "ㅔ"), "も": ("ㅁ", "ㅗ"),
    "や": ("ㅇ", "ㅑ"), "ゆ": ("ㅇ", "ㅠ"), "よ": ("ㅇ", "ㅛ"),
    "ら": ("ㄹ", "ㅏ"), "り": ("ㄹ", "ㅣ"), "る": ("ㄹ", "ㅜ"), "れ": ("ㄹ", "ㅔ"), "ろ": ("ㄹ", "ㅗ"),
    "わ": ("ㅇ", "ㅘ"), "を": ("ㅇ", "ㅗ"),
}

# 요음(拗音): 앞 모라(중성 ㅣ)의 초성 + 아래 중성. 예) きゃ=캬, しゅ=슈, きょ=쿄
_YOON = {"ゃ": "ㅑ", "ゅ": "ㅠ", "ょ": "ㅛ"}

# 작은 모음 단독(요음이 아닌 경우)의 대체 표기
_SMALL_VOWEL = {
    "ぁ": "아", "ぃ": "이", "ぅ": "우", "ぇ": "에", "ぉ": "오",
    "ゃ": "야", "ゅ": "유", "ょ": "요",
}


def _to_hiragana(text):
    # 가타카나를 히라가나로 정규화한다. (장음 ー(U+30FC) 는 그대로 둔다)
    out = []
    for ch in text:
        code = ord(ch)
        if 0x30A1 <= code <= 0x30F6:
            out.append(chr(code - 0x60))
        else:
            out.append(ch)
    return "".join(out)


def _kana_to_hangul(kana_text):
    # 히라가나 문자열을 한글 음차 문자열로 변환한다. 가나가 아닌 문자는 원문 유지.
    text = _to_hiragana(kana_text)
    result = []
    i = 0
    length = len(text)
    while i < length:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < length else ""

        if ch == "っ":  # 촉음: 앞 음절에 ㅅ 받침
            if result and _is_hangul(result[-1]):
                result[-1] = _add_final(result[-1], "ㅅ")
            i += 1
            continue

        if ch == "ん":  # 발음: 앞 음절에 ㄴ 받침
            if result and _is_hangul(result[-1]):
                result[-1] = _add_final(result[-1], "ㄴ")
            else:
                result.append("응")
            i += 1
            continue

        if ch == "ー":  # 장음: 앞 음절의 모음을 한 번 더
            if result and _is_hangul(result[-1]):
                result.append(_compose("ㅇ", _vowel_of(result[-1])))
            i += 1
            continue

        if ch in _BASE:
            cho, jung = _BASE[ch]
            if jung == "ㅣ" and nxt in _YOON:  # 요음 결합
                result.append(_compose(cho, _YOON[nxt]))
                i += 2
                continue
            result.append(_compose(cho, jung))
            i += 1
            continue

        if ch in _SMALL_VOWEL:  # 앞 모라와 결합하지 못한 작은 모음 단독
            result.append(_SMALL_VOWEL[ch])
            i += 1
            continue

        # 가나가 아닌 문자(구두점/숫자/영문 등)는 그대로 유지
        result.append(ch)
        i += 1

    return "".join(result)


def _transliterate_line(line):
    # pykakasi 로 한 줄을 조각내어 각 조각의 히라가나 읽기를 한글로 음차하고,
    # 조각 단위로 공백을 두어 단어 경계를 보존한다.
    parts = []
    for item in _kakasi.convert(line):
        hangul = _kana_to_hangul(item["hira"]).strip()
        if hangul:
            parts.append(hangul)
    return " ".join(parts)


def transliterate_to_korean(text):
    # 일본어(한자/가나 혼용) 텍스트를 한국어 발음(한글 음차)으로 변환한다.
    # 빈 입력은 예외를 던져 호출부(Result.failure)로 전파한다.
    if text is None or text.strip() == "":
        raise ValueError("text is empty")
    try:
        result_lines = []
        for line in text.split("\n"):
            if line.strip() == "":
                result_lines.append("")
            else:
                result_lines.append(_transliterate_line(line))
        return "\n".join(result_lines)
    except Exception as e:
        raise Exception("Transliteration failed: {}".format(e))
