package com.moigferdsrte.ragdoll.entity;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class RagdollDollEntity extends LivingEntity {
   private static final EntityDataAccessor<String> DATA_SKIN_UUID = SynchedEntityData.defineId(RagdollDollEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> DATA_SKIN_NAME = SynchedEntityData.defineId(RagdollDollEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> DATA_SKIN_TEXTURES = SynchedEntityData.defineId(RagdollDollEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> DATA_SKIN_TEXTURES_SIGNATURE = SynchedEntityData.defineId(
      RagdollDollEntity.class, EntityDataSerializers.STRING
   );
   private static final EntityDataAccessor<Integer> DATA_BODY_PART = SynchedEntityData.defineId(RagdollDollEntity.class, EntityDataSerializers.INT);
   private final java.util.EnumMap<EquipmentSlot, ItemStack> equipment = new java.util.EnumMap<>(EquipmentSlot.class);

   public RagdollDollEntity(EntityType<? extends RagdollDollEntity> entityType, Level level) {
      super(entityType, level);
   }

   public RagdollDollEntity(Level level) {
      this(RagdollSeatEntities.ragdollDoll(), level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return LivingEntity.createLivingAttributes()
         .add(Attributes.MAX_HEALTH, 0.0)
         .add(Attributes.MOVEMENT_SPEED, 0.0)
         .add(Attributes.STEP_HEIGHT, 0.0);
   }

   public void setSkinProfile(GameProfile profile) {
      this.entityData.set(DATA_SKIN_UUID, profile.id() == null ? "" : profile.id().toString());
      this.entityData.set(DATA_SKIN_NAME, profile.name() == null ? "" : profile.name());
      Property textures = profile.properties().get("textures").stream().findFirst().orElse(null);
      this.entityData.set(DATA_SKIN_TEXTURES, textures == null ? "" : textures.value());
      this.entityData.set(DATA_SKIN_TEXTURES_SIGNATURE, textures == null || textures.signature() == null ? "" : textures.signature());
   }

   public GameProfile getSkinProfile() {
      Optional<UUID> uuid = this.getSkinUuid();
      String name = this.entityData.get(DATA_SKIN_NAME);
      String textures = this.entityData.get(DATA_SKIN_TEXTURES);
      String signature = this.entityData.get(DATA_SKIN_TEXTURES_SIGNATURE);
      PropertyMap properties = textures.isBlank()
            ? PropertyMap.EMPTY
            : new PropertyMap(ImmutableMultimap.of(
                  "textures",
                  signature.isBlank() ? new Property("textures", textures) : new Property("textures", textures, signature)
            ));
      return new GameProfile(uuid.orElse(this.getUUID()), name.isBlank() ? "Player" : name, properties);
   }

   private Optional<UUID> getSkinUuid() {
      String value = this.entityData.get(DATA_SKIN_UUID);
      if (value.isBlank()) return Optional.empty();
      try {
         return Optional.of(UUID.fromString(value));
      } catch (IllegalArgumentException ignored) {
         return Optional.empty();
      }
   }

   public void setBodyPart(BodyPart bodyPart) {
      this.entityData.set(DATA_BODY_PART, bodyPart.id);
   }

   public BodyPart getBodyPart() {
      return BodyPart.byId(this.entityData.get(DATA_BODY_PART));
   }

   @Override
   public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
      return !(entity instanceof RagdollDollEntity) && super.canCollideWith(entity);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_SKIN_UUID, "");
      builder.define(DATA_SKIN_NAME, "");
      builder.define(DATA_SKIN_TEXTURES, "");
      builder.define(DATA_SKIN_TEXTURES_SIGNATURE, "");
      builder.define(DATA_BODY_PART, BodyPart.TORSO.id);
   }

   @Override
   public void readAdditionalSaveData(ValueInput input) {
      this.entityData.set(DATA_SKIN_UUID, input.read("SkinUuid", UUIDUtil.CODEC).map(UUID::toString).orElse(""));
      this.entityData.set(DATA_SKIN_NAME, input.getStringOr("SkinName", ""));
      this.entityData.set(DATA_SKIN_TEXTURES, input.getStringOr("SkinTextures", ""));
      this.entityData.set(DATA_SKIN_TEXTURES_SIGNATURE, input.getStringOr("SkinTexturesSignature", ""));
      this.entityData.set(DATA_BODY_PART, input.getIntOr("BodyPart", BodyPart.TORSO.id));
   }

   @Override
   public void addAdditionalSaveData(ValueOutput output) {
      this.getSkinUuid().ifPresent(uuid -> output.store("SkinUuid", UUIDUtil.CODEC, uuid));
      output.putString("SkinName", this.entityData.get(DATA_SKIN_NAME));
      output.putString("SkinTextures", this.entityData.get(DATA_SKIN_TEXTURES));
      output.putString("SkinTexturesSignature", this.entityData.get(DATA_SKIN_TEXTURES_SIGNATURE));
      output.putInt("BodyPart", this.entityData.get(DATA_BODY_PART));
   }

   @Override
   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
   }

   @Override
   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
      this.equipment.put(slot, stack.copy());
   }

   @Override
   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   public enum BodyPart {
      TORSO(0),
      HEAD(1),
      LEFT_ARM(2),
      RIGHT_ARM(3),
      LEFT_LEG(4),
      RIGHT_LEG(5);

      private static final BodyPart[] VALUES = values();
      public final int id;

      BodyPart(int id) {
         this.id = id;
      }

      public static BodyPart byId(int id) {
         return id >= 0 && id < VALUES.length ? VALUES[id] : TORSO;
      }
   }
}
