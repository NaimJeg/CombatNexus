package io.github.naimjeg.combatnexus.compat.apotheosis;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import net.minecraft.resources.Identifier;
/** Locked GemBonus identity formula, including namespace, equipment category and socket. */
public final class GemSources {
    private GemSources() {}
    public static Identifier identity(GemInstance instance) {
        return Identifier.fromNamespaceAndPath(instance.gem().getId().getNamespace(),instance.gem().getId().getPath()+"_modifier_"
                +instance.category().getSlots().id().toShortLanguageKey()+"_"+instance.slot());
    }
}
