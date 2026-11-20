#!/usr/bin/env python3
"""Build all 8 supported loader/version targets from profiles.json and verify zero-dependency budgets."""
import argparse
import fcntl
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MAX_JAR_BYTES = 120 * 1024
FORBIDDEN_SERVER_REFS = (
    "net/minecraft/client/",
    "com/mojang/blaze3d/",
    "net/neoforged/neoforge/client/",
    "net/minecraftforge/client/",
    "KeepButPenaltyConfigScreen",
)


def discover_jdk(major: int) -> str:
    env_key = f"JAVA_{major}_HOME"
    if env_key in os.environ and Path(os.environ[env_key]).is_dir():
        return os.environ[env_key]
    candidates = [
        Path(f"/opt/jdks/{major}"),
        Path(f"/opt/homebrew/opt/openjdk@{major}/libexec/openjdk.jdk/Contents/Home"),
        Path(f"/Users/rin/Library/Java/JavaVirtualMachines/temurin-{major}.jdk/Contents/Home"),
    ]
    for pattern in (
        f"/usr/lib/jvm/temurin-{major}-jdk-*",
        f"/opt/homebrew/Cellar/openjdk@{major}/*/libexec/openjdk.jdk/Contents/Home",
        f"{Path.home()}/.gradle/jdks/*-{major}-*/Contents/Home",
    ):
        for p in sorted(Path("/").glob(pattern.lstrip("/")), reverse=True):
            candidates.append(p)
    for c in candidates:
        if (c / "bin/java").is_file():
            return str(c)
    raise RuntimeError(f"Could not find JDK {major}; set {env_key}")


def read_constant_pool_utf8s(data: bytes) -> list[str]:
    stream = io.BytesIO(data)
    magic = stream.read(4)
    if magic != b"\xca\xfe\xba\xbe":
        return []
    stream.read(4)  # minor, major
    (cp_count,) = struct.unpack(">H", stream.read(2))
    utf8s: list[str] = []
    i = 1
    while i < cp_count:
        tag = stream.read(1)[0]
        if tag == 1:
            (length,) = struct.unpack(">H", stream.read(2))
            utf8s.append(stream.read(length).decode("utf-8", errors="replace"))
        elif tag in (3, 4):
            stream.read(4)
        elif tag in (5, 6):
            stream.read(8)
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            stream.read(2)
        elif tag in (9, 10, 11, 12, 17, 18):
            stream.read(4)
        elif tag == 15:
            stream.read(3)
        else:
            raise RuntimeError(f"Unsupported constant pool tag: {tag}")
        i += 1
    return utf8s


def verify_jar(jar_path: Path):
    size = jar_path.stat().st_size
    if size >= MAX_JAR_BYTES:
        raise RuntimeError(f"JAR exceeds 120KB budget: {jar_path.name} ({size} bytes)")
    with zipfile.ZipFile(jar_path) as zf:
        names = set(zf.namelist())
        if "keep_but_penalty.mixins.json" not in names:
            raise RuntimeError(f"Missing keep_but_penalty.mixins.json in {jar_path.name}")
        mixin_cfg = json.loads(zf.read("keep_but_penalty.mixins.json").decode("utf-8"))
        mixin_pkg = mixin_cfg["package"].replace(".", "/")
        declared_mixins = set()
        for key in ("mixins", "client", "server"):
            for entry in mixin_cfg.get(key, []):
                declared_mixins.add(f"{mixin_pkg}/{entry.replace('.', '/')}.class")
        if not declared_mixins:
            raise RuntimeError(f"No mixins declared in {jar_path.name}")
        for expected in declared_mixins:
            if expected not in names:
                raise RuntimeError(f"Declared mixin missing from {jar_path.name}: {expected}")

        for name in names:
            lower = name.lower()
            if "rinlib" in lower:
                raise RuntimeError(f"Forbidden rinlib entry found in {jar_path.name}: {name}")
            if name.startswith("com/terraformersmc/modmenu/"):
                raise RuntimeError(f"Leaked ModMenu stub entry in {jar_path.name}: {name}")
            if not name.endswith(".class"):
                continue
            pool = read_constant_pool_utf8s(zf.read(name))
            for ref in pool:
                if "rinlib" in ref.lower():
                    raise RuntimeError(f"Forbidden rinlib reference in {jar_path.name}:{name} -> {ref}")
            is_client = "/client/" in name or name.startswith("client/")
            if not is_client:
                for ref in pool:
                    for forbidden in FORBIDDEN_SERVER_REFS:
                        if forbidden in ref:
                            raise RuntimeError(
                                f"Dedicated Server isolation violation in {jar_path.name}:{name} -> {ref}"
                            )
            if "Lorg/spongepowered/asm/mixin/Mixin;" in pool and name not in declared_mixins:
                raise RuntimeError(
                    f"Compiled @Mixin class not registered in keep_but_penalty.mixins.json ({jar_path.name}): {name}"
                )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", help="Optional single MC version to build")
    args = parser.parse_args()

    profiles = json.loads((ROOT / "profiles.json").read_text())
    version = next(
        line.split("=", 1)[1].strip()
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    )
    output = ROOT / "build/releases"
    output.mkdir(parents=True, exist_ok=True)
    lock = (ROOT / "build/matrix.lock").open("w")
    fcntl.flock(lock, fcntl.LOCK_EX)

    all_jdks = {major: discover_jdk(major) for major in sorted({p["java"] for p in profiles.values()})}
    installations_paths = ",".join(all_jdks.values())

    manifest = []
    for minecraft, profile in profiles.items():
        if args.profile and minecraft != args.profile:
            continue
        loaders = profile["loaders"]
        env = os.environ.copy()
        daemon_java = all_jdks.get(21) if profile["java"] < 21 else all_jdks[profile["java"]]
        env["JAVA_HOME"] = daemon_java

        tasks = []
        for loader in loaders:
            tasks.append(f":{loader}:clean")
        for loader in loaders:
            tasks.append(f":{loader}:build")

        command = [
            str(ROOT / "gradlew"),
            *tasks,
            "-Pminecraft_version=" + minecraft,
            "-Ploader=all",
            "-Porg.gradle.java.installations.paths=" + installations_paths,
            "--no-configuration-cache",
            "--max-workers=2",
        ]
        subprocess.run(command, cwd=ROOT, env=env, check=True)

        for loader in loaders:
            source = (
                ROOT
                / f"build/profiles/{minecraft}/{loader}/libs/keep_but_penalty-{loader}-{version}+{minecraft}.jar"
            )
            target = output / f"keep-but-penalty-{version}+{minecraft}-{loader}.jar"
            shutil.copy2(source, target)
            verify_jar(target)
            binary = target.read_bytes()
            manifest.append(
                dict(
                    minecraft=minecraft,
                    loader=loader,
                    file=str(target),
                    bytes=len(binary),
                    sha256=hashlib.sha256(binary).hexdigest(),
                    sha512=hashlib.sha512(binary).hexdigest(),
                )
            )
            print(f"BUILT {minecraft} {loader}: {target.name} ({len(binary):,} bytes)", flush=True)

    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    print(f"ALL DONE ({len(manifest)} targets).", flush=True)


if __name__ == "__main__":
    main()
