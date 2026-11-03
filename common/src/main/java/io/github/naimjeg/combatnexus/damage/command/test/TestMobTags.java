package io.github.naimjeg.combatnexus.damage.command.test;

import net.minecraft.world.entity.Entity;

/** Stable vanilla entity tags used by the DamageModule test facility. */
public final class TestMobTags {

    public static final String TEST_ENTITY = "combatnexus_test_entity";
    /** Ordinary-gameplay-damage death protection, not removal immunity. */
    public static final String IMMORTAL = "combatnexus_test_immortal";
    /** Pending restore for one cancelled ordinary lethal damage transaction. */
    public static final String PENDING_IMMORTAL_RESTORE =
            "combatnexus_test_immortal_pending_restore";

    private TestMobTags() {
    }

    public static boolean isTestEntity(Entity entity) {
        return entity != null && entity.entityTags().contains(TEST_ENTITY);
    }

    public static boolean isImmortal(Entity entity) {
        return entity != null && entity.entityTags().contains(IMMORTAL);
    }
}
