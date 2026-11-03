package io.github.naimjeg.combatnexus.combat.api.scheduling;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
/** Startup registration for immutable, serializable intent parameters. */
public record IntentType<T extends ScheduledIntent>(Identifier id, Class<T> implementation, MapCodec<T> codec,
        Set<CombatOperation> permissions, BiConsumer<T, IntentExecution> executor) {
    public IntentType { Objects.requireNonNull(id); Objects.requireNonNull(implementation); Objects.requireNonNull(codec); permissions=Set.copyOf(permissions); Objects.requireNonNull(executor); }
}
