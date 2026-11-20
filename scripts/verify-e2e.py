#!/usr/bin/env python3
"""Run deliverable-level packaged-JAR real Minecraft 1.21.1 NeoForge E2E + ConfigScreen lifecycle checks."""
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def discover_jdk21() -> str:
    env_val = os.environ.get("JAVA_21_HOME")
    if env_val and Path(env_val, "bin/java").is_file():
        return env_val
    candidates = [
        Path("/opt/jdks/21"),
        Path("/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"),
        Path("/Users/rin/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"),
    ]
    for pattern in (
        "/usr/lib/jvm/temurin-21-jdk-*",
        "/opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home",
        f"{Path.home()}/.gradle/jdks/*-21-*/Contents/Home",
    ):
        for p in sorted(Path("/").glob(pattern.lstrip("/")), reverse=True):
            candidates.append(p)
    for c in candidates:
        if (c / "bin/java").is_file():
            return str(c)
    raise RuntimeError("Could not find JDK 21; set JAVA_21_HOME")


def main():
    jdk21 = discover_jdk21()
    env = os.environ.copy()
    env["JAVA_HOME"] = jdk21

    cmd = [
        str(ROOT / "gradlew"),
        ":neoforge:jar",
        ":neoforge:test",
        ":neoforge:runGameTestServer",
        "-Pminecraft_version=1.21.1",
        "-Ploader=neoforge",
        f"-Porg.gradle.java.installations.paths={jdk21}",
        "--no-configuration-cache",
    ]
    print("Running packaged-JAR E2E + ConfigScreen + Property + Architecture verification (1.21.1 NeoForge)...", flush=True)
    subprocess.run(cmd, cwd=ROOT, env=env, check=True)
    print("E2E VERIFICATION PASSED.", flush=True)


if __name__ == "__main__":
    main()
