package io.github.naimjeg.combatnexus.combat.api.behavior;
import net.minecraft.resources.Identifier;
/** Revocation-only capability; it cannot grant resources or submit combat requests. */
public interface BehaviorCleanup { void removeShield(Identifier pool); }
