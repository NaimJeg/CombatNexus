package io.github.naimjeg.combatnexus.compat.apotheosis;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.input.FactKey;
import io.github.naimjeg.combatnexus.combat.api.source.SourceDescription;
import io.github.naimjeg.combatnexus.presentation.api.DisplayText;

/** Native instances are translated during capture; no live stacks enter formula callbacks. */
public final class EquipmentFacts {
    private EquipmentFacts() {}
    public record Entry(String kind, String equipmentSlot, Identifier item, Identifier source, float level, String quality, int socket) {
        public static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("kind").forGetter(Entry::kind),Codec.STRING.fieldOf("slot").forGetter(Entry::equipmentSlot),
                Identifier.CODEC.fieldOf("item").forGetter(Entry::item),Identifier.CODEC.fieldOf("source").forGetter(Entry::source),
                Codec.FLOAT.fieldOf("level").forGetter(Entry::level),Codec.STRING.fieldOf("quality").forGetter(Entry::quality),Codec.INT.fieldOf("socket").forGetter(Entry::socket)
        ).apply(i,Entry::new));
    }
    public static final FactKey<List<Entry>> SOURCES=new FactKey<>(Identifier.parse("apotheosis:equipment_sources"),Entry.CODEC.listOf());
    public static void register(CombatRegistrar registrar) {
        registrar.facts(SOURCES,entity->{
            var result=new ArrayList<Entry>();
            for(var slot:EquipmentSlot.values()) {
                var stack=entity.getItemBySlot(slot); if(stack.isEmpty())continue;
                var item=BuiltInRegistries.ITEM.getKey(stack.getItem());
                for(var affix:AffixHelper.getAffixes(stack).values()) if(affix.isValid())
                    result.add(new Entry("affix",slot.getName(),item,affix.affix().getId(),affix.level(),affix.rarity().getId().toString(),-1));
                for(var gem:SocketHelper.getGems(stack).gems()) if(gem.isValid())
                    result.add(new Entry("gem",slot.getName(),item,gem.gem().getId(),0,gem.purity().name(),gem.slot()));
            }
            return List.copyOf(result);
        });
        registrar.sourceDescriptions(Identifier.parse("apotheosis:equipment_sources"),input->input.facts().external().find(SOURCES).orElse(List.of()).stream()
                .map(entry->new SourceDescription(entry.source(),entry.equipmentSlot(),DisplayText.translatable("source.combatnexus_compat."+entry.kind(),
                        entry.source().toString(),entry.equipmentSlot(),entry.quality(),entry.kind().equals("gem")?Integer.toString(entry.socket()):Float.toString(entry.level())))).toList());
    }
}
