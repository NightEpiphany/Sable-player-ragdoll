package com.moigferdsrte.ragdoll.physics;

import com.moigferdsrte.ragdoll.api.RagdollLimbConfig;
import com.moigferdsrte.ragdoll.api.RagdollLimbOptions;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class RagdollAssemblyData {
   private RagdollAssemblyData() {}

   public static void write(ServerLevel level, UUID owner, RagdollLimbOptions limbs) {
      CompoundTag record = encode(limbs);
      var cached = RagdollRelationships.currentMembers(level, owner);
      for (var be : cached == null ? RagdollBlockOwnership.loadedBlocks(level) : cached) {
         if (!(be instanceof RagdollPartBlockEntity)) continue;
         var identity = ((RagdollOwnedBlock) be).ragdollIdentity();
         if (!owner.equals(identity.owner())) continue;
         identity.assembly(record);
         be.setChanged();
      }
   }

   public static Map<BodyPart, UUID> parts(ServerLevel level, UUID owner) {
      Map<BodyPart, UUID> result = new EnumMap<>(BodyPart.class);
      var cached = RagdollRelationships.currentMembers(level, owner);
      for (var be : cached == null ? RagdollBlockOwnership.loadedBlocks(level) : cached) {
         if (!(be instanceof RagdollPartBlockEntity part)) continue;
         var identity = part.ragdollIdentity();
         if (owner.equals(identity.owner()) && !identity.severed() && identity.limb() != null)
            result.put(part.bodyPart(), identity.limb());
      }
      return result;
   }

   public static RagdollLimbOptions limbs(ServerLevel level, UUID owner) {
      CompoundTag record = record(level, owner);
      return record == null ? RagdollLimbOptions.defaults() : decodeLimbs(record);
   }

   public static @Nullable UUID ownerForLimb(ServerLevel level, UUID limb) {
      if (limb == null) return null;
      for (var be : RagdollBlockOwnership.loadedBlocks(level)) {
         if (!(be instanceof RagdollPartBlockEntity)) continue;
         var identity = ((RagdollOwnedBlock) be).ragdollIdentity();
         if (limb.equals(identity.limb()) && !identity.severed()) return identity.owner();
      }
      return null;
   }

   private static @Nullable CompoundTag record(ServerLevel level, UUID owner) {
      var cached = RagdollRelationships.currentMembers(level, owner);
      for (var be : cached == null ? RagdollBlockOwnership.loadedBlocks(level) : cached) {
         if (!(be instanceof RagdollPartBlockEntity)) continue;
         var identity = ((RagdollOwnedBlock) be).ragdollIdentity();
         if (owner.equals(identity.owner())) {
            return identity.assembly().copy();
         }
      }
      return null;
   }

   private static RagdollLimbOptions decodeLimbs(CompoundTag record) {
      if (!record.contains("Limbs")) return RagdollLimbOptions.defaults();
      RagdollLimbOptions.Builder builder = RagdollLimbOptions.builder();
      ListTag limbs = record.getListOrEmpty("Limbs");
      for (int index = 0; index < limbs.size(); index++) {
         CompoundTag limbTag = limbs.getCompoundOrEmpty(index);
         RagdollLimbConfig.Builder config = RagdollLimbConfig.builder();
         limbTag.getDouble("Pitch").ifPresent(config::pitch);
         limbTag.getDouble("Yaw").ifPresent(config::yaw);
         limbTag.getDouble("Roll").ifPresent(config::roll);
         var initialPitch = limbTag.getDouble("InitialPitch");
         var initialYaw = limbTag.getDouble("InitialYaw");
         var initialRoll = limbTag.getDouble("InitialRoll");
         if (initialPitch.isPresent() || initialYaw.isPresent() || initialRoll.isPresent()) {
            config.initialRotation(
               initialPitch.orElse(0.0),
               initialYaw.orElse(0.0),
               initialRoll.orElse(0.0)
            );
         }
         var right = limbTag.getDouble("Right");
         var up = limbTag.getDouble("Up");
         var forward = limbTag.getDouble("Forward");
         if (right.isPresent() || up.isPresent() || forward.isPresent()) {
            config.offset(
               right.orElse(0.0),
               up.orElse(0.0),
               forward.orElse(0.0)
            );
         }
         limbTag.getDouble("Stiffness").ifPresent(config::stiffness);
         limbTag.getDouble("Damping").ifPresent(config::damping);
         builder.limb(BodyPart.byName(limbTag.getStringOr("BodyPart", "")), config);
      }
      return builder.build();
   }

   private static CompoundTag encode(RagdollLimbOptions limbs) {
      CompoundTag record = new CompoundTag();
      if (limbs != null && !limbs.isEmpty()) {
         ListTag limbList = new ListTag();
         for (BodyPart part : BodyPart.values()) {
            RagdollLimbConfig config = limbs.get(part);
            if (config == null) continue;
            CompoundTag limbTag = new CompoundTag();
            limbTag.putString("BodyPart", part.getSerializedName());
            config.pitchDegrees().ifPresent(value -> limbTag.putDouble("Pitch", value));
            config.yawDegrees().ifPresent(value -> limbTag.putDouble("Yaw", value));
            config.rollDegrees().ifPresent(value -> limbTag.putDouble("Roll", value));
            config.initialPitchDegrees().ifPresent(value -> limbTag.putDouble("InitialPitch", value));
            config.initialYawDegrees().ifPresent(value -> limbTag.putDouble("InitialYaw", value));
            config.initialRollDegrees().ifPresent(value -> limbTag.putDouble("InitialRoll", value));
            config.rightOffset().ifPresent(value -> limbTag.putDouble("Right", value));
            config.upOffset().ifPresent(value -> limbTag.putDouble("Up", value));
            config.forwardOffset().ifPresent(value -> limbTag.putDouble("Forward", value));
            config.angularStiffness().ifPresent(value -> limbTag.putDouble("Stiffness", value));
            config.angularDamping().ifPresent(value -> limbTag.putDouble("Damping", value));
            limbList.add(limbTag);
         }
         if (!limbList.isEmpty()) record.put("Limbs", limbList);
      }
      return record;
   }
}
