package com.moigferdsrte.ragdoll.fabric;

import com.moigferdsrte.ragdoll.api.RagdollEndEvent;
import com.moigferdsrte.ragdoll.api.RagdollInteractEvent;
import com.moigferdsrte.ragdoll.api.RagdollStartEvent;
import com.moigferdsrte.ragdoll.mob.api.MobRagdollEndEvent;
import com.moigferdsrte.ragdoll.mob.api.MobRagdollStartEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RagdollFabricEvents {
    private static final List<Consumer<RagdollStartEvent>> PLAYER_START = new ArrayList<>();
    private static final List<Consumer<RagdollEndEvent>> PLAYER_END = new ArrayList<>();
    private static final List<Consumer<RagdollInteractEvent>> PLAYER_INTERACT = new ArrayList<>();
    private static final List<Consumer<MobRagdollStartEvent>> MOB_START = new ArrayList<>();
    private static final List<Consumer<MobRagdollEndEvent>> MOB_END = new ArrayList<>();

    private RagdollFabricEvents() {
    }

    public static void onPlayerStart(Consumer<RagdollStartEvent> listener) { PLAYER_START.add(listener); }
    public static void onPlayerEnd(Consumer<RagdollEndEvent> listener) { PLAYER_END.add(listener); }
    public static void onPlayerInteract(Consumer<RagdollInteractEvent> listener) { PLAYER_INTERACT.add(listener); }
    public static void onMobStart(Consumer<MobRagdollStartEvent> listener) { MOB_START.add(listener); }
    public static void onMobEnd(Consumer<MobRagdollEndEvent> listener) { MOB_END.add(listener); }

    public static RagdollStartEvent fire(RagdollStartEvent event) {
        PLAYER_START.forEach(listener -> listener.accept(event));
        return event;
    }

    public static RagdollEndEvent fire(RagdollEndEvent event) {
        PLAYER_END.forEach(listener -> listener.accept(event));
        return event;
    }

    public static RagdollInteractEvent fire(RagdollInteractEvent event) {
        PLAYER_INTERACT.forEach(listener -> listener.accept(event));
        return event;
    }

    public static MobRagdollStartEvent fire(MobRagdollStartEvent event) {
        MOB_START.forEach(listener -> listener.accept(event));
        return event;
    }

    public static MobRagdollEndEvent fire(MobRagdollEndEvent event) {
        MOB_END.forEach(listener -> listener.accept(event));
        return event;
    }
}
