import pykakasi
from hangulize import hangulize

# pykakasi 인스턴스는 재사용한다 (사전 로딩 비용이 크다)
_kakasi = pykakasi.kakasi()

# 가타카나 유니코드 범위 (U+30A0 ~ U+30FF)
_KATAKANA_START = "゠"
_KATAKANA_END = "ヿ"


def _has_katakana(segment):
    return any(_KATAKANA_START <= ch <= _KATAKANA_END for ch in segment)


def _transliterate_line(line):
    # pykakasi 로 한 줄을 형태소 단위로 나눠 각 조각의 가타카나 읽기를 얻는다.
    # 가타카나 조각은 hangulize('jpn') 로 한글 음차하고, 구두점 등은 원문을 유지한다.
    parts = []
    for item in _kakasi.convert(line):
        kana = item["kana"]
        if _has_katakana(kana):
            parts.append(hangulize(kana, "jpn"))
        else:
            parts.append(item["orig"])
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
        raise Exception(f"Transliteration failed: {e}")
