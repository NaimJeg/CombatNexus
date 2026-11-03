package io.github.naimjeg.combatnexus.combat.projectile.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;

/** Captured once; later owner/equipment changes cannot replace the launching participant. */
public record ProjectileLaunch(UUID action, UUID projectile, CombatInputFrame shooter,net.minecraft.world.item.ItemStack weapon) {
    public ProjectileLaunch { Objects.requireNonNull(action); Objects.requireNonNull(projectile); Objects.requireNonNull(shooter);weapon=Objects.requireNonNull(weapon).copy(); }
    @Override public net.minecraft.world.item.ItemStack weapon(){return weapon.copy();}
}
