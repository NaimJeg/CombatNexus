package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record SourceActionIsCondition(
        Identifier action
) implements DamageRuleCondition {
    public static final MapCodec<SourceActionIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("action")
                            .forGetter(SourceActionIsCondition::action)
            ).apply(instance, SourceActionIsCondition::new));

    public SourceActionIsCondition {
        Objects.requireNonNull(action, "action");
    }

    @Override public Identifier type() {
        return DamageModuleConditionIds.SOURCE_ACTION_IS;
    }

    @Override public boolean test(DamageContextView ctx) {
        return ctx.actionId().filter(action::equals).isPresent();
    }
}
