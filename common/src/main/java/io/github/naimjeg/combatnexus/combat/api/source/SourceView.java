package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.UUID;
import net.minecraft.resources.Identifier;
/** Description only: querying identity never grants revocation authority. */
public record SourceView(UUID target, SourceInstance source, Identifier definition, boolean persistent, boolean pending) {}
