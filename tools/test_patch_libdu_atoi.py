import hashlib
from pathlib import Path
import unittest

from patch_libdu_atoi import ORIGINAL, ORIGINAL_SHA256, OFFSET, PATCHED_SHA256, patched_library


class LibduPatchTest(unittest.TestCase):
    def test_packaged_library_matches_reviewed_patch_and_can_be_reproduced(self):
        path = Path(__file__).resolve().parents[1] / "app/src/main/jniLibs/arm64-v8a/libdu.so"
        patched = path.read_bytes()
        self.assertEqual(PATCHED_SHA256, hashlib.sha256(patched).hexdigest())
        original = patched[:OFFSET] + ORIGINAL + patched[OFFSET + len(ORIGINAL):]
        self.assertEqual(ORIGINAL_SHA256, hashlib.sha256(original).hexdigest())
        self.assertEqual(patched, patched_library(original))
        self.assertEqual(patched, patched_library(patched))

    def test_unknown_binary_is_rejected(self):
        with self.assertRaises(ValueError):
            patched_library(b"unrelated library")
