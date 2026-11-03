package probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent;
import io.github.naimjeg.combatnexus.effect.api.EffectDisplay;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

/** AutoJS startup extension. Not a production source set or published dependency. */
@Mod("combatnexus_effect_probe")
public final class EffectExtension {
    private static final java.util.List<io.github.naimjeg.combatnexus.combat.api.action.CombatMoment> MOMENTS = new java.util.ArrayList<>();
    public static java.util.List<io.github.naimjeg.combatnexus.combat.api.action.CombatMoment> moments() { return java.util.List.copyOf(MOMENTS); }
    public static void clearMoments() { MOMENTS.clear(); }
    private static final java.util.Map<io.github.naimjeg.combatnexus.effect.api.EffectCapability.Trigger, Integer> TRIGGERS = new java.util.EnumMap<>(io.github.naimjeg.combatnexus.effect.api.EffectCapability.Trigger.class);
    public static void resetTriggers() { TRIGGERS.clear(); }
    public static int triggers(String name) { return TRIGGERS.getOrDefault(io.github.naimjeg.combatnexus.effect.api.EffectCapability.Trigger.valueOf(name), 0); }
    public static net.minecraft.server.network.ServerGamePacketListenerImpl localConnection(net.minecraft.server.level.ServerPlayer player) {
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        return new net.minecraft.server.network.ServerGamePacketListenerImpl(player.level().getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> type) { return false; }
        };
    }
    public static boolean rejectedDescriptorMismatch;
    private static io.github.naimjeg.combatnexus.combat.api.platform.WorldLifecycleAccess lifecycle;
    public static boolean rejectedComputeLifecycle;
    public static boolean rejectsWrongThreadLifecycle(net.minecraft.server.level.ServerLevel level) {
        var rejected = new java.util.concurrent.atomic.AtomicBoolean();
        var worker = new Thread(() -> { try { lifecycle.loaded(level); } catch (IllegalStateException expected) { rejected.set(true); } });
        worker.start();
        try { worker.join(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
        return rejected.get();
    }
    private static io.github.naimjeg.combatnexus.combat.api.platform.EquipmentDamageAccess equipmentAccess;
    public static boolean rejectsExpiredEquipmentSink(net.minecraft.world.entity.LivingEntity target) {
        var captured = new java.util.concurrent.atomic.AtomicReference<java.util.function.Consumer<io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt>>();
        equipmentAccess.apply(target, target.damageSources().generic(), captured::set);
        try {
            captured.get().accept(new io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt(
                net.minecraft.world.entity.EquipmentSlot.BODY, Identifier.parse("minecraft:wolf_armor"), 0, 0, false));
            return false;
        } catch (IllegalStateException expected) { return true; }
    }
    private static final Identifier ID = Identifier.parse("combatnexus_effect_probe:condition");
    public EffectExtension(IEventBus bus) {
        var displays = DeferredRegister.create(Registries.MOB_EFFECT, "combatnexus_effect_probe");
        displays.register("condition", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x66AA88) {}); displays.register(bus);
        NeoForge.EVENT_BUS.addListener(EffectExtension::register);
        NeoForge.EVENT_BUS.addListener(EffectExtension::registerDamage);
    }
    private static io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext retainedDamageContext;
    public static boolean rejectsExpiredContribution() {
        try { retainedDamageContext.recordContribution(null, null); return false; }
        catch (IllegalStateException expected) { return true; }
    }
    private static final java.util.Map<java.util.UUID, io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput> INPUTS = new java.util.LinkedHashMap<>();
    public static io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput nativeInput(java.util.UUID target) { return INPUTS.get(target); }
    private static void registerDamage(io.github.naimjeg.combatnexus.combat.damage.platform.api.event.DamageModuleRegisterEvent event) {
        event.registerPhaseProcessor(Identifier.parse("combatnexus_effect_probe:native_input"), new io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor() {
            public io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase phase() {
                return io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase.BASE_MODIFICATION;
            }
            public int getPriority() { return Integer.MAX_VALUE; }
            public void apply(io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext context) {
                if (INPUTS.size() > 512) INPUTS.clear();
                try { lifecycle.beginTick((net.minecraft.server.level.ServerLevel)context.victim().level()); rejectedComputeLifecycle = false; }
                catch (IllegalStateException expected) { rejectedComputeLifecycle = true; }
                retainedDamageContext = context;
                INPUTS.put(context.victim().getUUID(), context.nativeInput());
            }
        });
    }
    private static void register(CombatRegisterEvent event) {
        var api = event.registrar();
        equipmentAccess = api.equipmentDamage();
        lifecycle = api.worldLifecycle();
        api.observeMoment(Identifier.parse("combatnexus_effect_probe:moments"), io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.class, moment -> {
            if (MOMENTS.size() == 4096) MOMENTS.removeFirst();
            MOMENTS.add(moment);
        });
        api.effectCondition(ID, facts -> facts.getOrDefault("active", 0.0) > 0);
        api.effectCapability(ID, context -> { TRIGGERS.merge(context.trigger(), 1, Integer::sum); return true; });
        api.effectDisplay(ID, new EffectDisplay(ID, false, false, true, null, false, false, true));
        api.effectStacking(Identifier.parse("combatnexus_effect_probe:keep_first"), (old, incoming, now) -> old);
        api.behavior(new BehaviorType<>(Identifier.parse("combatnexus_effect_probe:pulse"), Pulse.class,
            RecordCodecBuilder.mapCodec(i -> i.group(Codec.DOUBLE.fieldOf("amount").forGetter(Pulse::amount),
                Codec.INT.fieldOf("interval").forGetter(Pulse::intervalTicks)).apply(i, Pulse::new))));
        api.behavior(new BehaviorType<>(Identifier.parse("combatnexus_effect_probe:mystery"), Mystery.class, com.mojang.serialization.MapCodec.unit(new Mystery())));
        try { api.effectDescriptor(Identifier.parse("combatnexus_effect_probe:pulse"), Mystery.class, (value, level) -> null); }
        catch (IllegalArgumentException expected) { rejectedDescriptorMismatch = true; }
        api.effectDescriptor(Identifier.parse("combatnexus_effect_probe:pulse"), Pulse.class, (pulse, level) ->
            new io.github.naimjeg.combatnexus.presentation.api.TooltipBlock(net.minecraft.network.chat.Component.empty(),
                java.util.List.of(net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.effect.periodic",
                    io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.number(pulse.intervalTicks() / 20.0),
                    io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.number(pulse.amount()))), java.util.List.of()));
    }
    public record Mystery() implements BehaviorComponent {}
    public record Pulse(double amount, int intervalTicks) implements PeriodicBehavior {
        public PeriodicPhase phase() { return PeriodicPhase.PRESERVE; }
        public void execute(PeriodicExecution execution) { execution.heal(amount); }
    }
}
