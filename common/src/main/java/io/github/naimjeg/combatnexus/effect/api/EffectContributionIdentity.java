package io.github.naimjeg.combatnexus.effect.api;

import net.minecraft.resources.Identifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Source ownership decides the stable identity; attribute only installs the supplied contribution. */
public final class EffectContributionIdentity {
    private EffectContributionIdentity() {}
    public static Identifier id(EffectInstance effect, EffectInstance.AttributeContribution contribution) {
        String identity = effect.snapshot().effect() + "|" + effect.snapshot().source()
                + "|" + contribution.attribute() + "|" + contribution.modifier().source();
        return Identifier.fromNamespaceAndPath("combatnexus", "effect/"
                + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)));
    }
}
