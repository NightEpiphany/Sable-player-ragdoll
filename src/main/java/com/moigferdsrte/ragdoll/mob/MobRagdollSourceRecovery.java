package com.moigferdsrte.ragdoll.mob;

import com.moigferdsrte.ragdoll.physics.RagdollBlockLifetime;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class MobRagdollSourceRecovery {
    private static final String KEY = "sable_player_ragdoll_mob_recovery";

    private MobRagdollSourceRecovery() {}

    public static void begin(LivingEntity entity, UUID session, long expiresAt) {
        // Never snapshot flags that this mod has already overridden.
        restore(entity);
        CompoundTag tag = new CompoundTag();
        tag.store("Session", UUIDUtil.CODEC, session);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putLong("CreatedAt", entity.level().getGameTime());
        tag.putBoolean("Invisible", entity.isInvisible());
        tag.putBoolean("NoPhysics", entity.noPhysics);
        if (entity instanceof Mob mob) tag.putBoolean("NoAI", mob.isNoAi());
        com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity).put(KEY, tag);
        com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity).store(RagdollBlockLifetime.SOURCE_SESSION, UUIDUtil.CODEC, session);
    }

    public static boolean hasRecord(LivingEntity entity) {
        var data = com.moigferdsrte.ragdoll.fabric.FabricEntityData.peek(entity);
        return data != null && data.getCompound(KEY).flatMap(tag -> tag.read("Session", UUIDUtil.CODEC)).isPresent();
    }

    public static UUID session(LivingEntity entity) {
        return com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity).getCompound(KEY)
                .flatMap(tag -> tag.read("Session", UUIDUtil.CODEC)).orElse(null);
    }

    public static boolean active(LivingEntity entity) {
        var data = com.moigferdsrte.ragdoll.fabric.FabricEntityData.peek(entity);
        return data != null && data.getCompound(KEY).isPresent() && active(data, entity.level().getGameTime());
    }

    static boolean active(CompoundTag data, long now) {
        var tag = data.getCompoundOrEmpty(KEY);
        UUID session = tag.read("Session", UUIDUtil.CODEC).orElse(null);
        return session != null && session.equals(data.read(RagdollBlockLifetime.SOURCE_SESSION, UUIDUtil.CODEC).orElse(null))
                && now < tag.getLongOr("ExpiresAt", 0L);
    }

    public static boolean matches(LivingEntity entity, UUID session) {
        var data = com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity);
        return session != null && session.equals(data.read(RagdollBlockLifetime.SOURCE_SESSION, UUIDUtil.CODEC).orElse(null));
    }

    public static void extend(LivingEntity entity, long expiresAt) {
        CompoundTag data = com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity);
        CompoundTag tag = data.getCompound(KEY).orElse(null);
        if (tag == null || tag.read("Session", UUIDUtil.CODEC).isEmpty()) return;
        tag.putLong("ExpiresAt", expiresAt);
        data.put(KEY, tag);
    }

    public static void restore(LivingEntity entity) {
        var data = com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity);
        CompoundTag tag = data.getCompound(KEY).orElse(null);
        if (tag == null) return;
        UUID session = tag.read("Session", UUIDUtil.CODEC).orElse(null);
        if (session == null) return;
        if (matches(entity, session)) {
            data.remove(RagdollBlockLifetime.SOURCE_SESSION);
            entity.setInvisible(tag.getBooleanOr("Invisible", false));
            entity.noPhysics = tag.getBooleanOr("NoPhysics", false);
            if (entity instanceof Mob mob) mob.setNoAi(tag.getBooleanOr("NoAI", false));
        }
        data.remove(KEY);
        entity.refreshDimensions();
    }

    public static void clear(LivingEntity entity) {
        com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity).remove(RagdollBlockLifetime.SOURCE_SESSION);
        com.moigferdsrte.ragdoll.fabric.FabricEntityData.get(entity).remove(KEY);
    }
}
