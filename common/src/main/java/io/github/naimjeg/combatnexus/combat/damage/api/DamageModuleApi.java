package io.github.naimjeg.combatnexus.combat.damage.api;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequest;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageResult;
import io.github.naimjeg.combatnexus.combat.damage.api.item.DamageModuleItemApi;
import io.github.naimjeg.combatnexus.combat.damage.api.item.DamageModuleItemEntries;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestService;
import net.minecraft.world.item.ItemStack;

public final class DamageModuleApi {

    /** Owning server thread only; requires an adopted definition handle for the explicit server. */
    public static long definitionGeneration(net.minecraft.server.MinecraftServer server) {
        var snapshot = io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server);
        return snapshot == null ? 0 : snapshot.generation();
    }

    public static io.github.naimjeg.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot itemPresentation(
            net.minecraft.server.MinecraftServer server, net.minecraft.world.item.ItemStack stack) {
        var snapshot = java.util.Objects.requireNonNull(
                io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(server), "Server definitions required");
        var rules = io.github.naimjeg.combatnexus.combat.damage.core.security.DamageModuleItemSecurity
                .validateForExecution(stack, "presentation", snapshot);
        return new io.github.naimjeg.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot(
                snapshot.generation(), rules.entries(), rules.affixes());
    }

    private DamageModuleApi() {
    }

    public static DamageModuleItemEntries getItemEntries(ItemStack stack) {
        return DamageModuleItemApi.get(stack);
    }

    public static boolean setItemEntries(
            ItemStack stack,
            DamageModuleItemEntries entries
    ) {
        return DamageModuleItemApi.set(stack, entries);
    }

    public static boolean clearItemEntries(ItemStack stack) {
        return DamageModuleItemApi.clear(stack);
    }

    /**
     * Submits one immutable request through the authoritative server damage
     * entry and the complete DamageModule pipeline.
     *
     * <p>This method is synchronous and must be called on the owning Minecraft
     * server thread. Normal rejection and no-damage outcomes are returned as a
     * structured result. Unexpected engine or processor exceptions are not
     * swallowed. For a managed settlement, the submission scope is removed
     * before the observational {@code DamageSettledEvent} is posted. Derived
     * requests are authorized only in registered DamageModule settlement
     * callbacks, which run after that observation post. A callback submission
     * returns its result immediately; its own settlement delivery remains in
     * the coordinator FIFO until the current delivery exits. The outer FIFO is
     * drained synchronously before the root call returns.</p>
     */
    public static DamageResult submitDamage(DamageRequest request) {
        return DamageRequestService.submit(request);
    }
}
