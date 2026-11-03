package io.github.naimjeg.combatnexus.damage.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingEntityDamageSettlementMixinContractTest {

    @Test
    void requiredMixinIsRegisteredForBothSidesWithExactHurtDescriptor()
            throws IOException {
        String mixinJson = Files.readString(Path.of(
                "src/main/resources/combatnexus.mixins.json"
        ));
        String modMetadata = Files.readString(Path.of(
                "src/main/resources/META-INF/neoforge.mods.toml"
        ));
        String source = Files.readString(Path.of(
                "src/main/java/io/github/naimjeg/combatnexus/damage/mixin/"
                        + "LivingEntityDamageSettlementMixin.java"
        ));

        assertTrue(mixinJson.contains("\"required\": true"));
        assertTrue(mixinJson.contains(
                "\"LivingEntityDamageSettlementMixin\""
        ));
        assertTrue(mixinJson.contains("\"defaultRequire\": 1"));
        assertTrue(modMetadata.contains("[[mixins]]"));
        assertTrue(modMetadata.contains("config=\"${mod_id}.mixins.json\""));
        assertTrue(modMetadata.contains("side=\"BOTH\""));
        assertTrue(source.contains("@WrapMethod(method = \"hurtServer("));
        assertTrue(source.contains(
                "ServerLevel;Lnet/minecraft/world/damagesource/"
        ));
        assertTrue(source.contains("DamageSource;F)Z\""));
    }
}
