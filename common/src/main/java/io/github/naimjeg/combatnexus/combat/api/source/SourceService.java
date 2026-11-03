package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CommandResult;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
public interface SourceService {
    SourceResult install(SourceInstallation request);
    SourceResult install(SourceInstallation request, CombatParent parent);
    CommandResult<Boolean> revoke(UUID request, InstallationHandle handle);
    CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent);
    List<SourceView> snapshot(UUID target);
    CommandResult<Boolean> refresh(UUID request, UUID target);
}
