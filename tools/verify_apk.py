#!/usr/bin/env python3
"""Verify a personal Black APK before handing it to the player; no device is needed."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile


def verify(apk, build_tools, package, version, commit, certificate):
    badging = subprocess.check_output([str(build_tools / 'aapt2'), 'dump', 'badging', str(apk)], text=True)
    identity = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging)
    if not identity or identity[1] != package or identity[3] != version:
        raise ValueError('APK package/version does not match the requested personal build')
    environment = os.environ.copy()
    if environment.get('JAVA_HOME'):
        environment['PATH'] = str(Path(environment['JAVA_HOME']) / 'bin') + os.pathsep + environment.get('PATH', '')
    signature = subprocess.check_output([str(build_tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)], text=True, env=environment)
    signer = re.search(r'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)', signature)
    expected = certificate.replace(':', '').lower()
    if not signer or signer[1].lower() != expected:
        raise ValueError('APK is not signed with the existing personal certificate')
    if 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signature:
        raise ValueError('APK Signature Scheme v2 verification did not pass')
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        libraries = {name for name in names if name.startswith('lib/') and name.endswith('.so')}
        required = {'lib/arm64-v8a/' + name for name in ('libmain.so', 'libSDL3.so', 'libc++_shared.so')}
        if not required <= libraries or any(not name.startswith('lib/arm64-v8a/') for name in libraries):
            raise ValueError('APK must contain all three arm64 runtime libraries and no other ABI')
        if any(Path(name).suffix.lower() in {'.iso', '.xbe'} for name in names):
            raise ValueError('APK contains bundled game assets')
        main = archive.read('lib/arm64-v8a/libmain.so')
        if commit[:7].encode() not in main:
            raise ValueError('Native library does not identify the expected source commit')
    digest = hashlib.sha256()
    with apk.open('rb') as content:
        for block in iter(lambda: content.read(1024 * 1024), b''):
            digest.update(block)
    return {
        'apk': str(apk.resolve()), 'package': identity[1], 'version_code': identity[2], 'version': identity[3],
        'source_commit': commit, 'certificate_sha256': signer[1].lower(),
        'sha256': digest.hexdigest(),
        'bytes': apk.stat().st_size, 'native_libraries': sorted(libraries),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--build-tools', type=Path, default=Path(os.environ.get('ANDROID_HOME', '/opt/android-sdk')) / 'build-tools' / '34.0.0')
    parser.add_argument('--package', default='com.aqsama.black')
    parser.add_argument('--version', required=True)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--certificate', default='31266d1d1618f15e74419431e689f505d679433bba90e02debda5c1de2268d1a')
    args = parser.parse_args()
    try:
        result = verify(args.apk, args.build_tools, args.package, args.version, args.commit, args.certificate)
    except (ValueError, OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
