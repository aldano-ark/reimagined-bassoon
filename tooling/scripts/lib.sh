# Shared POSIX functions. Entry points define NATIVE_ROOT from their own path.
# Each platform invocation has its own process scope, output directory, and log.

native_error() {
    printf 'Error: %s\n' "$*" >&2
    return 1
}

native_doctor() (
    case "$1" in
        android)
            if [ "${JAVA_HOME+x}" = x ]; then
                native_java="$JAVA_HOME/bin/java"
                [ -n "$JAVA_HOME" ] && [ -x "$native_java" ] || {
                    native_error 'JAVA_HOME must point to a JDK 17 installation.'; exit 1;
                }
            else
                native_java=$(command -v java) || {
                    native_error 'Java is missing. Set JAVA_HOME to a JDK 17 installation.'; exit 1;
                }
            fi
            native_java_version=$("$native_java" -version 2>&1) || {
                native_error 'Java could not start. Set JAVA_HOME to a working JDK 17 installation.'; exit 1;
            }
            native_java_major=$(printf '%s\n' "$native_java_version" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n 1)
            [ "$native_java_major" = 17 ] || {
                native_error 'This baseline requires JDK 17. Select it with JAVA_HOME.'; exit 1;
            }
            printf '%s\n' "$native_java_version"
            [ -n "${ANDROID_HOME:-}" ] && [ -f "$ANDROID_HOME/platforms/android-36/android.jar" ] || {
                native_error 'ANDROID_HOME must contain the Android SDK platform android-36.'; exit 1;
            }
            [ -d "$ANDROID_HOME/build-tools/36.0.0" ] || {
                native_error 'Install build-tools 36.0.0 in ANDROID_HOME with the SDK Manager.'; exit 1;
            }
            [ -x "$NATIVE_ROOT/apps/android/gradlew" ] &&
                [ -s "$NATIVE_ROOT/apps/android/gradle/wrapper/gradle-wrapper.jar" ] || {
                    native_error 'The checked-in Android Gradle wrapper is missing or not executable.'; exit 1;
                }
            printf 'Android SDK: %s\n' "$ANDROID_HOME"
            ;;
        ios)
            [ "$(uname -s)" = Darwin ] || {
                native_error 'iOS builds require macOS and Xcode.'; exit 1;
            }
            if [ "${DEVELOPER_DIR+x}" = x ] && [ ! -d "$DEVELOPER_DIR" ]; then
                native_error 'DEVELOPER_DIR must point to an installed Xcode developer directory.'
                exit 1
            fi
            command -v xcodebuild >/dev/null 2>&1 && xcodebuild -version || {
                native_error 'Xcode is unavailable. Complete Xcode setup or select it with DEVELOPER_DIR.'; exit 1;
            }
            native_ios_sdk=$(xcrun --sdk iphonesimulator --show-sdk-path) || {
                native_error 'The selected Xcode has no usable iOS Simulator SDK.'; exit 1;
            }
            [ -d "$native_ios_sdk" ] || {
                native_error 'The iOS Simulator SDK path does not exist.'; exit 1;
            }
            printf 'iOS Simulator SDK: %s\n' "$native_ios_sdk"
            ;;
    esac
)

native_compile() (
    native_platform=$1
    native_action=$2
    native_doctor "$native_platform" || exit $?
    case "$native_platform" in
        android)
            cd "$NATIVE_ROOT/apps/android" || exit 1
            set -- --no-daemon :app:assembleDebug
            if [ "$native_action" = verify ]; then
                set -- "$@" :app:lintDebug
            fi
            ./gradlew "$@" || exit $?
            native_artifact="$NATIVE_ROOT/apps/android/app/build/outputs/apk/debug/app-debug.apk"
            [ -s "$native_artifact" ] || {
                native_error "Missing or empty Android artifact: $native_artifact"; exit 1;
            }
            ;;
        ios)
            xcodebuild -project "$NATIVE_ROOT/apps/ios/NativeTemplate.xcodeproj" \
                -scheme NativeTemplate -configuration Debug \
                -destination 'generic/platform=iOS Simulator' \
                -derivedDataPath "$NATIVE_ROOT/.build/ios" \
                CODE_SIGNING_ALLOWED=NO build || exit $?
            native_artifact="$NATIVE_ROOT/.build/ios/Build/Products/Debug-iphonesimulator/NativeTemplate.app"
            [ -s "$native_artifact/NativeTemplate" ] &&
                [ -x "$native_artifact/NativeTemplate" ] &&
                [ -s "$native_artifact/Info.plist" ] || {
                    native_error "Missing or incomplete iOS artifact: $native_artifact"; exit 1;
                }
            ;;
    esac
    printf 'Artifact: %s\n' "$native_artifact"
)

native_build() { native_compile "$1" build; }
native_verify() { native_compile "$1" verify; }

native_run_action() {
    case "$1" in
        doctor) native_doctor "$2" ;;
        build) native_build "$2" ;;
        verify) native_verify "$2" ;;
    esac
}

native_logged() (
    native_log="$NATIVE_ROOT/.build/logs/$1/$2.log"
    shift 2
    mkdir -p "$(dirname "$native_log")" || exit 1
    if "$@" > "$native_log" 2>&1; then
        native_command_status=0
    else
        native_command_status=$?
    fi
    cat "$native_log"
    printf 'Log: %s\n' "$native_log"
    exit "$native_command_status"
)

native_dispatch() {
    native_action=$1
    shift
    if [ "$#" -ne 1 ]; then
        printf 'Usage: %s android|ios|all\n' "$0" >&2
        return 2
    fi
    case "$1" in
        android|ios) native_platforms=$1 ;;
        all) native_platforms='android ios' ;;
        *) printf 'Usage: %s android|ios|all\n' "$0" >&2; return 2 ;;
    esac
    native_first_status=0
    # Only the fixed platform names above undergo word splitting.
    for native_platform in $native_platforms; do
        if native_logged "$native_platform" "$native_action" native_run_action "$native_action" "$native_platform"; then
            printf '%s: passed\n' "$native_platform"
        else
            native_platform_status=$?
            printf '%s: failed (%s)\n' "$native_platform" "$native_platform_status" >&2
            if [ "$native_first_status" -eq 0 ]; then
                native_first_status=$native_platform_status
            fi
        fi
    done
    return "$native_first_status"
}
