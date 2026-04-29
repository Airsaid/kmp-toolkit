#!/usr/bin/env bash
set -euo pipefail

rg_or_grep() {
  if command -v rg >/dev/null 2>&1; then
    rg "$@"
  else
    grep -E "$@"
  fi
}

version_from_gradle() {
  rg_or_grep -n '^VERSION_NAME=' gradle.properties | head -n1 | cut -d= -f2
}

require_pattern() {
  local file="$1"
  local pattern="$2"
  local message="$3"

  if ! rg_or_grep -q "$pattern" "$file"; then
    echo "$message" >&2
    exit 1
  fi
}

version="${1:-}"
if [[ -z "$version" ]]; then
  version="$(version_from_gradle)"
fi

gradle_version="$(version_from_gradle)"
if [[ "$gradle_version" != "$version" ]]; then
  echo "VERSION_NAME in gradle.properties is '$gradle_version', expected '$version'." >&2
  exit 1
fi

for file in README.md README.zh.md; do
  pattern="com\\.airsaid:toolkit:(\\\$version|${version})"
  require_pattern "$file" "$pattern" "Expected $file to reference version $version."
done

for file in toolkit/README.md toolkit/README.zh.md; do
  pattern="com\\.airsaid:toolkit:(\\\$version|${version})"
  require_pattern "$file" "$pattern" "Expected $file to reference version $version."
done

require_pattern README.md "\\[中文说明\\]\\(README\\.zh\\.md\\)" "Expected README.md to link to README.zh.md."
require_pattern README.zh.md "\\[English\\]\\(README\\.md\\)" "Expected README.zh.md to link to README.md."
require_pattern toolkit/README.md "\\[中文说明\\]\\(README\\.zh\\.md\\)" "Expected toolkit/README.md to link to toolkit/README.zh.md."
require_pattern toolkit/README.zh.md "\\[English\\]\\(README\\.md\\)" "Expected toolkit/README.zh.md to link to toolkit/README.md."

require_pattern README.md "\\[toolkit/README\\.md\\]\\(toolkit/README\\.md\\)" "Expected README.md to link to toolkit/README.md."
require_pattern README.zh.md "\\[toolkit/README\\.zh\\.md\\]\\(toolkit/README\\.zh\\.md\\)" "Expected README.zh.md to link to toolkit/README.zh.md."
