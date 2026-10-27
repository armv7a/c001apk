#!/usr/bin/env python3
"""Reproduce the reviewed ARM64 libdu stack-copy fix, with exact input/output hashes."""

import argparse
import hashlib
from pathlib import Path

ORIGINAL_SHA256 = "3f37818d7aa8af50ad13956cfb3acfd72a447f721b77d3216043f710d57044ea"
PATCHED_SHA256 = "f03cb88122cfb3ad23e3554f93d8f98cede1625c661711428badb80dcb9c7be5"
OFFSET = 0x86048
ORIGINAL = bytes.fromhex("cb00005408040051a106009102050091e0030091a52efe97e0030091")
PATCHED = bytes.fromhex("6b000054a006009102000014e00300911f2003d51f2003d51f2003d5")


def patched_library(data: bytes) -> bytes:
    digest = hashlib.sha256(data).hexdigest()
    if digest == PATCHED_SHA256:
        return data
    if digest != ORIGINAL_SHA256 or data[OFFSET:OFFSET + len(ORIGINAL)] != ORIGINAL:
        raise ValueError("Unrecognized libdu.so; refusing to patch a different binary")
    result = data[:OFFSET] + PATCHED + data[OFFSET + len(ORIGINAL):]
    if hashlib.sha256(result).hexdigest() != PATCHED_SHA256:
        raise ValueError("Patched libdu.so hash does not match the reviewed binary")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("library", type=Path)
    parser.add_argument("--check", action="store_true", help="Verify the installed patch without writing")
    args = parser.parse_args()
    content = args.library.read_bytes()
    if args.check:
        if hashlib.sha256(content).hexdigest() != PATCHED_SHA256:
            parser.error("libdu.so does not contain the reviewed patch")
    else:
        args.library.write_bytes(patched_library(content))
    print("libdu.so atoi patch verified: " + PATCHED_SHA256)
