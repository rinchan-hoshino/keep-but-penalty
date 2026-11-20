#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_NAME="keep-but-penalty-verify:local"

USE_CONTAINER=0
IN_CONTAINER=0
for arg in "$@"; do
    case "$arg" in
        --container) USE_CONTAINER=1 ;;
        --in-container) IN_CONTAINER=1 ;;
        *)
            echo "Usage: $0 [--container]" >&2
            exit 2
            ;;
    esac
done

find_jdk() {
    local major="$1"
    local env_var="JAVA_${major}_HOME"
    if [[ -n "${!env_var:-}" && -x "${!env_var}/bin/java" ]]; then
        printf '%s\n' "${!env_var}"
        return 0
    fi
    for candidate in \
        "/opt/jdks/${major}" \
        "/opt/homebrew/opt/openjdk@${major}/libexec/openjdk.jdk/Contents/Home" \
        "/Users/rin/Library/Java/JavaVirtualMachines/temurin-${major}.jdk/Contents/Home" \
        /usr/lib/jvm/temurin-"${major}"-jdk-* \
        /opt/homebrew/Cellar/openjdk@"${major}"/*/libexec/openjdk.jdk/Contents/Home \
        "${HOME}"/.gradle/jdks/*-"${major}"-*/Contents/Home; do
        if [[ -x "${candidate}/bin/java" ]]; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    done
    return 1
}

if [[ "$IN_CONTAINER" -eq 0 ]]; then
    if [[ "$USE_CONTAINER" -eq 0 ]]; then
        if ! find_jdk 17 >/dev/null || ! find_jdk 21 >/dev/null || ! find_jdk 25 >/dev/null; then
            echo "[verify] Host is missing one or more required JDKs (17/21/25); falling back to container." >&2
            USE_CONTAINER=1
        fi
    fi

    if [[ "$USE_CONTAINER" -eq 1 ]]; then
        RUNTIME=""
        if command -v docker >/dev/null 2>&1; then
            RUNTIME="docker"
        elif command -v podman >/dev/null 2>&1; then
            RUNTIME="podman"
        else
            echo "[verify] Neither docker nor podman is available." >&2
            exit 1
        fi
        echo "[verify] Building ${IMAGE_NAME} via ${RUNTIME}..."
        "${RUNTIME}" build -f "${ROOT}/Dockerfile.verify" -t "${IMAGE_NAME}" "${ROOT}"
        echo "[verify] Running full verification suite inside container..."
        exec "${RUNTIME}" run --rm --init \
            -v "${ROOT}:/workspace" \
            -v rinchan-gradle-cache-kbp:/root/.gradle \
            -e ORG_GRADLE_PROJECT_neoform_decompile_maxMemory=2g \
            -e GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2560m" \
            -w /workspace \
            "${IMAGE_NAME}" \
            bash scripts/verify.sh --in-container
    fi
fi

export JAVA_17_HOME="$(find_jdk 17)"
export JAVA_21_HOME="$(find_jdk 21)"
export JAVA_25_HOME="$(find_jdk 25)"

cd "${ROOT}"
echo "[verify] Step 1/2: Full 8-target matrix build + JUnit + Property (>=1,000) + Architecture + JAR checks..."
python3 scripts/build-matrix.py

echo "[verify] Step 2/2: Packaged-JAR real Minecraft 1.21.1 NeoForge E2E + ConfigScreen lifecycle..."
python3 scripts/verify-e2e.py

echo "[verify] ALL VERIFICATION GATES PASSED."
