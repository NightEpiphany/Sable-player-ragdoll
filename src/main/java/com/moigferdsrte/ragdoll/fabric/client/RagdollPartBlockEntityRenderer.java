package com.moigferdsrte.ragdoll.fabric.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.entity.RagdollDollEntity;
import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.mixin.client.LivingEntityRendererAccessor;
import com.moigferdsrte.ragdoll.mob.client.EmfVanillaModelCompat;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RagdollPartBlockEntityRenderer
      implements BlockEntityRenderer<RagdollPartBlockEntity, RagdollPartBlockEntityRenderer.RenderState>,
      RenderLayerParent<AvatarRenderState, PlayerModel> {
   private static PlayerModel activeModel;
   private static BodyPart activeBodyPart;
   private final SplitPlayerModel defaultModel;
   private final SplitPlayerModel slimModel;
   private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;
   private PlayerModel model;

   public RagdollPartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
      ModelPart defaultRoot = context.bakeLayer(ModelLayers.PLAYER);
      ModelPart slimRoot = context.bakeLayer(ModelLayers.PLAYER_SLIM);
      this.defaultModel = new SplitPlayerModel(defaultRoot, false);
      this.slimModel = new SplitPlayerModel(slimRoot, true);
      this.itemModelResolver = context.itemModelResolver();
      this.model = this.defaultModel;
   }

   public static PlayerModel currentModel() {
      return activeModel;
   }

   public static BodyPart activeBodyPart() {
      return activeBodyPart;
   }

   @Override
   public RenderState createRenderState() {
      return new RenderState();
   }

   @Override
   public boolean shouldRender(RagdollPartBlockEntity blockEntity, Vec3 cameraPosition) {
      // Sable extracts sub-level block entities with a transformed (local)
      // camera position, while the block entity keeps its global plot
      // coordinates. The vanilla distance check therefore culls every part.
      return !blockEntity.isRemoved();
   }

   @Override
   public void extractRenderState(
         RagdollPartBlockEntity blockEntity,
         RenderState state,
         float partialTick,
         Vec3 cameraPosition,
         ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
      state.bodyPart = blockEntity.bodyPart();
      state.corpse = blockEntity.isCorpse();
      state.profile = blockEntity.skinProfile();
      state.skin = this.resolveSkin(state.profile);
      state.head = blockEntity.itemBySlot(EquipmentSlot.HEAD).copy();
      state.chest = blockEntity.itemBySlot(EquipmentSlot.CHEST).copy();
      state.legs = blockEntity.itemBySlot(EquipmentSlot.LEGS).copy();
      state.feet = blockEntity.itemBySlot(EquipmentSlot.FEET).copy();
      state.mainHand = blockEntity.itemBySlot(EquipmentSlot.MAINHAND).copy();
      state.offHand = blockEntity.itemBySlot(EquipmentSlot.OFFHAND).copy();
      state.avatar.bodyPart = state.bodyPart;
      state.avatar.skin = state.skin;
      state.avatar.lightCoords = state.lightCoords;
      state.avatar.showCape = state.skin.cape() != null;

      Minecraft minecraft = Minecraft.getInstance();
      state.source = null;
      if (!state.corpse && minecraft.level != null) {
         UUID profileId = state.profile.id();
         if (profileId != null) state.source = minecraft.level.getPlayerByUUID(profileId);
      }
      if (state.source == null && minecraft.level != null) {
         if (state.doll == null || state.doll.level() != minecraft.level) state.doll = new RagdollDollEntity(minecraft.level);
         RagdollDollEntity doll = state.doll;
         doll.setSkinProfile(state.profile);
         doll.setItemSlot(EquipmentSlot.MAINHAND, state.mainHand);
         doll.setItemSlot(EquipmentSlot.OFFHAND, state.offHand);
         doll.setItemSlot(EquipmentSlot.HEAD, state.head);
         doll.setItemSlot(EquipmentSlot.CHEST, state.chest);
         doll.setItemSlot(EquipmentSlot.LEGS, state.legs);
         doll.setItemSlot(EquipmentSlot.FEET, state.feet);
         state.source = doll;
      }
      state.hurt = state.source != null && state.source.hurtTime > 0;

      if (state.source != null) {
         LivingEntityRenderer renderer = (LivingEntityRenderer) rendererFor(state.source);
         renderer.extractRenderState(state.source, state.avatar, partialTick);
         // The source player is intentionally invisible while seated; render the ragdoll as a visible avatar.
         state.avatar.isInvisible = false;
         state.avatar.isInvisibleToPlayer = false;
         state.avatar.bodyPart = state.bodyPart;
         state.avatar.skin = state.skin;
         state.avatar.headEquipment = state.head;
         state.avatar.chestEquipment = state.chest;
         state.avatar.legsEquipment = state.legs;
         state.avatar.feetEquipment = state.feet;
         HumanoidArm mainArm = state.avatar.mainArm;
         state.avatar.rightHandItemStack = mainArm == HumanoidArm.RIGHT ? state.mainHand : state.offHand;
         state.avatar.leftHandItemStack = mainArm == HumanoidArm.LEFT ? state.mainHand : state.offHand;
         this.itemModelResolver.updateForLiving(
               state.avatar.rightHandItemState,
               mainArm == HumanoidArm.RIGHT ? state.mainHand : state.offHand,
               ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
               state.source);
         this.itemModelResolver.updateForLiving(
               state.avatar.leftHandItemState,
               mainArm == HumanoidArm.LEFT ? state.mainHand : state.offHand,
               ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
               state.source);
      }
   }

   @Override
   public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Minecraft minecraft = Minecraft.getInstance();
      if (state.bodyPart == BodyPart.HEAD
            && minecraft.options.getCameraType() == CameraType.FIRST_PERSON
            && minecraft.player != null
            && minecraft.player.getVehicle() instanceof RagdollSeatEntity
            && minecraft.player.getUUID().equals(state.profile.id())) {
         return;
      }

      state.avatar.bodyPart = state.bodyPart;
      this.model = state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.defaultModel;
      try (EmfVanillaModelCompat.Session ignored = EmfVanillaModelCompat.enter(this.model)) {
         activeModel = this.model;
         activeBodyPart = state.bodyPart;
         poseStack.pushPose();
         try {
            this.model.setupAnim(state.avatar);
            ModelPart selectedPart = visiblePart(this.model, state.bodyPart);
            positionPart(state.bodyPart, selectedPart, poseStack);
            int overlay = state.hurt ? OverlayTexture.pack(0, 3) : OverlayTexture.NO_OVERLAY;
            var renderType = RenderTypes.entityTranslucent(state.skin.body().texturePath());
            collector.submitModel(this.model, state.avatar, poseStack, renderType, state.lightCoords, overlay, -1, null, state.avatar.outlineColor);

            if (state.source != null) {
               LivingEntityRenderer<?, ?, ?> sourceRenderer = rendererFor(state.source);
               LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) sourceRenderer;
               for (Object candidate : accessor.getLayers()) {
                  @SuppressWarnings("unchecked")
                  RenderLayer<AvatarRenderState, PlayerModel> layer = (RenderLayer<AvatarRenderState, PlayerModel>) candidate;
                  if (layer instanceof net.minecraft.client.renderer.entity.layers.CapeLayer && state.bodyPart != BodyPart.TORSO) continue;
                  if (layer instanceof net.minecraft.client.renderer.entity.layers.WingsLayer<?, ?> && state.bodyPart != BodyPart.TORSO) continue;
                  if (layer instanceof net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer<?, ?>) {
                     if (state.bodyPart != BodyPart.LEFT_ARM && state.bodyPart != BodyPart.RIGHT_ARM) continue;
                     if (state.bodyPart == BodyPart.RIGHT_ARM) state.avatar.leftHandItemState.clear();
                     else state.avatar.rightHandItemState.clear();
                  }
                  layer.submit(poseStack, collector, state.lightCoords, state.avatar, state.avatar.yRot, state.avatar.xRot);
               }
            }

            if (state.breakProgress != null) {
               collector.order(1).submitCrumblingOverlay(
                     this.model, state.avatar, poseStack, renderType, state.lightCoords, overlay, -1, state.breakProgress);
            }
         } finally {
            poseStack.popPose();
            activeModel = null;
            activeBodyPart = null;
         }
      }
   }

   @Override
   public PlayerModel getModel() {
      return this.model;
   }

   public Identifier getTextureLocation(AvatarRenderState state) {
      return state.skin.body().texturePath();
   }

   private PlayerSkin resolveSkin(GameProfile profile) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.getSkinManager() == null
            ? DefaultPlayerSkin.get(profile)
            : minecraft.getSkinManager().createLookup(profile, false).get();
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static LivingEntityRenderer<?, ?, ?> rendererFor(LivingEntity entity) {
      return (LivingEntityRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
   }

   private static ModelPart visiblePart(PlayerModel model, BodyPart part) {
      return switch (part) {
         case HEAD -> model.head;
         case TORSO -> model.body;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_LEG -> model.leftLeg;
         case RIGHT_LEG -> model.rightLeg;
      };
   }

   private static void positionPart(BodyPart part, ModelPart modelPart, PoseStack poseStack) {
      poseStack.translate(0.5F, 0.5F, 0.5F);
      poseStack.rotateDegrees(Axis.YP, 180.0F);
      float scale = part.renderScale();
      poseStack.scale(-scale, -scale, scale);
      poseStack.translate(-modelPart.x / 16.0F, -modelPart.y / 16.0F, -modelPart.z / 16.0F);
      switch (part) {
         case HEAD -> nudge(poseStack, 0.0F, 4.0F, 0.0F);
         case TORSO -> nudge(poseStack, 0.0F, -6.5F, 0.0F);
         case LEFT_ARM -> nudge(poseStack, -2.0F, -4.0F, 0.0F);
         case RIGHT_ARM -> nudge(poseStack, 0.0F, -4.0F, 0.0F);
         case LEFT_LEG, RIGHT_LEG -> nudge(poseStack, -1.0F, -6.0F, 0.0F);
      }
   }

   private static void nudge(PoseStack poseStack, float x, float y, float z) {
      poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
   }

   public static final class RenderState extends BlockEntityRenderState {
      public BodyPart bodyPart = BodyPart.TORSO;
      public GameProfile profile = new GameProfile(new UUID(0L, 0L), "Player");
      public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
      public AvatarState avatar = new AvatarState();
      public boolean corpse;
      public boolean hurt;
      public ItemStack head = ItemStack.EMPTY;
      public ItemStack chest = ItemStack.EMPTY;
      public ItemStack legs = ItemStack.EMPTY;
      public ItemStack feet = ItemStack.EMPTY;
      public ItemStack mainHand = ItemStack.EMPTY;
      public ItemStack offHand = ItemStack.EMPTY;
      public @Nullable LivingEntity source;
      public @Nullable RagdollDollEntity doll;
   }

   public static final class AvatarState extends AvatarRenderState {
      private BodyPart bodyPart = BodyPart.TORSO;
   }

   private static final class SplitPlayerModel extends PlayerModel {
      private SplitPlayerModel(ModelPart root, boolean slim) {
         super(root, slim);
      }

      @Override
      public void setupAnim(AvatarRenderState state) {
         super.setupAnim(state);
         if (state instanceof AvatarState ragdollState) {
            BodyPart part = ragdollState.bodyPart;
            this.head.visible = part == BodyPart.HEAD;
            this.body.visible = part == BodyPart.TORSO;
            this.leftArm.visible = part == BodyPart.LEFT_ARM;
            this.rightArm.visible = part == BodyPart.RIGHT_ARM;
            this.leftLeg.visible = part == BodyPart.LEFT_LEG;
            this.rightLeg.visible = part == BodyPart.RIGHT_LEG;
            this.hat.visible = part == BodyPart.HEAD && state.showHat;
            this.jacket.visible = part == BodyPart.TORSO && state.showJacket;
            this.leftSleeve.visible = part == BodyPart.LEFT_ARM && state.showLeftSleeve;
            this.rightSleeve.visible = part == BodyPart.RIGHT_ARM && state.showRightSleeve;
            this.leftPants.visible = part == BodyPart.LEFT_LEG && state.showLeftPants;
            this.rightPants.visible = part == BodyPart.RIGHT_LEG && state.showRightPants;
         }
      }
   }
}
