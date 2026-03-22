#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/run_ios_app.sh [device_name]
# Optional env:
#   IOS_SCHEME=iosApp
#   IOS_CONFIGURATION=Debug
#   IOS_PROJECT_PATH=iosApp/iosApp.xcodeproj
#   IOS_DERIVED_DATA_PATH=.derivedData/iosApp
#   IOS_APP_NAME=iosApp
#   IOS_RUN_TARGET=auto|device|simulator
#   IOS_ALLOW_PROVISIONING_UPDATES=0|1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

DEVICE_NAME="${1:-${IOS_SIM_DEVICE_NAME:-iPhone 16}}"
SCHEME="${IOS_SCHEME:-iosApp}"
CONFIGURATION="${IOS_CONFIGURATION:-Debug}"
PROJECT_PATH="${IOS_PROJECT_PATH:-$ROOT_DIR/iosApp/iosApp.xcodeproj}"
DERIVED_DATA_PATH="${IOS_DERIVED_DATA_PATH:-$ROOT_DIR/.derivedData/iosApp}"
APP_NAME="${IOS_APP_NAME:-$SCHEME}"
RUN_TARGET="${IOS_RUN_TARGET:-auto}"
ALLOW_PROVISIONING_UPDATES="${IOS_ALLOW_PROVISIONING_UPDATES:-}"

case "$RUN_TARGET" in
  auto|device|simulator)
    ;;
  *)
    echo "Error: unsupported IOS_RUN_TARGET '$RUN_TARGET'. Expected auto, device, or simulator." >&2
    exit 1
    ;;
esac

case "${ALLOW_PROVISIONING_UPDATES:-auto}" in
  ""|auto|0|1)
    ;;
  *)
    echo "Error: unsupported IOS_ALLOW_PROVISIONING_UPDATES '$ALLOW_PROVISIONING_UPDATES'. Expected 0, 1, or unset." >&2
    exit 1
    ;;
esac

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: missing required tool '$1'." >&2
    exit 1
  fi
}

require_tool xcodebuild
require_tool xcrun
require_tool /usr/libexec/PlistBuddy

if [[ ! -d "$PROJECT_PATH" ]]; then
  echo "Error: Xcode project not found: $PROJECT_PATH" >&2
  exit 1
fi

find_connected_ios_device() {
  xcodebuild -project "$PROJECT_PATH" -scheme "$SCHEME" -showdestinations 2>/dev/null | awk '
    /Available destinations for the/ {
      in_destinations = 1
      next
    }
    !in_destinations {
      next
    }
    /\{[[:space:]]*platform:iOS,/ {
      line = $0
      if (line ~ /name:Any iOS Device/ || line ~ /id:dvtdevice-/) {
        next
      }

      id = ""
      name = ""
      if (match(line, /id:[^,}]+/)) {
        id = substr(line, RSTART + 3, RLENGTH - 3)
      }
      if (match(line, /name:[^,}]+/)) {
        name = substr(line, RSTART + 5, RLENGTH - 5)
      }

      gsub(/^[[:space:]]+|[[:space:]]+$/, "", id)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)

      if (id != "" && name != "") {
        print id "\t" name
        exit
      }
    }
  '
}

DEVICE_SELECTION=""
if [[ "$RUN_TARGET" != "simulator" ]]; then
  DEVICE_SELECTION="$(find_connected_ios_device || true)"
fi

if [[ -n "$DEVICE_SELECTION" ]]; then
  TARGET_KIND="device"
  UDID="${DEVICE_SELECTION%%$'\t'*}"
  ACTUAL_DEVICE_NAME="${DEVICE_SELECTION#*$'\t'}"
  SDK_SUFFIX="iphoneos"
  DESTINATION="id=$UDID"
