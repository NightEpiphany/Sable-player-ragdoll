package com.moigferdsrte.ragdoll.mob;

import com.moigferdsrte.ragdoll.mob.block.MobPartRole;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.mob.api.MobRagdollLaunchOptions;
import com.moigferdsrte.ragdoll.physics.RagdollBlockOwnership;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class MobRagdollAssemblyData {
    private static final int DEFAULT_DURATION_TICKS = 80;
    private MobRagdollAssemblyData() {}

    public static void migrate(ServerLevel level, MobRagdollPartBlockEntity block,
                               dev.ryanhcode.sable.sublevel.ServerSubLevel body) {
        var id = block.ragdollIdentity();
        if (id.severed()) return;
        if (id.assembly().getIntOr("MobDataVersion", 0) >= 2) return;
        if (!block.renderAnchor() && id.createdAt() >= 0) {
            CompoundTag marker = new CompoundTag();
            marker.putInt("MobDataVersion", 2);
            id.assembly(marker);
            block.setChanged();
            return;
        }
        CompoundTag tag = id.assembly().copy();
        if (readUuid(tag, "EntityId").isEmpty() && body != null) {
            for (var item : MobRagdollSavedData.legacyEntries(level)) {
                CompoundTag candidate = (CompoundTag) item;
                var parts = candidate.getListOrEmpty("Parts");
                for (int i = 0; i < parts.size(); i++) {
                    var part = parts.getCompoundOrEmpty(i);
                    if (!body.getUniqueId().equals(readUuid(part, "SubLevelId").orElse(null))) continue;
                    tag = candidate.copy();
                    if (!id.known()) {
                        UUID sourceId = readUuid(candidate, "EntityId").orElse(null);
                        if (sourceId == null) continue;
                        UUID owner = UUID.nameUUIDFromBytes(("legacy-mob:" + sourceId + ":" + candidate.getLongOr("SpawnedAt", 0L))
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        block.setRagdollIdentity(owner, body.getUniqueId(), part.getStringOr("Name", ""));
                    }
                    break;
                }
                if (readUuid(tag, "EntityId").isPresent()) break;
            }
        }
        if (readUuid(tag, "EntityId").isEmpty() && id.limb() != null) {
            for (var be : RagdollBlockOwnership.loadedBlocks(level)) {
                if (be instanceof MobRagdollPartBlockEntity anchor && anchor.renderAnchor()
                        && id.limb().equals(anchor.ragdollIdentity().limb())
                        && readUuid(anchor.ragdollIdentity().assembly(), "EntityId").isPresent()) {
                    tag = anchor.ragdollIdentity().assembly().copy();
                    break;
                }
            }
        }
        Optional<UUID> sourceId = readUuid(tag, "EntityId");
        if (sourceId.isEmpty()) return;
        tag.putInt("MobDataVersion", 2);
        tag.remove("EntityData");
        tag.remove("EntityType");
        if (id.owner() != null) {
            tag.store("SessionId", UUIDUtil.CODEC, id.owner());
            boolean mobless = tag.getBooleanOr("Mobless", false);
            int duration = tag.getIntOr("DurationTicks", DEFAULT_DURATION_TICKS);
            long created = tag.getLongOr("SpawnedAt", 0L);
            boolean permanent = mobless && duration == MobRagdollAssembly.DEFAULT_MOBLESS_DURATION_TICKS;
            if (id.createdAt() < 0 || mobless) id.lifetime(created, permanent ? "PERMANENT" : mobless ? "TIMED" : "MOB",
                    mobless ? null : sourceId.get(), permanent ? -1 : created + (long) duration);
        }
        if (!block.renderAnchor()) {
            tag = new CompoundTag();
            tag.putInt("MobDataVersion", 2);
        }
        id.assembly(tag);
        block.setChanged();
    }

    public static Map<UUID, Entry> loaded(ServerLevel level) {
        return loaded(RagdollBlockOwnership.loadedBlocks(level));
    }

    public static Map<UUID, Entry> loaded(java.util.Collection<net.minecraft.world.level.block.entity.BlockEntity> blocks) {
        int expected = blocks.size();
        int capacity = expected >= (1 << 29) ? Integer.MAX_VALUE : Math.max(16, expected + (expected >> 1));
        Map<UUID, Entry> result = new HashMap<>(capacity);
        for (var be : blocks) {
            if (!(be instanceof MobRagdollPartBlockEntity mob) || !mob.renderAnchor()) continue;
            var id = mob.ragdollIdentity();
            if (id.owner() == null || id.severed()) continue;
            var tag = id.assembly();
            if (readUuid(tag, "EntityId").isPresent()) result.computeIfAbsent(id.owner(), owner -> decode(tag, owner));
        }
        return result;
    }

    public static void write(ServerLevel level, Entry entry) {
        CompoundTag tag = encode(entry);
        for (var be : RagdollBlockOwnership.loadedBlocks(level)) {
            if (!(be instanceof MobRagdollPartBlockEntity mob)) continue;
            var id = mob.ragdollIdentity();
            if (entry.sessionId().equals(id.owner()) && !id.severed()) {
                CompoundTag stored = mob.renderAnchor() ? tag : new CompoundTag();
                stored.putInt("MobDataVersion", 2);
                id.assembly(stored);
                be.setChanged();
            }
        }
    }

    public static CompoundTag encode(Entry entry) {
        CompoundTag entryTag = new CompoundTag();
        entryTag.putInt("MobDataVersion", 2);
        entryTag.store("EntityId", UUIDUtil.CODEC, entry.sourceId());
        entryTag.store("SessionId", UUIDUtil.CODEC, entry.sessionId());
        entryTag.putLong("SpawnedAt", entry.spawnedAtTick);
        entryTag.putInt("DurationTicks", entry.durationTicks);
        entryTag.putInt("CorpseDurationTicks", entry.corpseDurationTicks);
        entryTag.putBoolean("FallApartOnDeath", entry.fallApartOnDeath);
        entryTag.putBoolean("Mobless", entry.mobless);
        CompoundTag posTag = new CompoundTag();
        Vec3 pos = entry.preRagdollPos;
        posTag.putDouble("X", pos.x);
        posTag.putDouble("Y", pos.y);
        posTag.putDouble("Z", pos.z);
        entryTag.put("PreRagdollPos", posTag);
        ListTag partsTag = new ListTag();
        for (var partEntry : entry.partInfos.entrySet()) {
            CompoundTag partTag = new CompoundTag();
            PartInfo info = partEntry.getValue();
            partTag.putString("Name", partEntry.getKey());
            partTag.putString("Role", info.role().getSerializedName());
            partTag.putFloat("PivotX", info.pivotX());
            partTag.putFloat("PivotY", info.pivotY());
            partTag.putFloat("PivotZ", info.pivotZ());
            partTag.putFloat("CenterX", info.centerX());
            partTag.putFloat("CenterY", info.centerY());
            partTag.putFloat("CenterZ", info.centerZ());
            partTag.putFloat("RotQx", info.rotQx());
            partTag.putFloat("RotQy", info.rotQy());
            partTag.putFloat("RotQz", info.rotQz());
            partTag.putFloat("RotQw", info.rotQw());
            UUID subLevelId = entry.partIds.get(partEntry.getKey());
            if (subLevelId != null) {
                partTag.store("LimbId", UUIDUtil.CODEC, subLevelId);
            }
            partsTag.add(partTag);
        }
        entryTag.put("Parts", partsTag);
        return entryTag;
    }

    public static Entry decode(CompoundTag entryTag, UUID session) {
        long spawnedAtTick = entryTag.getLongOr("SpawnedAt", 0L);
        int durationTicks = entryTag.getIntOr("DurationTicks", DEFAULT_DURATION_TICKS);
        int corpseDurationTicks = entryTag.getIntOr("CorpseDurationTicks", MobRagdollLaunchOptions.DEFAULT_CORPSE_DURATION_TICKS);
        CompoundTag posTag = entryTag.getCompoundOrEmpty("PreRagdollPos");
        Vec3 preRagdollPos = new Vec3(posTag.getDoubleOr("X", 0.0), posTag.getDoubleOr("Y", 0.0), posTag.getDoubleOr("Z", 0.0));
        ListTag partsTag = entryTag.getListOrEmpty("Parts");
        int expectedParts = partsTag.size();
        int capacity = expectedParts >= (1 << 29) ? Integer.MAX_VALUE : Math.max(16, expectedParts + (expectedParts >> 1));
        Map<String, PartInfo> partInfos = new HashMap<>(capacity);
        Map<String, UUID> partIds = new HashMap<>(capacity);
        for (int j = 0; j < partsTag.size(); j++) {
            CompoundTag partTag = partsTag.getCompoundOrEmpty(j);
            String name = partTag.getStringOr("Name", "");
            MobPartRole role = MobPartRole.valueOf(partTag.getStringOr("Role", "OTHER").toUpperCase(java.util.Locale.ROOT));
            float pivotX = partTag.getFloatOr("PivotX", 0.0F);
            float pivotY = partTag.getFloatOr("PivotY", 0.0F);
            float pivotZ = partTag.getFloatOr("PivotZ", 0.0F);
            float centerX = partTag.getFloatOr("CenterX", 0.0F);
            float centerY = partTag.getFloatOr("CenterY", 0.0F);
            float centerZ = partTag.getFloatOr("CenterZ", 0.0F);
            float rotQx = partTag.getFloatOr("RotQx", 0.0F);
            float rotQy = partTag.getFloatOr("RotQy", 0.0F);
            float rotQz = partTag.getFloatOr("RotQz", 0.0F);
            float rotQw = partTag.getFloatOr("RotQw", 1.0F);
            partInfos.put(name, new PartInfo(role, pivotX, pivotY, pivotZ, centerX, centerY, centerZ,
                    rotQx, rotQy, rotQz, rotQw));
            readUuid(partTag, "LimbId").or(() -> readUuid(partTag, "SubLevelId")).ifPresent(uuid -> partIds.put(name, uuid));
        }
        boolean mobless = entryTag.getBooleanOr("Mobless", false);
        UUID sourceId = readUuid(entryTag, "EntityId").orElseThrow(() -> new IllegalArgumentException("Missing mob ragdoll EntityId"));
        return new Entry(session, sourceId, spawnedAtTick, durationTicks, corpseDurationTicks,
                entryTag.getBooleanOr("FallApartOnDeath", false), preRagdollPos, partInfos, partIds, mobless);
    }

    private static Optional<UUID> readUuid(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC);
    }

    public record Entry(UUID sessionId, UUID sourceId, long spawnedAtTick, int durationTicks,
                        int corpseDurationTicks, boolean fallApartOnDeath, Vec3 preRagdollPos,
                        Map<String, PartInfo> partInfos, Map<String, UUID> partIds, boolean mobless) {
        public Entry {
            partInfos = Map.copyOf(partInfos);
            partIds = Map.copyOf(partIds);
        }
    }

    public record PartInfo(MobPartRole role, float pivotX, float pivotY, float pivotZ,
                            float centerX, float centerY, float centerZ,
                            float rotQx, float rotQy, float rotQz, float rotQw) {
    }
}
