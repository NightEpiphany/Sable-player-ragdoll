package com.moigferdsrte.ragdoll.fabric.network;

import java.util.Objects;

/** Small adapter that keeps packet logic independent from Fabric's callback implementation. */
public final class FabricPayloadContext {
    private final Object player;

    public FabricPayloadContext(Object player) {
        this.player = player;
    }

    public Object player() {
        return player;
    }

    public void enqueueWork(Runnable task) {
        Objects.requireNonNull(task).run();
    }
}
