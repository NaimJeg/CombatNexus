package io.github.naimjeg.combatnexus.combat.damage.api.display.phrase;
import io.github.naimjeg.combatnexus.presentation.api.phrase.*;

import net.minecraft.world.entity.MobCategory;
import java.util.Objects;

public record MobCategoryValue(MobCategory category) implements PhraseValue {
    public MobCategoryValue {
        Objects.requireNonNull(category, "category");
    }
}
