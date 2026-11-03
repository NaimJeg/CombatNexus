package io.github.naimjeg.combatnexus.combat.api.behavior;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import net.minecraft.resources.Identifier;
/** Named parameter codec used by definitions and stored state. */
public record BehaviorType<T extends BehaviorComponent>(Identifier id, Class<T> implementation, MapCodec<T> codec) {
    public BehaviorType {
        Objects.requireNonNull(id); Objects.requireNonNull(implementation); Objects.requireNonNull(codec);
    }
}
