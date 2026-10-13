package com.moigferdsrte.ragdoll.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.entity.RagdollDollEntity;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.mob.client.EmfVanillaModelCompat;
import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import net.minecraft.client.CameraType;
import com.moigferdsrte.ragdoll.mixin.client.LivingEntityRendererAccessor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RagdollPartBlockEntityRenderer implements BlockEntityRenderer<RagdollPartBlockEntity>, RenderLayerParent<RagdollDollEntity, PlayerModel<RagdollDollEntity>> {
   private static PlayerModel<RagdollDollEntity> activeModel = null;
   private static BodyPart activeBodyPart = null;

   public static PlayerModel<RagdollDollEntity> currentModel() {
      return activeModel;
   }

   public static BodyPart activeBodyPart() {
      return activeBodyPart;
   }

   private final PlayerModel<RagdollDollEntity> defaultModel;
   private final PlayerModel<RagdollDollEntity> slimModel;
   private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();
   private final ItemStack[] savedItems = new ItemStack[EQUIPMENT_SLOTS.length];
   private final float[] savedPartY = new float[6];
   private final ModelPart[] defaultParts;
   private final ModelPart[] slimParts;
   private final Set<Class<?>> failedLayers = new HashSet<>();
   private final boolean accessoriesLoaded = FabricLoader.getInstance().isModLoaded("accessories");
   private PlayerModel<RagdollDollEntity> model;
   private final ModelPart defaultCloak;
   private final ModelPart slimCloak;
   private final ElytraLayer<RagdollDollEntity, PlayerModel<RagdollDollEntity>> elytraLayer;
   private RagdollDollEntity renderEntity;
   private ResourceLocation currentTexture = DefaultPlayerSkin.getDefaultTexture();
   private ResourceLocation currentCapeTexture = null;

   public RagdollPartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
      ModelPart defaultRoot = context.bakeLayer(ModelLayers.PLAYER);
      ModelPart slimRoot = context.bakeLayer(ModelLayers.PLAYER_SLIM);
      this.defaultModel = new PlayerModel<>(defaultRoot, false);
      this.slimModel = new PlayerModel<>(slimRoot, true);
      this.defaultParts = mainParts(this.defaultModel);
      this.slimParts = mainParts(this.slimModel);
      this.defaultCloak = defaultRoot.getChild("cloak");
      this.slimCloak = slimRoot.getChild("cloak");
      this.model = this.defaultModel;
      this.elytraLayer = new ElytraLayer<>(this, context.getModelSet());
   }

   @Override
   public void render(RagdollPartBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
      BodyPart bodyPart = blockEntity.bodyPart();
      if (bodyPart == BodyPart.HEAD
            && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON
            && Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.getVehicle() instanceof RagdollSeatEntity
            && Minecraft.getInstance().player.getUUID().equals(blockEntity.skinProfile().getId())) {
         return;
      }
      PlayerSkin skin = this.skin(blockEntity);
      this.model = skin.model() == PlayerSkin.Model.SLIM ? this.slimModel : this.defaultModel;
      try (EmfVanillaModelCompat.Session ignored = EmfVanillaModelCompat.enter(this.model)) {
         this.currentTexture = skin.texture();
         this.currentCapeTexture = skin.capeTexture();
         this.showOnly(bodyPart);
         this.renderEntity(blockEntity); // keep renderEntity up-to-date for elytra/cape
         LivingEntity entity = this.getRenderEntity(blockEntity);

         activeModel = this.model;
         activeBodyPart = bodyPart;
         poseStack.pushPose();
         try {
            this.positionPart(bodyPart, poseStack);
            this.renderLayers(blockEntity, bodyPart, entity, poseStack, buffer, packedLight, partialTick);
         } finally {
            poseStack.popPose();
            activeModel = null;
            activeBodyPart = null;
         }
      }
   }

   private void positionPart(BodyPart bodyPart, PoseStack poseStack) {
      poseStack.translate(0.5F, 0.5F, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      float partScale = bodyPart.renderScale();
      poseStack.scale(-partScale, -partScale, partScale);
      this.centerVisiblePart(bodyPart, poseStack);
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private void renderLayers(RagdollPartBlockEntity blockEntity, BodyPart bodyPart, LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
      // Render the base model — use hurt overlay when the real player is in their hurt window
      int baseOverlay = !blockEntity.isCorpse() && entity.hurtTime > 0
          ? OverlayTexture.pack(0, 3)  // u=0 (full flash), v=3 (red hurt row)
          : OverlayTexture.NO_OVERLAY;
      VertexConsumer vertices = buffer.getBuffer(RenderType.entityTranslucent(this.currentTexture));
      this.model.renderToBuffer(poseStack, vertices, packedLight, baseOverlay);

      // Render all vanilla layers from the standard PlayerRenderer
      Minecraft minecraft = Minecraft.getInstance();
      boolean slim = this.model == this.slimModel;
      var playerRenderer = ((com.moigferdsrte.ragdoll.mixin.client.EntityRenderDispatcherAccessor) minecraft.getEntityRenderDispatcher()).ragdoll$playerRenderers().get(slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE);

      if ((Object) playerRenderer instanceof LivingEntityRendererAccessor accessor) {
         boolean wasInvisible = entity.isInvisible();
         entity.setInvisible(false);

         // Only populate slots belonging to this body part leave the rest empty.
         for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            savedItems[slot.ordinal()] = entity.getItemBySlot(slot);
            ItemStack candidate = blockEntity.itemBySlot(slot);
            if (accessoriesLoaded && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && blockEntity.hasAccessoriesItems()) {
               ItemStack cosmetic = AccessoriesRenderHelper.storedCosmeticArmorOverride(blockEntity, slot);
               if (cosmetic != null) candidate = cosmetic;
            }
            ItemStack toShow = showsSlot(bodyPart, slot) && !isArmorSlotBlockedForPart(bodyPart, slot, candidate)
               ? candidate : ItemStack.EMPTY;
            entity.setItemSlot(slot, toShow);
         }

         // Displace all model parts that don't belong to this block
         ModelPart currentPart = visiblePart(bodyPart);
         ModelPart[] allMainParts = slim ? slimParts : defaultParts;
         for (int j = 0; j < allMainParts.length; j++) {
            savedPartY[j] = allMainParts[j].y;
            if (allMainParts[j] != currentPart) allMainParts[j].y += 10000f;
         }

         try {
            for (var layer : accessor.getLayers()) {
               String layerClass = layer.getClass().getName();
               // Curios/Accessories handled separately; ElytraLayer handled explicitly below.
               if (layerClass.equals("top.theillusivec4.curios.client.render.CuriosLayer")
                     || layerClass.equals("io.wispforest.accessories.client.AccessoriesRenderLayer")
                     || layer instanceof ElytraLayer<?, ?> || failedLayers.contains(layer.getClass())) {
                  continue;
               }
               // YDM Weapon Master
               if (bodyPart != BodyPart.TORSO
                     && (layerClass.equals("com.minecraftserverzone.weaponmaster.itemlayers.HumanoidItemLayer")
                        || layerClass.equals("com.minecraftserverzone.weaponmaster.itemlayers.HumanoidItemLayerLac"))) {
                  continue;
               }
               try {
                  ((net.minecraft.client.renderer.entity.layers.RenderLayer) layer).render(
                     poseStack, buffer, packedLight, entity, 0.0F, 0.0F, partialTick, entity.tickCount + partialTick, 0.0F, 0.0F
                  );
               } catch (ClassCastException e) {
                  failedLayers.add(layer.getClass());
               } catch (Exception e) {
                  if (failedLayers.add(layer.getClass()))
                     SablePlayerRagdoll.LOGGER.warn("Skipping incompatible ragdoll render layer {}", layerClass, e);
               }
            }
         } finally {
            for (int j = 0; j < allMainParts.length; j++) allMainParts[j].y = savedPartY[j];
            entity.setInvisible(wasInvisible);
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
               entity.setItemSlot(slot, savedItems[slot.ordinal()]);
               savedItems[slot.ordinal()] = null;
            }
         }
      }

      if (bodyPart == BodyPart.TORSO && this.renderEntity != null) {
         this.elytraLayer.render(poseStack, buffer, packedLight, this.renderEntity, 0.0F, 0.0F, partialTick, 0.0F, 0.0F, 0.0F);
         this.renderCape(blockEntity, entity, poseStack, buffer, packedLight);
      }

      if (accessoriesLoaded) {
         if (blockEntity.hasAccessoriesItems()) {
            AccessoriesRenderHelper.renderFromStored(bodyPart, blockEntity, entity, this, poseStack, buffer, packedLight, partialTick);
         }
      }

   }

   @Override
   public PlayerModel<RagdollDollEntity> getModel() {
      return this.model;
   }

   @Override
   public ResourceLocation getTextureLocation(RagdollDollEntity entity) {
      return this.currentTexture;
   }

   private PlayerSkin skin(RagdollPartBlockEntity blockEntity) {
      if (Minecraft.getInstance().getSkinManager() == null) {
         return DefaultPlayerSkin.get(blockEntity.skinProfile());
      }
      return Minecraft.getInstance().getSkinManager().getInsecureSkin(blockEntity.skinProfile());
   }

   private LivingEntity getRenderEntity(RagdollPartBlockEntity blockEntity) {
      Minecraft minecraft = Minecraft.getInstance();
      if (!blockEntity.isCorpse() && minecraft.level != null) {
         UUID uuid = blockEntity.skinProfile().getId();
         if (uuid != null) {
            Player player = minecraft.level.getPlayerByUUID(uuid);
            if (player != null) {
               return player;
            }
         }
      }
      return this.renderEntity(blockEntity);
   }

   private RagdollDollEntity renderEntity(RagdollPartBlockEntity blockEntity) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null && (this.renderEntity == null || this.renderEntity.level() != minecraft.level)) {
         this.renderEntity = new RagdollDollEntity(minecraft.level);
      }
      if (this.renderEntity != null) {
         this.renderEntity.setSkinProfile(blockEntity.skinProfile());
         this.renderEntity.setItemSlot(EquipmentSlot.MAINHAND, blockEntity.itemBySlot(EquipmentSlot.MAINHAND));
         this.renderEntity.setItemSlot(EquipmentSlot.OFFHAND, blockEntity.itemBySlot(EquipmentSlot.OFFHAND));
         this.renderEntity.setItemSlot(EquipmentSlot.HEAD, blockEntity.itemBySlot(EquipmentSlot.HEAD));
         this.renderEntity.setItemSlot(EquipmentSlot.CHEST, blockEntity.itemBySlot(EquipmentSlot.CHEST));
         this.renderEntity.setItemSlot(EquipmentSlot.LEGS, blockEntity.itemBySlot(EquipmentSlot.LEGS));
         this.renderEntity.setItemSlot(EquipmentSlot.FEET, blockEntity.itemBySlot(EquipmentSlot.FEET));
      }
      return this.renderEntity;
   }

   private void showOnly(BodyPart bodyPart) {
      this.model.setAllVisible(false);
      this.model.young = false;
      this.model.crouching = false;

      switch (bodyPart) {
         case HEAD -> { this.model.head.visible = true; this.model.hat.visible = true; }
         case LEFT_ARM -> { this.model.leftArm.visible = true; this.model.leftSleeve.visible = true; }
         case RIGHT_ARM -> { this.model.rightArm.visible = true; this.model.rightSleeve.visible = true; }
         case LEFT_LEG -> { this.model.leftLeg.visible = true; this.model.leftPants.visible = true; }
         case RIGHT_LEG -> { this.model.rightLeg.visible = true; this.model.rightPants.visible = true; }
         default -> { this.model.body.visible = true; this.model.jacket.visible = true; }
      }
   }

   private void centerVisiblePart(BodyPart bodyPart, PoseStack poseStack) {
      ModelPart part = this.visiblePart(bodyPart);
      poseStack.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
            // Per-part corrections, in pixels (1/16 block) like the pivot above.
      switch (bodyPart) {
         case HEAD -> nudge(poseStack, 0.0F, 4.0F, 0.0F);
         case TORSO -> nudge(poseStack, 0.0F, -6.5F, 0.0F);
         case LEFT_ARM -> nudge(poseStack, -2.0F, -4.0F, 0.0F);
         case RIGHT_ARM -> nudge(poseStack, 0.0F, -4.0F, 0.0F);
         case LEFT_LEG -> nudge(poseStack, -1.0F, -6.0F, 0.0F);
         case RIGHT_LEG -> nudge(poseStack, -1.0F, -6.0F, 0.0F);
      }
   }

   // Per-part offset in pixels (1/16 of a block).
   private static void nudge(PoseStack poseStack, float xPixels, float yPixels, float zPixels) {
      poseStack.translate(xPixels / 16.0F, yPixels / 16.0F, zPixels / 16.0F);
   }

   private ModelPart visiblePart(BodyPart bodyPart) {
      return switch (bodyPart) {
         case HEAD -> this.model.head;
         case LEFT_ARM -> this.model.leftArm;
         case RIGHT_ARM -> this.model.rightArm;
         case LEFT_LEG -> this.model.leftLeg;
         case RIGHT_LEG -> this.model.rightLeg;
         case TORSO -> this.model.body;
      };
   }

   private static ModelPart[] mainParts(PlayerModel<?> model) {
      return new ModelPart[] {model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg};
   }

   private static boolean showsSlot(BodyPart bodyPart, EquipmentSlot slot) {
      return switch (bodyPart) {
         case HEAD      -> slot == EquipmentSlot.HEAD;
         case TORSO     -> slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
         case RIGHT_ARM -> slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.CHEST;
         case LEFT_ARM  -> slot == EquipmentSlot.OFFHAND || slot == EquipmentSlot.CHEST;
         case LEFT_LEG, RIGHT_LEG -> slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
      };
   }

   private static boolean isArmorSlotBlockedForPart(BodyPart bodyPart, EquipmentSlot slot, ItemStack item) {
      if (slot != EquipmentSlot.CHEST) return false;
      if (bodyPart != BodyPart.LEFT_ARM && bodyPart != BodyPart.RIGHT_ARM) return false;
      return !(item.getItem() instanceof net.minecraft.world.item.ArmorItem);
   }

   private void renderCape(RagdollPartBlockEntity blockEntity, LivingEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (this.currentCapeTexture == null) return;
      if (blockEntity.itemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
      if (entity instanceof net.minecraft.client.player.AbstractClientPlayer acp && !acp.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.CAPE)) return;
      ModelPart cloak = this.model == this.slimModel ? this.slimCloak : this.defaultCloak;
      cloak.visible = true;
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
      poseStack.translate(0.0F, 0.0F, -0.125F);
      poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
      VertexConsumer vertices = buffer.getBuffer(RenderType.entitySolid(this.currentCapeTexture));
      cloak.render(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY);
      poseStack.popPose();
   }

}
