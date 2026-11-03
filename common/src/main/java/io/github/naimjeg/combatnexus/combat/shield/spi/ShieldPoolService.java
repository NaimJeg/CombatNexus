package io.github.naimjeg.combatnexus.combat.shield.spi;
import io.github.naimjeg.combatnexus.combat.shield.api.*;

import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Server-owned pool lifecycle. All calls belong to one world session and its server thread. */
public interface ShieldPoolService extends ShieldService {
    ShieldGrantReceipt replace(UUID target, ShieldGrant pool);
    ShieldGrantReceipt replace(UUID target, ShieldGrant pool, io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent parent);
    Reservation reserve(UUID target, long damageId, Identifier damageType, double amount,
                        io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent parent);
    void remove(UUID target, Identifier source, ShieldPoolSnapshot.RemovalReason reason);

    record Change(UUID target, ShieldPoolSnapshot pool, ShieldPoolSnapshot.RemovalReason reason) {}
}
