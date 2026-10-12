#!/usr/bin/env bash
# PostToolUse (Edit|Write|MultiEdit): compile after a .java edit and feed
# errors back to the model. Silent on success. Exit 2 = report to Claude.
set -uo pipefail

file_path=$(jq -r '.tool_input.file_path // empty')

case "$file_path" in
  *.java) ;;
  *) exit 0 ;;
esac

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
out=$(./gradlew compileJava -q --console=plain 2>&1)
status=$?
if [[ $status -ne 0 ]]; then
  printf '%s\n' "$out" | grep -E 'error:|warning: \[|FAILED|What went wrong' -A2 | head -40 >&2
  exit 2
fi
