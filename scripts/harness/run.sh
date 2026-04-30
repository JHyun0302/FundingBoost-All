#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
PROFILE_DIR="$ROOT_DIR/harness/profiles"
ARTIFACT_ROOT="$ROOT_DIR/artifacts/harness"
mkdir -p "$ARTIFACT_ROOT"

PROFILE_ID=""
PROFILE_NAME=""
PROFILE_REASON=""
PASS_CONDITION=""
REQUIRED_FILES=()
REQUIRED_FILE_REASONS=()
CHECK_IDS=()
CHECK_CMDS=()
CHECK_REASONS=()

usage() {
  cat <<USAGE
Usage:
  ./scripts/harness/run.sh list
  ./scripts/harness/run.sh <profile-id> [profile-id ...]
  ./scripts/harness/run.sh all

Examples:
  ./scripts/harness/run.sh stage2-catalog-cutover
  ./scripts/harness/run.sh stage3-payment-cutover stage4-saga-foundation
USAGE
}

reset_profile_state() {
  PROFILE_ID=""
  PROFILE_NAME=""
  PROFILE_REASON=""
  PASS_CONDITION=""
  REQUIRED_FILES=()
  REQUIRED_FILE_REASONS=()
  CHECK_IDS=()
  CHECK_CMDS=()
  CHECK_REASONS=()
}

load_profile() {
  local profile_file="$1"
  reset_profile_state

  while IFS= read -r raw_line || [ -n "$raw_line" ]; do
    local line="${raw_line%$'\r'}"
    case "$line" in
      ""|\#*)
        continue
        ;;
      PROFILE_ID=*)
        PROFILE_ID="${line#PROFILE_ID=}"
        ;;
      PROFILE_NAME=*)
        PROFILE_NAME="${line#PROFILE_NAME=}"
        ;;
      PROFILE_REASON=*)
        PROFILE_REASON="${line#PROFILE_REASON=}"
        ;;
      PASS_CONDITION=*)
        PASS_CONDITION="${line#PASS_CONDITION=}"
        ;;
      REQUIRED_FILE::*)
        local entry path reason
        entry="${line#REQUIRED_FILE::}"
        IFS='|' read -r path reason <<< "$entry"
        REQUIRED_FILES+=("$path")
        REQUIRED_FILE_REASONS+=("${reason:-}")
        ;;
      CHECK::*)
        local entry check_id check_cmd check_reason rest
        entry="${line#CHECK::}"
        check_id="${entry%%|||*}"
        rest="${entry#*|||}"
        check_cmd="${rest%%|||*}"
        check_reason="${rest#*|||}"
        CHECK_IDS+=("$check_id")
        CHECK_CMDS+=("$check_cmd")
        CHECK_REASONS+=("${check_reason:-}")
        ;;
      *)
        echo "Unknown profile line: $line" >&2
        return 1
        ;;
    esac
  done < "$profile_file"

  if [ -z "$PROFILE_ID" ] || [ -z "$PROFILE_NAME" ] || [ -z "$PASS_CONDITION" ]; then
    echo "Invalid profile: $profile_file" >&2
    return 1
  fi
}

profile_file_from_id() {
  local profile_id="$1"
  local profile_file="$PROFILE_DIR/${profile_id}.profile"
  if [ ! -f "$profile_file" ]; then
    echo "Profile not found: $profile_id" >&2
    return 1
  fi
  printf '%s\n' "$profile_file"
}

list_profiles() {
  local profile_file
  for profile_file in "$PROFILE_DIR"/*.profile; do
    [ -e "$profile_file" ] || continue
    load_profile "$profile_file"
    printf '%-28s %s\n' "$PROFILE_ID" "$PROFILE_NAME"
  done
}

run_profile() {
  local profile_file="$1"
  local log_dir="$2"
  local profile_log
  local index

  load_profile "$profile_file"
  profile_log="$log_dir/${PROFILE_ID}.log"
  : > "$profile_log"

  echo
  echo "== $PROFILE_ID =="
  echo "$PROFILE_NAME"
  [ -n "$PROFILE_REASON" ] && echo "reason: $PROFILE_REASON"
  echo "pass:   $PASS_CONDITION"
  echo "log:    ${profile_log#$ROOT_DIR/}"

  for index in "${!REQUIRED_FILES[@]}"; do
    local required_path="$ROOT_DIR/${REQUIRED_FILES[$index]}"
    local required_reason="${REQUIRED_FILE_REASONS[$index]}"
    if [ -e "$required_path" ]; then
      printf '  [required] PASS %s\n' "${REQUIRED_FILES[$index]}"
    else
      printf '  [required] FAIL %s\n' "${REQUIRED_FILES[$index]}"
      [ -n "$required_reason" ] && printf '           %s\n' "$required_reason"
      return 1
    fi
  done

  for index in "${!CHECK_IDS[@]}"; do
    local check_id="${CHECK_IDS[$index]}"
    local check_cmd="${CHECK_CMDS[$index]}"
    local check_reason="${CHECK_REASONS[$index]}"

    printf '  [check]    RUN  %s\n' "$check_id"
    {
      echo "### $check_id"
      echo "$check_cmd"
      echo
    } >> "$profile_log"

    if /bin/bash -lc "cd \"$ROOT_DIR\" && $check_cmd" >> "$profile_log" 2>&1; then
      printf '  [check]    PASS %s\n' "$check_id"
    else
      printf '  [check]    FAIL %s\n' "$check_id"
      [ -n "$check_reason" ] && printf '           %s\n' "$check_reason"
      printf '           see %s\n' "${profile_log#$ROOT_DIR/}"
      return 1
    fi
  done
}

main() {
  if [ "$#" -eq 0 ]; then
    usage
    exit 1
  fi

  if [ "$1" = "list" ]; then
    list_profiles
    exit 0
  fi

  local run_id
  local log_dir
  local profile_id
  local profile_file
  local targets=()

  if [ "$1" = "all" ]; then
    while IFS= read -r profile_file; do
      [ -n "$profile_file" ] && targets+=("$profile_file")
    done < <(find "$PROFILE_DIR" -maxdepth 1 -type f -name '*.profile' | sort)
  else
    for profile_id in "$@"; do
      profile_file=$(profile_file_from_id "$profile_id")
      targets+=("$profile_file")
    done
  fi

  if [ "${#targets[@]}" -eq 0 ]; then
    echo "No profiles selected." >&2
    exit 1
  fi

  run_id=$(date '+%Y%m%d-%H%M%S')
  log_dir="$ARTIFACT_ROOT/$run_id"
  mkdir -p "$log_dir"

  echo "Harness run: $run_id"
  echo "Repository:  $ROOT_DIR"

  for profile_file in "${targets[@]}"; do
    run_profile "$profile_file" "$log_dir"
  done

  echo
  echo "Harness PASS (${#targets[@]} profile(s))"
  echo "Artifacts: artifacts/harness/$run_id"
}

main "$@"
