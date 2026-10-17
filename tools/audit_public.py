"""Complementary pre-publication checks. Reports locations, never matched values.

Usage: python tools/audit_public.py .
This scans the current tree, not previous Git commits or downloaded releases.
"""
from pathlib import Path
import re
import sys

FORBIDDEN_SUFFIXES = {
    '.jks', '.keystore', '.password', '.tkn', '.p12', '.pfx', '.pem', '.key',
    '.enc', '.apk', '.aab', '.dex', '.class', '.har', '.db', '.sqlite', '.zip',
    '.png', '.jpg', '.jpeg', '.log',
}
FORBIDDEN_NAMES = {'commands.json', 'computername.cfg', 'local.properties', 'LocalProtocolConfig.java'}
PATTERNS = {
    'private key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY-----'),
    'GitHub credential': re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})'),
    'AWS credential': re.compile(rb'(?:AKIA|ASIA)[A-Z0-9]{16}'),
    'JWT-like token': re.compile(rb'eyJ[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}'),
    'private Windows path': re.compile(rb'[A-Za-z]:[\\/]Users[\\/][^\\/\s]+[\\/]'),
    'email address': re.compile(rb'[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'),
    'embedded OEM credential': re.compile(rb'A_[A-Za-z0-9_]{50,}'),
}


def scan(root):
    findings = []
    count = 0
    for path in sorted(root.rglob('*')):
        relative = path.relative_to(root)
        if '.git' in relative.parts:
            continue
        if path.is_symlink():
            findings.append((relative, 'symbolic link: inspect target separately'))
            continue
        if not path.is_file():
            continue
        count += 1
        if path.suffix.lower() in FORBIDDEN_SUFFIXES or path.name in FORBIDDEN_NAMES or path.name.startswith('.env') or path.name.endswith('.local.json'):
            findings.append((relative, 'file must stay outside public source'))
        if any(part in {'node_modules', 'build', 'work', 'dist', '.gradle', '.idea', '__pycache__'} for part in relative.parts):
            findings.append((relative, 'generated/private directory'))
        data = path.read_bytes()
        for name, pattern in PATTERNS.items():
            if pattern.search(data):
                findings.append((relative, name))
        # Check real-looking JSON assignments; source identifiers and placeholders are allowed.
        for match in re.finditer(rb'"(?:token|sid|localKey|appSecret|password)"\s*:\s*"([^"\r\n]+)"', data):
            value = match.group(1)
            if not value.startswith((b'PREENCHER', b'fake-test-', b'test-only')):
                findings.append((relative, 'literal credential assignment'))
    return count, findings


if __name__ == '__main__':
    root = Path(sys.argv[1] if len(sys.argv) > 1 else '.').resolve()
    count, findings = scan(root)
    for path, reason in findings:
        print(f'REVIEW: {path.as_posix()} — {reason}')
    print(f'{count} files examined; {len(findings)} findings. No matched values printed.')
    print('This check does not prove absence of secrets; review changes and Git history separately.')
    raise SystemExit(bool(findings))
