import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[1] / "restore-postgres.ps1"


class RestorePostgresKvCompatibilityTest(unittest.TestCase):
    def test_accepts_kv_download_only_with_checksum_and_isolated_target(self):
        script = SCRIPT.read_text(encoding="utf-8")
        self.assertIn("GetFileName($resolvedBackup) -ne 'database.age'", script)
        self.assertIn("$resolvedBackup -notmatch '\\.dump\\.age$'", script)
        self.assertIn('Assert-PostgresTargetMatches', script)
        self.assertIn('"$resolvedBackup.sha256"', script)
        self.assertIn('Get-FileHash -LiteralPath $resolvedBackup -Algorithm SHA256', script)
        self.assertIn('Invoke-BinaryPipe', script)


if __name__ == "__main__":
    unittest.main()
