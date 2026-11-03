package io.github.naimjeg.combatnexus.combat.damage.api.rule;

import net.minecraft.resources.Identifier;

import java.util.List;

public interface ChannelReferencingCondition {

    List<Identifier> referencedChannels();
}
