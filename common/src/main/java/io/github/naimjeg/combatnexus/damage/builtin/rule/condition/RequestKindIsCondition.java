package io.github.naimjeg.combatnexus.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record RequestKindIsCondition(
        DamageRequestKind kind
) implements DamageRuleCondition {
    public static final MapCodec<RequestKindIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRequestKind.CODEC.fieldOf("kind")
                            .forGetter(RequestKindIsCondition::kind)
            ).apply(instance, RequestKindIsCondition::new));

    public RequestKindIsCondition {
        Objects.requireNonNull(kind, "kind");
    }

    @Override public Identifier type() {
        return DamageModuleConditionIds.REQUEST_KIND_IS;
    }

    @Override public boolean test(DamageRuleContext ctx) {
        return ctx.requestKind() == kind;
    }
}
