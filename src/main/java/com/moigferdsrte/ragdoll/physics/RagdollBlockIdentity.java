package com.moigferdsrte.ragdoll.physics;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public final class RagdollBlockIdentity {
    private static final String KEY = "RagdollIdentity";
    private UUID owner;
    private UUID limb;
    private UUID parent;
    private long orphanDeadline = -1;
    long nextRelationshipCheck;
    long nextJointCheck;
    long nextPolicyCheck;
    net.minecraft.world.level.block.entity.BlockEntity parentReference;
    java.util.List<java.lang.ref.WeakReference<net.minecraft.world.level.block.entity.BlockEntity>> relatives = java.util.List.of();
    java.util.Map<UUID, UUID> jointBodies = java.util.Map.of();

    public @Nullable UUID parent() { return parent; }
    public void parent(@Nullable UUID parent) {
        this.parent = parent;
        parentReference = null;
        nextRelationshipCheck = 0;
        // Reparenting does not extend an existing unresolved deadline
        if (parent == null) orphanDeadline = -1;
    }
    public boolean orphanExpired(long now) { return orphanDeadline >= 0 && now >= orphanDeadline; }
    public void parentResolved() { orphanDeadline = -1; }
    public void parentMissing(long now) {
        if (orphanDeadline < 0) orphanDeadline = now + RagdollBlockLifetime.CREATION_GRACE_TICKS;
    }
    private void resetReferences() {
        relatives = java.util.List.of();
        parentReference = null;
        jointBodies = java.util.Map.of();
        nextRelationshipCheck = 0;
        nextJointCheck = 0;
        nextPolicyCheck = 0;
    }
    private String kind = "";
    private boolean severed;
    private long createdAt = -1;
    private long expiresAt = -1;
    private String lifetime = "UNBOUND";
    private UUID source;
    private CompoundTag assembly = new CompoundTag();
    private CompoundTag session = new CompoundTag();
    private long loadedAt = -1;

    public CompoundTag assembly() { return assembly; }
    public void assembly(CompoundTag tag) { assembly = tag.copy(); }
    public boolean ready(long now) {
        if (loadedAt < 0) loadedAt = now;
        return now - loadedAt >= RagdollBlockLifetime.CREATION_GRACE_TICKS;
    }

    public long createdAt() { return createdAt; }
    public long expiresAt() { return expiresAt; }
    public String lifetime() { return lifetime; }
    public UUID source() { return source; }
    public CompoundTag session() { return session; }
    public void session(CompoundTag tag) { session = tag.copy(); }
    public void lifetime(long createdAt, String mode, UUID source, long expiresAt) {
        this.createdAt = createdAt;
        this.lifetime = mode;
        this.source = source;
        this.expiresAt = expiresAt;
    }

    public @Nullable UUID owner() { return owner; }
    public @Nullable UUID limb() { return limb; }
    public String kind() { return kind; }
    public boolean severed() { return severed; }
    public boolean known() { return limb != null; }

    public void assign(UUID owner, UUID limb, String kind) {
        this.owner = owner;
        this.limb = limb;
        this.kind = kind;
        this.severed = false;
        resetReferences();
    }

    public void sever(long now) {
        createdAt = now;
        owner = null;
        parent = null;
        orphanDeadline = -1;
        resetReferences();
        source = null;
        session = new CompoundTag();
        assembly = new CompoundTag();
        lifetime = "TIMED";
        expiresAt = createdAt + 1200;
        severed = true;
    }

    public void save(ValueOutput output) {
        ValueOutput identity = output.child(KEY);
        identity.putInt("Version", 3);
        identity.storeNullable("Parent", UUIDUtil.CODEC, parent);
        identity.putLong("OrphanDeadline", orphanDeadline);
        identity.putLong("CreatedAt", createdAt);
        identity.putLong("ExpiresAt", expiresAt);
        identity.putString("Lifetime", lifetime);
        identity.storeNullable("Source", UUIDUtil.CODEC, source);
        identity.store("Session", CompoundTag.CODEC, session);
        identity.store("Assembly", CompoundTag.CODEC, assembly);
        identity.storeNullable("Owner", UUIDUtil.CODEC, owner);
        identity.storeNullable("Limb", UUIDUtil.CODEC, limb);
        identity.putString("Kind", kind);
        identity.putBoolean("Severed", severed);
    }

    public void load(ValueInput input) {
        ValueInput identity = input.childOrEmpty(KEY);
        createdAt = identity.getLongOr("CreatedAt", -1);
        expiresAt = identity.getLongOr("ExpiresAt", -1);
        lifetime = identity.getStringOr("Lifetime", "UNBOUND");
        source = identity.read("Source", UUIDUtil.CODEC).orElse(null);
        session = identity.read("Session", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        assembly = identity.read("Assembly", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        loadedAt = -1;
        limb = identity.read("Limb", UUIDUtil.CODEC).orElse(null);
        severed = identity.getBooleanOr("Severed", false);
        owner = severed ? null : identity.read("Owner", UUIDUtil.CODEC).orElse(null);
        kind = identity.getStringOr("Kind", "");
        parent = identity.read("Parent", UUIDUtil.CODEC).orElse(null);
        // Version 2 player limbs depended directly on the torso. Mob ownership IDs
        // are not necessarily limb IDs, so do not infer a mob hierarchy here.
        if (identity.getIntOr("Version", 0) < 3 && owner != null && !owner.equals(limb)
                && session.contains("startTick")) parent = owner;
        orphanDeadline = identity.getLongOr("OrphanDeadline", -1);
        resetReferences();
    }
}
