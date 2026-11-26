#!/usr/bin/env python3
"""세션이 시작될 때 `docs/` 의 기능·실험 문서 목록을 모델 컨텍스트에 넣는다.

목록을 문서에 복제하지 않으려고(루트 `AGENTS.md` §7 — 폴더가 목록이다) 폴더를 그때그때
훑는다. 문서를 더하거나 지우거나 이름을 바꾸면 다음 세션에 그대로 반영된다.

경로는 이 파일 위치에서 거슬러 올라가 잡는다 — 실행될 때의 작업 디렉터리에 의존하지 않는다.
"""

import glob
import json
import os
import sys

HOOK_EVENT = "SessionStart"

GUIDE = (
    "`docs/` 가 사용자 관점의 기능 문서를 갖는다. **기능의 동작·정책·제약을 바꾸거나 더하기 전에"
    " 그 기능의 문서를 읽는다.** 코드 구조와 1:1 이 아니므로 아래 이름에서 고른다."
    " 가로지르는 것은 필요할 때만 본다 — `ui-rationale` 은 화면의 형태·배치·흐름을 정할 때,"
    " `DECISIONS` 는 되돌리기 어려운 기술 선택을 뒤집으려 할 때다."
    " 문서를 쓰고 고치는 규칙은 `docs/AGENTS.md` 에 있다."
)


def doc_names(directory: str) -> list[str]:
    return sorted(os.path.basename(path)[:-3] for path in glob.glob(os.path.join(directory, "*.md")))


def main() -> None:
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    docs = os.path.join(root, "docs")
    crosscutting = [name for name in doc_names(docs) if name != "AGENTS"]
    features = doc_names(os.path.join(docs, "features"))
    experiments = doc_names(os.path.join(docs, "experiments"))
    if not features and not experiments:
        return

    lines = [GUIDE, ""]
    if crosscutting:
        lines.append("`docs/` — 기능을 가로지르는 것: " + ", ".join(crosscutting))
    if features:
        lines.append("`docs/features/` — 지금 있는 기능: " + ", ".join(features))
    if experiments:
        lines.append("`docs/experiments/` — 지금 없는 것(아이디어·접은 시도): " + ", ".join(experiments))

    sys.stdout.reconfigure(encoding="utf-8")
    print(
        json.dumps(
            {"hookSpecificOutput": {"hookEventName": HOOK_EVENT, "additionalContext": "\n".join(lines)}},
            ensure_ascii=False,
        )
    )


main()