else
  if [[ "$RUN_TARGET" == "device" ]]; then
    echo "Error: IOS_RUN_TARGET=device, but no connected iOS device was found for scheme '$SCHEME'." >&2
    exit 1
  fi

  TARGET_KIND="simulator"
  UDID="$(IOS_SIM_UDID_ONLY=1 "$SCRIPT_DIR/start_ios_simulator.sh" "$DEVICE_NAME")"
  ACTUAL_DEVICE_NAME="$(xcrun simctl list devices available | awk -v id="$UDID" '
    index($0, id) {
      line = $0
      sub(/^[[:space:]]*/, "", line)
      sub(/[[:space:]]+\([0-9A-Fa-f-]{36}\).*/, "", line)
      print line
      exit
    }
  ')"
  if [[ -z "$ACTUAL_DEVICE_NAME" ]]; then
    ACTUAL_DEVICE_NAME="$DEVICE_NAME"
  fi
  SDK_SUFFIX="iphonesimulator"
  DESTINATION="platform=iOS Simulator,id=$UDID"
fi

XCODEBUILD_ARGS=(
  -project "$PROJECT_PATH"
  -scheme "$SCHEME"
  -configuration "$CONFIGURATION"
  -destination "$DESTINATION"
  -derivedDataPath "$DERIVED_DATA_PATH"
)

if [[ "$TARGET_KIND" == "device" ]]; then
  SHOULD_ALLOW_PROVISIONING_UPDATES="$ALLOW_PROVISIONING_UPDATES"
  if [[ -z "$SHOULD_ALLOW_PROVISIONING_UPDATES" || "$SHOULD_ALLOW_PROVISIONING_UPDATES" == "auto" ]]; then
    SHOULD_ALLOW_PROVISIONING_UPDATES="1"
  fi
  if [[ "$SHOULD_ALLOW_PROVISIONING_UPDATES" == "1" ]]; then
    XCODEBUILD_ARGS+=(-allowProvisioningUpdates)
  fi
fi

echo "Using $TARGET_KIND: $ACTUAL_DEVICE_NAME ($UDID)"
echo "Building scheme '$SCHEME' ($CONFIGURATION) ..."
xcodebuild "${XCODEBUILD_ARGS[@]}" build

PRODUCTS_DIR="$DERIVED_DATA_PATH/Build/Products/${CONFIGURATION}-${SDK_SUFFIX}"
if [[ ! -d "$PRODUCTS_DIR" ]]; then
  echo "Error: expected products dir not found: $PRODUCTS_DIR" >&2
  exit 1
fi

APP_PATH="$(find "$PRODUCTS_DIR" -maxdepth 1 -type d -name "$APP_NAME.app" | head -n1)"
if [[ -z "$APP_PATH" ]]; then
  APP_PATH="$(find "$PRODUCTS_DIR" -maxdepth 1 -type d -name "*.app" | head -n1)"
fi
if [[ -z "$APP_PATH" || ! -d "$APP_PATH" ]]; then
  echo "Error: built app not found under $PRODUCTS_DIR" >&2
  exit 1
fi

INFO_PLIST="$APP_PATH/Info.plist"
if [[ ! -f "$INFO_PLIST" ]]; then
  echo "Error: Info.plist not found in app bundle: $APP_PATH" >&2
  exit 1
fi

BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print:CFBundleIdentifier' "$INFO_PLIST")"
if [[ -z "$BUNDLE_ID" ]]; then
  echo "Error: CFBundleIdentifier is empty in $INFO_PLIST" >&2
  exit 1
fi

echo "Installing app: $APP_PATH"
if [[ "$TARGET_KIND" == "device" ]]; then
  xcrun devicectl device install app --device "$UDID" "$APP_PATH"

  echo "Launching app: $BUNDLE_ID"
  if ! xcrun devicectl device process launch --device "$UDID" --terminate-existing "$BUNDLE_ID"; then
    echo "Error: launch failed for $BUNDLE_ID on $UDID" >&2
    exit 1
  fi

  echo "App is running on device: $ACTUAL_DEVICE_NAME ($UDID)"
else
  xcrun simctl install "$UDID" "$APP_PATH"

  echo "Launching app: $BUNDLE_ID"
  if ! xcrun simctl launch "$UDID" "$BUNDLE_ID"; then
    echo "Error: launch failed for $BUNDLE_ID on $UDID" >&2
    echo "Try checking logs with: xcrun simctl spawn $UDID log stream --level debug --style compact --predicate 'process == \"$APP_NAME\"'" >&2
    exit 1
  fi

  echo "App is running on simulator: $ACTUAL_DEVICE_NAME ($UDID)"
fi
