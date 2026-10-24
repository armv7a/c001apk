import unittest
from verify_apk import certificate_digest

class CertificateReportTest(unittest.TestCase):
    def test_accepts_both_tool_report_prefixes(self):
        digest = "ab" * 32
        for prefix in ("Signer #1", "Signer"):
            self.assertEqual(digest, certificate_digest(f"{prefix} certificate SHA-256 digest: {digest.upper()}\n"))
    def test_never_confuses_source_stamp_or_different_signers(self):
        with self.assertRaises(ValueError):
            certificate_digest("Source Stamp Signer certificate SHA-256 digest: " + "ab" * 32)
        with self.assertRaises(ValueError):
            certificate_digest("Signer #1 certificate SHA-256 digest: " + "ab" * 32 + "\nSigner #2 certificate SHA-256 digest: " + "cd" * 32)
if __name__ == "__main__": unittest.main()
