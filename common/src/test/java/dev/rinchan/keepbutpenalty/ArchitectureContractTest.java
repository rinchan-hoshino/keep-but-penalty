package dev.rinchan.keepbutpenalty;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated architecture guardrails inspecting the real packaged mod JAR:
 * 1. Dedicated Server isolation (no client/GUI references outside client packages).
 * 2. Mixin bidirectional closure (mixins.json <-> compiled classes & @Mixin sources).
 * 3. Clean package redline (zero rinlib, zero modmenu compile-only stub leakage).
 */
class ArchitectureContractTest {
    private static final List<String> FORBIDDEN_SERVER_REFS = List.of(
            "net/minecraft/client/",
            "com/mojang/blaze3d/",
            "net/neoforged/neoforge/client/",
            "net/minecraftforge/client/",
            "KeepButPenaltyConfigScreen"
    );

    private static final Pattern MIXIN_PACKAGE_PATTERN =
            Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MIXIN_ARRAY_PATTERN =
            Pattern.compile("\"(?:mixins|client|server)\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
    private static final Pattern JSON_STRING_PATTERN =
            Pattern.compile("\"([^\"]+)\"");
    private static final Pattern SOURCE_MIXIN_ANNOTATION =
            Pattern.compile("(?m)^\\s*@Mixin\\b");

    private static Path resolvePackagedJar() {
        String prop = System.getProperty("kbp.jar");
        assertNotNull(prop, "System property 'kbp.jar' must point to the built mod JAR");
        Path jarPath = Path.of(prop);
        assertTrue(Files.isRegularFile(jarPath), "Packaged mod JAR does not exist: " + jarPath);
        return jarPath;
    }

    @Test
    void dedicatedServerIsolationRedline() throws IOException {
        Path jarPath = resolvePackagedJar();
        List<String> violations = new ArrayList<>();
        int scannedServerClasses = 0;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || isClientPackage(name)) {
                    continue;
                }
                scannedServerClasses++;
                List<String> poolStrings = readConstantPoolUtf8s(jar.getInputStream(entry));
                for (String ref : poolStrings) {
                    for (String forbidden : FORBIDDEN_SERVER_REFS) {
                        if (ref.contains(forbidden)) {
                            violations.add(name + " -> " + ref);
                            break;
                        }
                    }
                }
            }
        }

        assertTrue(scannedServerClasses > 0, "Expected at least one non-client class in " + jarPath);
        assertTrue(violations.isEmpty(),
                "Dedicated Server isolation violated! Client references found in non-client classes:\n"
                        + String.join("\n", violations));
    }

    @Test
    void mixinBidirectionalClosure() throws IOException {
        Path jarPath = resolvePackagedJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry mixinsEntry = jar.getJarEntry("keep_but_penalty.mixins.json");
            assertNotNull(mixinsEntry, "keep_but_penalty.mixins.json missing from packaged JAR: " + jarPath);

            String json = new String(jar.getInputStream(mixinsEntry).readAllBytes(), StandardCharsets.UTF_8);
            Matcher pkgMatcher = MIXIN_PACKAGE_PATTERN.matcher(json);
            assertTrue(pkgMatcher.find(), "Missing 'package' in keep_but_penalty.mixins.json");
            String mixinPkgPath = pkgMatcher.group(1).replace('.', '/');

            Set<String> declaredClasses = new HashSet<>();
            Matcher arrayMatcher = MIXIN_ARRAY_PATTERN.matcher(json);
            while (arrayMatcher.find()) {
                Matcher strMatcher = JSON_STRING_PATTERN.matcher(arrayMatcher.group(1));
                while (strMatcher.find()) {
                    String rel = strMatcher.group(1).replace('.', '/');
                    declaredClasses.add(mixinPkgPath + "/" + rel + ".class");
                }
            }
            assertFalse(declaredClasses.isEmpty(), "keep_but_penalty.mixins.json declared zero mixins");

            // Forward check: every declared mixin exists in the final JAR
            for (String expectedClass : declaredClasses) {
                assertNotNull(jar.getJarEntry(expectedClass),
                        "Mixin declared in keep_but_penalty.mixins.json is missing from JAR: " + expectedClass);
            }

            // Reverse check (bytecode): every @Mixin class in the final JAR is declared
            Set<String> bytecodeMixins = new HashSet<>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                List<String> poolStrings = readConstantPoolUtf8s(jar.getInputStream(entry));
                if (poolStrings.contains("Lorg/spongepowered/asm/mixin/Mixin;")) {
                    bytecodeMixins.add(name);
                }
            }

            assertFalse(bytecodeMixins.isEmpty(), "Expected @Mixin-annotated classes in packaged JAR");
            for (String found : bytecodeMixins) {
                assertTrue(declaredClasses.contains(found),
                        "Compiled @Mixin class is NOT registered in keep_but_penalty.mixins.json: " + found);
            }

            // Reverse check (source): every @Mixin source file in common/main + common/<apiLayer> is declared
            String projectRootProp = System.getProperty("kbp.projectRoot");
            String apiLayerProp = System.getProperty("kbp.apiLayer");
            if (projectRootProp != null && apiLayerProp != null) {
                Path root = Path.of(projectRootProp);
                List<Path> srcRoots = List.of(
                        root.resolve("common/src/main/java"),
                        root.resolve("common/src/" + apiLayerProp + "/java")
                );
                for (Path srcRoot : srcRoots) {
                    if (!Files.isDirectory(srcRoot)) continue;
                    try (Stream<Path> walk = Files.walk(srcRoot)) {
                        List<Path> javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
                        for (Path file : javaFiles) {
                            String source = Files.readString(file, StandardCharsets.UTF_8);
                            if (SOURCE_MIXIN_ANNOTATION.matcher(source).find()) {
                                String relClass = srcRoot.relativize(file).toString()
                                        .replace('\\', '/')
                                        .replaceAll("\\.java$", ".class");
                                assertTrue(declaredClasses.contains(relClass),
                                        "Source @Mixin class is NOT registered in keep_but_penalty.mixins.json: " + relClass);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void cleanPackageRedline() throws IOException {
        Path jarPath = resolvePackagedJar();
        List<String> violations = new ArrayList<>();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("rinlib")) {
                    violations.add("Forbidden rinlib entry: " + name);
                }
                if (name.startsWith("com/terraformersmc/modmenu/")) {
                    violations.add("Leaked ModMenu compile-only stub entry: " + name);
                }
                if (name.endsWith(".class")) {
                    for (String utf8 : readConstantPoolUtf8s(jar.getInputStream(entry))) {
                        if (utf8.toLowerCase(Locale.ROOT).contains("rinlib")) {
                            violations.add("Forbidden rinlib constant-pool ref in " + name + ": " + utf8);
                        }
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Clean package redline violated in " + jarPath + ":\n" + String.join("\n", violations));
    }

    private static boolean isClientPackage(String entryName) {
        return entryName.contains("/client/") || entryName.startsWith("client/");
    }

    /**
     * Zero-dependency JVM ClassFile constant pool UTF-8 extractor (JVMS §4.4).
     */
    private static List<String> readConstantPoolUtf8s(InputStream rawIn) throws IOException {
        List<String> utf8s = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(rawIn)) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                return utf8s;
            }
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major
            int cpCount = in.readUnsignedShort();
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8s.add(in.readUTF()); // CONSTANT_Utf8
                    case 3, 4 -> in.skipBytes(4); // Integer, Float
                    case 5, 6 -> { // Long, Double (occupy two slots)
                        in.skipBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> in.skipBytes(2); // Class, String, MethodType, Module, Package
                    case 9, 10, 11, 12, 17, 18 -> in.skipBytes(4); // Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
                    case 15 -> in.skipBytes(3); // MethodHandle
                    default -> throw new IOException("Unsupported constant pool tag: " + tag);
                }
            }
        }
        return utf8s;
    }
}
