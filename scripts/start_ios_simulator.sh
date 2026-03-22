#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/start_ios_simulator.sh [device_name]
# Optional env:
#   IOS_SIM_UDID_ONLY=1  # only print UDID

REQUESTED_DEVICE_NAME="${1:-${IOS_SIM_DEVICE_NAME:-iPhone 16}}"

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: missing required tool '$1'." >&2
    exit 1
  fi
}

require_full_xcode() {
  local dev_dir
  dev_dir="$(xcode-select -p 2>/dev/null || true)"
  if [[ -z "$dev_dir" || "$dev_dir" == "/Library/Developer/CommandLineTools" ]]; then
    cat >&2 <<MSG
Error: full Xcode is not selected.
Please install/select Xcode first, e.g.:
  sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
MSG
    exit 1
  fi
}

extract_udid() {
  local name="$1"
  xcrun simctl list devices available | awk -v name="$name" '
    {
      if (index($0, name " (") > 0) {
        if (match($0, /\([0-9A-Fa-f-][0-9A-Fa-f-]*\)/)) {
          udid = substr($0, RSTART + 1, RLENGTH - 2)
          if (length(udid) == 36) {
            print udid
          }
        }
      }
    }
  ' | tail -n1
}

select_fallback_iphone() {
  # Prefer currently booted iPhone, otherwise first available iPhone.
  xcrun simctl list devices available | awk '
    /iPhone/ && /\(Booted\)/ && !booted {
      if (match($0, /\([0-9A-Fa-f-][0-9A-Fa-f-]*\)/)) {
        udid = substr($0, RSTART + 1, RLENGTH - 2)
        if (length(udid) == 36) {
          booted = udid
        }
      }
    }
    /iPhone/ && !first {
      if (match($0, /\([0-9A-Fa-f-][0-9A-Fa-f-]*\)/)) {
        udid = substr($0, RSTART + 1, RLENGTH - 2)
        if (length(udid) == 36) {
          first = udid
        }
      }
    }
    END {
      if (booted) print booted;
      else if (first) print first;
    }
  '
}

device_name_by_udid() {
  local id="$1"
  xcrun simctl list devices available | awk -v id="$id" '
    index($0, id) {
      line = $0
      sub(/^[[:space:]]*/, "", line)
      sub(/[[:space:]]+\([0-9A-Fa-f-]{36}\).*/, "", line)
      print line
      exit
    }
  '
}

list_available_iphone_names() {
  xcrun simctl list devices available | awk '
    /iPhone/ {
      line = $0
      sub(/^[[:space:]]*/, "", line)
      sub(/[[:space:]]+\([0-9A-Fa-f-]{36}\).*/, "", line)
      print line
    }
  '
}

require_tool xcode-select
require_tool xcrun
require_full_xcode

if ! xcrun simctl list devices available >/dev/null 2>&1; then
  echo "Error: unable to access simulator devices via simctl." >&2
  exit 1
fi

UDID="$(extract_udid "$REQUESTED_DEVICE_NAME")"
if [[ -z "$UDID" ]]; then
  UDID="$(select_fallback_iphone)"
fi
if [[ -z "$UDID" ]]; then
  echo "Error: no available iPhone simulator found." >&2
  echo "Tip: open Xcode > Settings > Platforms, install an iOS Simulator runtime first." >&2
  exit 1
fi

DEVICE_NAME="$(device_name_by_udid "$UDID")"
if [[ -z "$DEVICE_NAME" ]]; then
  DEVICE_NAME="$REQUESTED_DEVICE_NAME"
fi

STATE="$(xcrun simctl list devices | awk -v id="$UDID" 'index($0, id) { if (index($0, "(Booted)")>0) print "Booted"; else print "Shutdown"; exit }')"
if [[ "$STATE" != "Booted" ]]; then
  xcrun simctl boot "$UDID" >/dev/null
fi

open -a Simulator >/dev/null 2>&1 || true
xcrun simctl bootstatus "$UDID" -b >/dev/null

if [[ "${IOS_SIM_UDID_ONLY:-0}" == "1" ]]; then
  echo "$UDID"
else
  if [[ "$REQUESTED_DEVICE_NAME" != "$DEVICE_NAME" ]]; then
    echo "Requested simulator '$REQUESTED_DEVICE_NAME' not found, switched to '$DEVICE_NAME'."
    echo "Available iPhone simulators:"
    list_available_iphone_names | sed 's/^/  - /'
  fi
  echo "Simulator ready: $DEVICE_NAME ($UDID)"
fi
