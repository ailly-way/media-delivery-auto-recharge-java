#!/bin/sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/media-delivery-java-build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src -name '*.java' -print)

if [ "${1:-run}" = "test" ]; then
  java -cp "$BUILD_DIR" example.media.MediaDeliveryServiceTest
else
  java -cp "$BUILD_DIR" example.media.Main
fi
