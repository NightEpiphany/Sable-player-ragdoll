package com.moigferdsrte.ragdoll.mob.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class MobRagdollStartEvent {
    private final LivingEntity entity;
    private Vec3 velocity;
    private boolean canceled;

    public MobRagdollStartEvent(LivingEntity entity, Vec3 velocity) {
        this.entity = entity;
        this.velocity = velocity;
    }

    public LivingEntity entity() {
        return this.entity;
    }

    public Vec3 velocity() {
        return this.velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
