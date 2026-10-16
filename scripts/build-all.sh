#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mod_version=$(sed -n 's/^mod_version=//p' gradle.properties)

# Build sequentially: the versions share generated classes and resources.
for mc_version in 26.1 26.1.1 26.1.2 26.2 26.3; do
    ./gradlew build "-Pminecraft_version=$mc_version"
    mkdir -p "dist/$mc_version/test-results"
    for loader in fabric-quilt forge neoforge; do
        cp "build/libs/svc-password-hide-$mod_version+$mc_version-$loader.jar" "dist/$mc_version/"
    done
    cp build/test-results/test/TEST-*.xml "dist/$mc_version/test-results/"
done
cp README.md LICENSE dist/
mkdir -p dist/docs/images
cp docs/DEVELOPMENT.md dist/docs/
cp docs/images/group-password-hidden.png dist/docs/images/
