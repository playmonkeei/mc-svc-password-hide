#!/usr/bin/env python3
"""Package the complete set of CI-built client JARs for a GitHub release."""

import argparse
import hashlib
from pathlib import Path
import re
import shutil
import zipfile


MINECRAFT_VERSIONS = ("26.1", "26.1.1", "26.1.2", "26.2", "26.3")
LOADERS = ("fabric-quilt", "forge", "neoforge")
PROJECT_ROOT = Path(__file__).resolve().parent.parent


def digest(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()


def package(version, input_dir, output_dir):
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?", version):
        raise ValueError("Expected a semantic mod version, for example 1.0.1")
    expected = [
        (mc, f"svc-password-hide-{version}+{mc}-{loader}.jar")
        for mc in MINECRAFT_VERSIONS
        for loader in LOADERS
    ]
    jars = []
    for mc, name in expected:
        matches = list(input_dir.rglob(name))
        if len(matches) != 1:
            raise ValueError(f"Expected exactly one {name}, found {len(matches)}")
        file = matches[0]
        with zipfile.ZipFile(file) as jar:
            if jar.testzip() is not None:
                raise ValueError(f"Invalid JAR: {name}")
            if "svc-password-hide.mixins.json" not in jar.namelist():
                raise ValueError(f"Missing mod resources: {name}")
        jars.append((mc, file))

    documentation = [
        Path("README.md"),
        Path("LICENSE"),
        Path("docs/DEVELOPMENT.md"),
        Path("docs/images/group-password-hidden.png"),
    ]
    for relative in documentation:
        if not (PROJECT_ROOT / relative).is_file():
            raise ValueError(f"Missing documentation: {relative}")
    if output_dir.exists() and any(output_dir.iterdir()):
        raise ValueError("Output directory must be empty")
    output_dir.mkdir(parents=True, exist_ok=True)

    for _, file in jars:
        shutil.copy2(file, output_dir / file.name)
    bundle = output_dir / f"svc-password-hide-{version}.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as archive:
        for mc, file in jars:
            archive.write(file, f"{mc}/{file.name}")
        for relative in documentation:
            archive.write(PROJECT_ROOT / relative, relative.as_posix())
        archive.writestr("SHA256SUMS", "".join(
            f"{digest(file)}  {mc}/{file.name}\n" for mc, file in jars
        ))

    assets = sorted(output_dir.iterdir())
    (output_dir / "SHA256SUMS").write_text("".join(
        f"{digest(file)}  {file.name}\n" for file in assets
    ), encoding="utf-8")
    print(f"Packaged {len(jars)} client JARs, the complete ZIP and SHA256SUMS.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    package(args.version, args.input, args.output)
