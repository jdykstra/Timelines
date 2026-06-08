#!/bin/sh
set -eu

SRCDIR=$1
BUILDDIR=$2
OUTPUT_JAR=$3
MAIN_CLASS=$4

mkdir -p "$BUILDDIR/classes"
rm -rf "$BUILDDIR/classes"/*

javac -d "$BUILDDIR/classes" "$SRCDIR"/*.java

cat > "$BUILDDIR/manifest.mf" <<EOF
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS
EOF

jar cfm "$BUILDDIR/$OUTPUT_JAR" "$BUILDDIR/manifest.mf" \
  -C "$BUILDDIR/classes" . \
  -C "$SRCDIR" Icons

rm -f "$BUILDDIR/manifest.mf"
rm -rf "$BUILDDIR/classes"
