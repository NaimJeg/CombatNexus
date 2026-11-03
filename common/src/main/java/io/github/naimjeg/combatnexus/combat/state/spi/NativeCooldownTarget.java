package io.github.naimjeg.combatnexus.combat.state.spi;
import java.util.Map;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.state.api.CooldownStamp;
public interface NativeCooldownTarget { Map<Identifier, CooldownStamp> combatnexus$cooldowns(); }
