package io.github.naimjeg.combatnexus.damage.api.rule.builder;

import io.github.naimjeg.combatnexus.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.damage.api.rule.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DamageRuleBuilder {

    private final Identifier id;
    private final List<DamageRuleCondition> conditions = new ArrayList<>();
    private final List<DamageRuleOperation> operations = new ArrayList<>();
    private DamageRuleRole role;
    private DamagePhase phase = DamagePhase.BASE_MODIFICATION;
    private int priority = 500;
    private DamageRuleStacking stacking = DamageRuleStacking.STACK;
    private Optional<Identifier> stackingGroup = Optional.empty();
    private Optional<String> traceLabel = Optional.empty();

    private DamageRuleBuilder(
            Identifier id,
            DamageRuleRole role
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Damage rule id must not be null"
        );

        this.role = Objects.requireNonNull(
                role,
                "Damage rule role must not be null"
        );
    }

    public static DamageRuleBuilder offensive(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.OFFENSIVE);
    }

    public static DamageRuleBuilder defensive(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.DEFENSIVE);
    }

    public static DamageRuleBuilder any(Identifier id) {
        return new DamageRuleBuilder(id, DamageRuleRole.ANY);
    }

    public DamageRuleBuilder role(DamageRuleRole role) {
        this.role = Objects.requireNonNull(
                role,
                "Damage rule role must not be null"
        );
        return this;
    }

    public DamageRuleBuilder phase(DamagePhase phase) {
        this.phase = Objects.requireNonNull(
                phase,
                "Damage rule phase must not be null"
        );
        return this;
    }

    public DamageRuleBuilder baseModification() {
        return phase(DamagePhase.BASE_MODIFICATION);
    }

    public DamageRuleBuilder typeScaling() {
        return phase(DamagePhase.TYPE_SCALING);
    }

    public DamageRuleBuilder criticalHit() {
        return phase(DamagePhase.CRITICAL_HIT);
    }

    public DamageRuleBuilder conditionalMultiplier() {
        return phase(DamagePhase.CONDITIONAL_MULTI);
    }

    public DamageRuleBuilder globalAdjustment() {
        return phase(DamagePhase.GLOBAL_ADJUSTMENT);
    }

    public DamageRuleBuilder mitigationSetup() {
        return phase(DamagePhase.MITIGATION_SETUP);
    }

    public DamageRuleBuilder finalOverride() {
        return phase(DamagePhase.FINAL_OVERRIDE);
    }


    public DamageRuleBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public DamageRuleBuilder when(DamageRuleCondition condition) {
        this.conditions.add(Objects.requireNonNull(
                condition,
                "Damage rule condition must not be null"
        ));
        return this;
    }

    public DamageRuleBuilder always() {
        return when(DamageModuleConditions.always());
    }

    public DamageRuleBuilder critical() {
        return when(DamageModuleConditions.critical());
    }

    public DamageRuleBuilder targetOnFire() {
        return when(DamageModuleConditions.targetOnFire());
    }

    /*
     * ---------------------------------------------------------------------
     * Condition shortcuts
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder whenAllOf(
            List<DamageRuleCondition> conditions
    ) {
        return when(DamageModuleConditions.allOf(conditions));
    }

    public DamageRuleBuilder whenAllOf(
            DamageRuleCondition... conditions
    ) {
        return when(DamageModuleConditions.allOf(conditions));
    }

    public DamageRuleBuilder whenAnyOf(
            List<DamageRuleCondition> conditions
    ) {
        return when(DamageModuleConditions.anyOf(conditions));
    }

    public DamageRuleBuilder whenAnyOf(
            DamageRuleCondition... conditions
    ) {
        return when(DamageModuleConditions.anyOf(conditions));
    }

    public DamageRuleBuilder whenNot(
            DamageRuleCondition condition
    ) {
        return when(DamageModuleConditions.not(condition));
    }

    public DamageRuleBuilder damageChannelIs(
            Identifier channel
    ) {
        return when(DamageModuleConditions.damageChannelIs(channel));
    }

    /*
     * ---------------------------------------------------------------------
     * Health conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerHealthBelow(float ratio) {
        return when(DamageModuleConditions.attackerHealthBelow(ratio));
    }

    public DamageRuleBuilder attackerHealthAbove(float ratio) {
        return when(DamageModuleConditions.attackerHealthAbove(ratio));
    }

    public DamageRuleBuilder targetHealthBelow(float ratio) {
        return when(DamageModuleConditions.targetHealthBelow(ratio));
    }

    public DamageRuleBuilder targetHealthAbove(float ratio) {
        return when(DamageModuleConditions.targetHealthAbove(ratio));
    }

    /*
     * ---------------------------------------------------------------------
     * Mob-effect conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerHasEffect(
            Identifier effect
    ) {
        return when(DamageModuleConditions.attackerHasEffect(effect));
    }

    public DamageRuleBuilder targetHasEffect(
            Identifier effect
    ) {
        return when(DamageModuleConditions.targetHasEffect(effect));
    }

    public DamageRuleBuilder attackerEffectTag(TagKey<MobEffect> tag) {
        return when(DamageModuleConditions.attackerEffectTag(tag));
    }

    public DamageRuleBuilder targetEffectTag(TagKey<MobEffect> tag) {
        return when(DamageModuleConditions.targetEffectTag(tag));
    }

    public DamageRuleBuilder sourceActionIs(Identifier action) {
        return when(DamageModuleConditions.sourceActionIs(action));
    }

    public DamageRuleBuilder sourceTag(Identifier tag) {
        return when(DamageModuleConditions.sourceTag(tag));
    }

    public DamageRuleBuilder requestKindIs(DamageRequestKind kind) {
        return when(DamageModuleConditions.requestKindIs(kind));
    }

    public DamageRuleBuilder isPrimaryDamage() {
        return when(DamageModuleConditions.isPrimaryDamage());
    }

    public DamageRuleBuilder isProcDamage() {
        return when(DamageModuleConditions.isProcDamage());
    }

    public DamageRuleBuilder hasParentDamage() {
        return when(DamageModuleConditions.hasParentDamage());
    }

    public DamageRuleBuilder procAllowed() {
        return when(DamageModuleConditions.procAllowed());
    }

    /*
     * ---------------------------------------------------------------------
     * Damage source / damage type conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder damageTypeIs(
            Identifier damageType
    ) {
        return when(DamageModuleConditions.damageTypeIs(damageType));
    }

    public DamageRuleBuilder damageTypeTag(
            TagKey<DamageType> tag
    ) {
        return when(DamageModuleConditions.damageTypeTag(tag));
    }

    /*
     * ---------------------------------------------------------------------
     * Target entity conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder targetEntityTypeIs(
            Identifier entityType
    ) {
        return when(DamageModuleConditions.targetEntityTypeIs(entityType));
    }

    public DamageRuleBuilder targetEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return when(DamageModuleConditions.targetEntityTypeTag(tag));
    }

    public DamageRuleBuilder targetMobCategoryIs(
            MobCategory category
    ) {
        return when(DamageModuleConditions.targetMobCategoryIs(category));
    }

    public DamageRuleBuilder targetIsBoss() {
        return when(DamageModuleConditions.targetIsBoss());
    }

    /*
     * ---------------------------------------------------------------------
     * Attacker entity conditions
     * ---------------------------------------------------------------------
     */

    public DamageRuleBuilder attackerEntityTypeIs(
            Identifier entityType
    ) {
        return when(DamageModuleConditions.attackerEntityTypeIs(entityType));
    }

    public DamageRuleBuilder attackerEntityTypeTag(
            TagKey<EntityType<?>> tag
    ) {
        return when(DamageModuleConditions.attackerEntityTypeTag(tag));
    }

    public DamageRuleBuilder attackerMobCategoryIs(
            MobCategory category
    ) {
        return when(DamageModuleConditions.attackerMobCategoryIs(category));
    }

    public DamageRuleBuilder attackerIsBoss() {
        return when(DamageModuleConditions.attackerIsBoss());
    }

    public DamageRuleBuilder operation(DamageRuleOperation operation) {
        this.operations.add(Objects.requireNonNull(
                operation,
                "Damage rule operation must not be null"
        ));
        return this;
    }

    public DamageRuleBuilder addTrueDamage(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addTrueDamage(channel, value));
    }

    public DamageRuleBuilder addBaseDamage(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addBaseDamage(channel, value));
    }

    public DamageRuleBuilder addBaseDamage(
            Identifier channel,
            DamageApplicationBucket applicationBucket,
            float value
    ) {
        return operation(DamageModuleOperations.addBaseDamage(
                channel,
                applicationBucket,
                value
        ));
    }

    public DamageRuleBuilder addChannelPreMultiplier(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addChannelPreMultiplier(channel, value));
    }

    public DamageRuleBuilder addChannelPreMultiplier(
            Identifier channel,
            Identifier preMultiplierBucket,
            float value
    ) {
        return operation(DamageModuleOperations.addChannelPreMultiplier(
                channel,
                preMultiplierBucket,
                value
        ));
    }

    public DamageRuleBuilder addChannelPostMultiplier(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addChannelPostMultiplier(channel, value));
    }

    public DamageRuleBuilder addGlobalPreMultiplier(float value) {
        return operation(DamageModuleOperations.addGlobalPreMultiplier(value));
    }

    public DamageRuleBuilder addGlobalPreMultiplier(
            Identifier preMultiplierBucket,
            float value
    ) {
        return operation(DamageModuleOperations.addGlobalPreMultiplier(
                preMultiplierBucket,
                value
        ));
    }

    public DamageRuleBuilder addGlobalPostMultiplier(float value) {
        return operation(DamageModuleOperations.addGlobalPostMultiplier(value));
    }

    public DamageRuleBuilder convertDamage(
            Identifier from,
            Identifier to,
            float ratio
    ) {
        return operation(DamageModuleOperations.convertDamage(from, to, ratio));
    }

    public DamageRuleBuilder gainExtraDamage(
            Identifier basedOn,
            Identifier to,
            float ratio
    ) {
        return operation(DamageModuleOperations.gainExtraDamage(basedOn, to, ratio));
    }

    public DamageRuleBuilder addTemporaryResistance(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addTemporaryResistance(channel, value));
    }

    public DamageRuleBuilder addChannelMitigation(
            Identifier channel,
            float value
    ) {
        return operation(DamageModuleOperations.addChannelMitigation(channel, value));
    }

    public DamageRuleBuilder addGlobalMitigation(float value) {
        return operation(DamageModuleOperations.addGlobalMitigation(value));
    }

    public DamageRuleBuilder multiplyArmorEffectiveness(float value) {
        return operation(DamageModuleOperations.multiplyArmorEffectiveness(value));
    }

    public DamageRuleBuilder overrideFinalDamage(float value) {
        return operation(DamageModuleOperations.overrideFinalDamage(value));
    }

    public DamageRuleBuilder cancelDamage() {
        return operation(DamageModuleOperations.cancelDamage());
    }

    public DamageRuleBuilder cancelDamage(String sourceId) {
        return operation(DamageModuleOperations.cancelDamage(sourceId));
    }

    public DamageRuleBuilder stacking(DamageRuleStacking stacking) {
        this.stacking = Objects.requireNonNull(
                stacking,
                "Damage rule stacking policy must not be null"
        );
        return this;
    }

    public DamageRuleBuilder stack() {
        return stacking(DamageRuleStacking.STACK);
    }

    public DamageRuleBuilder uniqueSource() {
        return stacking(DamageRuleStacking.UNIQUE_SOURCE);
    }

    public DamageRuleBuilder highestValue() {
        return stacking(DamageRuleStacking.HIGHEST_VALUE);
    }

    public DamageRuleBuilder lowestValue() {
        return stacking(DamageRuleStacking.LOWEST_VALUE);
    }

    public DamageRuleBuilder replace() {
        return stacking(DamageRuleStacking.REPLACE);
    }

    public DamageRuleBuilder stackingGroup(Identifier group) {
        this.stackingGroup = Optional.ofNullable(group);
        return this;
    }

    public DamageRuleBuilder trace(String traceLabel) {
        this.traceLabel = Optional.ofNullable(traceLabel);
        return this;
    }

    public DamageRuleDefinition build() {
        validateBuilderState();

        DamageRuleDefinition rule = new DamageRuleDefinition(
                id,
                role,
                phase,
                priority,
                List.copyOf(conditions),
                List.copyOf(operations),
                stacking,
                safeStackingGroup(),
                safeTraceLabel()
        );

        DamageRuleValidator.requireValid(
                rule,
                "java_api/builder"
        );

        return rule;
    }

    private void validateBuilderState() {
        if (id == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null id"
            );
        }

        if (role == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null role: " + id
            );
        }

        if (phase == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null phase: " + id
            );
        }

        if (stacking == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null stacking policy: " + id
            );
        }

        if (stackingGroup == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null stacking group optional: " + id
            );
        }

        if (traceLabel == null) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule with null trace label optional: " + id
            );
        }

        if (operations.isEmpty()) {
            throw new IllegalStateException(
                    "DamageRuleBuilder cannot build rule without operations: " + id
            );
        }

        for (DamageRuleCondition condition : conditions) {
            if (condition == null) {
                throw new IllegalStateException(
                        "DamageRuleBuilder cannot build rule with null condition: " + id
                );
            }
        }

        for (DamageRuleOperation operation : operations) {
            if (operation == null) {
                throw new IllegalStateException(
                        "DamageRuleBuilder cannot build rule with null operation: " + id
                );
            }
        }
    }

    private Optional<Identifier> safeStackingGroup() {
        return stackingGroup != null ? stackingGroup : Optional.empty();
    }

    private Optional<String> safeTraceLabel() {
        return traceLabel != null ? traceLabel : Optional.empty();
    }
}
