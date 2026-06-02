filepath = 'shared/data/src/commonMain/kotlin/com/kintmin/data/local_db/dao_facade/AudioMediaFacade.kt'

with open(filepath, 'rb') as f:
    content = f.read()

FFFD = b'\xef\xbf\xbd'

replacements = [
    # Line 68: addTrack error - 추가할 수 없다
    (
        b'            error("?' + FFFD + b'\xec\xb2\xb4??' + b'\xeb\xaf\xb8\xeb\xb6\x84\xeb\xa5\x98\xeb\x8a\x94 \xec\xb6\x94' + FFFD + b'??????' + FFFD + b'\xeb\x8b\xa4.")',
        '            error("전체와 미분류는 추가할 수 없다.")'.encode('utf-8'),
    ),
    # Line 71: comment (12 spaces indent)
    (
        b'            // shouldInsertAtTop ?' + FFFD + b' ?' + FFFD + b'\xec\x9a\xb4\xeb\xa1\x9c\xeb\x93\x9c ?' + FFFD + b'\xec\x97\x90' + FFFD + b'??' + FFFD + b'\xec\x9a\xa9?' + FFFD + b'\xea\xb8\xb0 ?' + FFFD + b'\xeb\xac\xb8??false \xea\xb3\xa0\xec\xa0\x95',
        '            // shouldInsertAtTop은 다운로드 시에만 사용하기 때문에 false 고정'.encode('utf-8'),
    ),
    # Line 80: deleteTrack error - 지울 수 없다
    (
        b'            error("?' + FFFD + b'\xec\xb2\xb4??' + b'\xeb\xaf\xb8\xeb\xb6\x84\xeb\xa5\x98\xeb\x8a\x94 \xec\xa7\x80?????' + FFFD + b'\xeb\x8b\xa4.")',
        '            error("전체와 미분류는 지울 수 없다.")'.encode('utf-8'),
    ),
    # Line 239: comment (8 spaces indent)
    (
        b'        // shouldInsertAtTop ?' + FFFD + b' ?' + FFFD + b'\xec\x9a\xb4\xeb\xa1\x9c\xeb\x93\x9c ?' + FFFD + b'\xec\x97\x90' + FFFD + b'??' + FFFD + b'\xec\x9a\xa9?' + FFFD + b'\xea\xb8\xb0 ?' + FFFD + b'\xeb\xac\xb8??false \xea\xb3\xa0\xec\xa0\x95',
        '        // shouldInsertAtTop은 다운로드 시에만 사용하기 때문에 false 고정'.encode('utf-8'),
    ),
]

for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        print(f'Replaced: {repr(old[:40])}...')
    else:
        print(f'NOT FOUND: {repr(old[:40])}...')

with open(filepath, 'wb') as f:
    f.write(content)
print('Done!')
