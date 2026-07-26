#!/usr/bin/env python3
"""Report Compose resource-key gaps and optionally require complete locales."""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def string_keys(path: Path) -> set[str]:
    return {
        node.attrib["name"]
        for node in ET.parse(path).getroot()
        if node.tag == "string" and "name" in node.attrib
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--require-complete",
        action="append",
        default=[],
        metavar="LOCALE",
        help="Fail when this locale is missing a base string key; repeatable.",
    )
    args = parser.parse_args()
    required = set(args.require_complete)
    root = Path(__file__).resolve().parents[1]
    bases = sorted(root.glob("**/composeResources/values/strings.xml"))
    missing_required = False

    if not bases:
        print("No Compose string resources found", file=sys.stderr)
        return 2

    for base in bases:
        module = base.relative_to(root).as_posix().split("/src/", maxsplit=1)[0]
        base_keys = string_keys(base)
        locale_root = base.parent.parent
        for localized in sorted(locale_root.glob("values-*/strings.xml")):
            locale = localized.parent.name.removeprefix("values-")
            missing = sorted(base_keys - string_keys(localized))
            if not missing:
                continue
            print(f"{module} [{locale}]: {len(missing)} missing")
            print("  " + ", ".join(missing))
            missing_required = missing_required or locale in required

    if missing_required:
        print(
            "Required locale coverage is incomplete. Add reviewed strings or explicitly "
            "change the CI policy.",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
