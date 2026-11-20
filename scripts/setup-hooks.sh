#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"
git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh
echo "Git hooks configured: core.hooksPath=$(git config --get core.hooksPath)"
