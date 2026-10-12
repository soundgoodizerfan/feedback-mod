#!/usr/bin/env bash
# PreToolUse gate (Edit|Write|MultiEdit): src/generated/resources is datagen
# output (FormItemModels, FormData). Hand edits are overwritten by runData.
set -euo pipefail

file_path=$(jq -r '.tool_input.file_path // empty')

case "$file_path" in
  */src/generated/*) ;;
  *) exit 0 ;;
esac

jq -n '{
  hookSpecificOutput: {
    hookEventName: "PreToolUse",
    permissionDecision: "deny",
    permissionDecisionReason: "src/generated is datagen output and is overwritten by ./gradlew runData. Edit the generator (data/ package, FormItemModels, FormData) or the hand-authored file under src/main/resources instead."
  }
}'
