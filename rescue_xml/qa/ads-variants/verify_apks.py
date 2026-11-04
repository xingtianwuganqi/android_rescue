"""Verify compiled SDK isolation: python3 verify_apks.py noAds.apk domestic.apk."""
import struct
import sys
import zipfile
from pathlib import Path


def classes(apk):
    result = set()
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if not (name.startswith('classes') and name.endswith('.dex')):
                continue
            dex = archive.read(name)
            def uint(offset):
                return struct.unpack_from('<I', dex, offset)[0]
            strings = uint(60)
            types = uint(68)
            count, definitions = uint(96), uint(100)
            for index in range(count):
                type_index = uint(definitions + index * 32)
                string_index = uint(types + type_index * 4)
                offset = uint(strings + string_index * 4)
                while dex[offset] & 0x80:
                    offset += 1
                offset += 1  # skip the final UTF-16 length byte
                end = dex.index(b'\x00', offset)
                result.add(dex[offset:end].decode('utf-8', errors='replace'))
    return result


def verify(no_ads, domestic):
    plain, ads = classes(no_ads), classes(domestic)
    prefixes = ('Lcom/anythink/', 'Lcom/bytedance/sdk/openadsdk/', 'Lcom/smartdigimkt/')
    leaked = sorted(name for name in plain if name.startswith(prefixes))
    assert not leaked, f'Ad SDK classes leaked into noAds: {leaked[:10]}'
    for prefix in prefixes:
        assert any(name.startswith(prefix) for name in ads), f'Domestic SDK missing: {prefix}'
    demo = 'Lcom/rescue/flutter_720yun/ads/AdDemoActivity;'
    assert demo not in plain, 'Ad debug activity leaked into noAds'
    assert demo in ads, 'Expected domesticDebug APK with ad demo'
    for path in (no_ads, domestic):
        print(f'{path}: {Path(path).stat().st_size / 1024 / 1024:.1f} MiB')
    print('PASS: SDK classes isolated; ad debug activity domestic-only')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    verify(*sys.argv[1:])
