package io.github.naimjeg.combatnexus.combat.damage.api.context;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttribution;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttributionProvenance;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageLineage;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageMetadata;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface DamageContextView {
    NativeDamageInput nativeInput();
    /** A detached list of currently active channels, without mutable packet components. */
    java.util.List<DamageChannel> activeChannels();
    default Optional<io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileLaunch> projectileLaunch() {
        return io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileFacts.launch(directEntity());
    }

    /** Immutable calculation input, shared with read-only consumers. Empty for nonparticipants.
     * Active world sessions issue the revision; each request captures fresh values, even within one tick.
     */
    default Optional<io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot> attributeSnapshot(
            java.util.UUID entity) {
        return Optional.empty();
    }

    io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel resolveChannel(net.minecraft.resources.Identifier id);
    io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame input(java.util.UUID entity);
    io.github.naimjeg.combatnexus.combat.api.input.CombatEntityFacts facts(java.util.UUID entity);

    boolean isManaged();

    DamageOrigin origin();

    DamageAttribution attribution();

    @Nullable LivingEntity logicalAttacker();

    @Nullable Entity directEntity();

    @Nullable Entity effectOwner();

    @Nullable LivingEntity equipmentOwner();

    DamageRequestKind requestKind();

    DamageLineage lineage();

    Optional<Identifier> actionId();

    Set<Identifier> sourceTags();

    DamageMetadata metadata();

    DamageAttributionProvenance attributionProvenance();

    LivingEntity victim();

    DamageSource source();

    long damageId();

    DamageChannel getInitialChannel();

    /**
     * Returns whether the current damage packet contains positive damage in the
     * given channel at this point of the pipeline.
     */
    boolean hasActiveDamageInChannel(DamageChannel channel);

    DamagePhase currentPhase();

    /** Final frozen critical result once the CRITICAL_HIT sub-lifecycle begins. */
    boolean isCritical();

    /** Immutable view of this transaction's critical decision lifecycle. */
    CriticalDecisionSnapshot criticalDecision();

    boolean isDamageCancelled();
}
