package io.github.naimjeg.combatnexus.damage.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExternalApiFixtureImportTest {
    @Test
    void fixtureImportsNoDamageModuleCoreInternalOrRegistryTypes() throws Exception {
        Path root = Path.of(
                "src/test/java/io/github/naimjeg/combatnexus/damage/externalapi");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString()
                    .endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains(
                        "io.github.naimjeg.combatnexus.damage.core"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.combatnexus.damage.internal"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.combatnexus.damage.registry"), file.toString());
                assertFalse(source.contains(
                        "io.github.naimjeg.combatnexus.damage.diagnostics"), file.toString());
            }
        }
    }
}
