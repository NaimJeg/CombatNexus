package io.github.naimjeg.combatnexus.combat.shield.api;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
/** Business grants and queries. Reservation and application commit belong exclusively to the application SPI. */
public interface ShieldCommands {
    List<ShieldPoolSnapshot> snapshot(UUID target);
    CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant);
    CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant, CombatParent parent);
    CommandResult<Boolean> remove(UUID request, UUID target, Identifier source);
}
