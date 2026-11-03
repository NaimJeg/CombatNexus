package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

/** Read-only framework view; entity references remain live platform objects. */
final class ReadOnlyDamageContext implements DamageContextView {

    @Override public io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput() { return delegate.nativeInput(); }
    @Override public java.util.List<DamageChannel> activeChannels() { return delegate.activeChannels(); }
    private final DamageContextView delegate;

    ReadOnlyDamageContext(DamageContextView delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Optional<io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot> attributeSnapshot(
            java.util.UUID entity) {
        return delegate.attributeSnapshot(entity);
    }

    @Override
    public io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel resolveChannel(net.minecraft.resources.Identifier id) { return delegate.resolveChannel(id); }
    @Override
    public io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame input(java.util.UUID entity) { return delegate.input(entity); }
    @Override
    public io.github.naimjeg.combatnexus.combat.api.input.CombatEntityFacts facts(java.util.UUID entity) {
        return delegate.facts(entity);
    }

    @Override
    public boolean isManaged() {
        return delegate.isManaged();
    }

    @Override
    public DamageOrigin origin() {
        return delegate.origin();
    }

    @Override
    public DamageAttribution attribution() {
        return delegate.attribution();
    }

    @Override
    public @Nullable LivingEntity logicalAttacker() {
        return delegate.logicalAttacker();
    }

    @Override
    public @Nullable Entity directEntity() {
        return delegate.directEntity();
    }

    @Override
    public @Nullable Entity effectOwner() {
        return delegate.effectOwner();
    }

    @Override
    public @Nullable LivingEntity equipmentOwner() {
        return delegate.equipmentOwner();
    }

    @Override
    public DamageRequestKind requestKind() {
        return delegate.requestKind();
    }

    @Override
    public DamageLineage lineage() {
        return delegate.lineage();
    }

    @Override
    public Optional<Identifier> actionId() {
        return delegate.actionId();
    }

    @Override
    public Set<Identifier> sourceTags() {
        return delegate.sourceTags();
    }

    @Override
    public DamageMetadata metadata() {
        return delegate.metadata();
    }

    @Override
    public DamageAttributionProvenance attributionProvenance() {
        return delegate.attributionProvenance();
    }

    @Override
    public LivingEntity victim() {
        return delegate.victim();
    }

    @Override
    public DamageSource source() {
        return delegate.source();
    }

    @Override
    public long damageId() {
        return delegate.damageId();
    }

    @Override
    public DamageChannel getInitialChannel() {
        return delegate.getInitialChannel();
    }

    @Override
    public boolean hasActiveDamageInChannel(DamageChannel channel) {
        return delegate.hasActiveDamageInChannel(channel);
    }

    @Override
    public DamagePhase currentPhase() {
        return delegate.currentPhase();
    }

    @Override
    public boolean isCritical() {
        return delegate.isCritical();
    }

    @Override
    public CriticalDecisionSnapshot criticalDecision() {
        return delegate.criticalDecision();
    }

    @Override
    public boolean isDamageCancelled() {
        return delegate.isDamageCancelled();
    }

}
