package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.presentation.api.DisplayText;
/** Display-only explanation of a captured source, never installation authority. */
public record SourceDescription(Identifier source, String slot, DisplayText description) {
    public SourceDescription { Objects.requireNonNull(source);Objects.requireNonNull(slot);Objects.requireNonNull(description); if(slot.length()>256)throw new IllegalArgumentException("Source slot too long"); }
}
