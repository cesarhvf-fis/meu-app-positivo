"""Artificial credentials only; no account access or network requests."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('audit_public', Path(__file__).resolve().parents[1] / 'tools/audit_public.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class PublicationAuditTests(unittest.TestCase):
    def test_ignored_files_are_still_checked(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / '.gitignore').write_text('*.jks\n', encoding='utf-8')
            (root / 'private.jks').write_bytes(b'synthetic')
            _, findings = audit.scan(root)
            self.assertTrue(any(path.name == 'private.jks' for path, _ in findings))

    def test_literal_token_and_private_path_are_flagged(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'capture.json').write_text(json.dumps({'token': 'synthetic-credential-for-test'}), encoding='utf-8')
            (root / 'capture.txt').write_text('C:' + chr(92) + 'Users' + chr(92) + 'SyntheticUser' + chr(92) + 'capture', encoding='utf-8')
            _, findings = audit.scan(root)
            self.assertEqual({path.name for path, _ in findings}, {'capture.json', 'capture.txt'})

    def test_example_has_no_real_credentials(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'protocol.example.json').write_text('{"appSecret":"PREENCHER_APP_SECRET"}', encoding='utf-8')
            _, findings = audit.scan(root)
            self.assertEqual(findings, [])


if __name__ == '__main__':
    unittest.main()
