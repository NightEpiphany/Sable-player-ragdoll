package com.moigferdsrte.ragdoll.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.entity.RagdollDollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RagdollDollEntityRenderer
      extends LivingEntityRenderer<RagdollDollEntity, AvatarRenderState, PlayerModel> {
   private final SplitPlayerModel wideModel;
   private final SplitPlayerModel slimModel;

   public RagdollDollEntityRenderer(Context context) {
      super(context, new SplitPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.wideModel = (SplitPlayerModel) this.model;
      this.slimModel = new SplitPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

      this.addLayer(new ModelTypeArmorLayer(this, context, false, PlayerModelType.WIDE));
      this.addLayer(new ModelTypeArmorLayer(this, context, true, PlayerModelType.SLIM));
      this.addLayer(new PlayerItemInHandLayer<>(this));
      this.addLayer(new ArrowLayer<>(this, context));
      this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
      this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
      this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
      this.addLayer(new ParrotOnShoulderLayer(this, context.getModelSet()));
      this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
      this.addLayer(new BeeStingerLayer<>(this, context));
   }

   @Override
   public RenderState createRenderState() {
      return new RenderState();
   }

   @Override
   public void extractRenderState(RagdollDollEntity entity, AvatarRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      net.minecraft.client.renderer.entity.HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
      if (state instanceof RenderState ragdollState) {
         ragdollState.bodyPart = entity.getBodyPart();
      }
      state.skin = skin(entity);
      state.isSpectator = false;
      state.arrowCount = entity.getArrowCount();
      state.stingerCount = entity.getStingerCount();
      state.showHat = true;
      state.showJacket = true;
      state.showLeftPants = true;
      state.showRightPants = true;
      state.showLeftSleeve = true;
      state.showRightSleeve = true;
      state.showCape = state.skin.cape() != null;
      state.id = entity.getId();

      state.rightArmPose = armPose(entity, HumanoidArm.RIGHT);
      state.leftArmPose = armPose(entity, HumanoidArm.LEFT);
   }

   @Override
   public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      this.model = state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.wideModel;
      this.prepareModel(state);
      super.submit(state, poseStack, collector, camera);
   }

   public void submitBlockPart(
         RenderState state,
         RagdollDollEntity.BodyPart bodyPart,
         PoseStack poseStack,
         SubmitNodeCollector collector,
         int lightCoords,
         net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay breakProgress
   ) {
      state.lightCoords = lightCoords;
      this.selectBlockPart(state, bodyPart);

      int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
      var renderType = this.model.renderType(state.skin.body().texturePath());
      collector.submitModel(this.model, state, poseStack, renderType, lightCoords, overlay, -1, null, state.outlineColor);
      this.model.setupAnim(state);
      for (var layer : this.layers) {
         layer.submit(poseStack, collector, lightCoords, state, state.yRot, state.xRot);
      }
      if (breakProgress != null) {
         collector.order(1).submitCrumblingOverlay(this.model, state, poseStack, renderType, lightCoords, overlay, -1, breakProgress);
      }
   }

   private void prepareModel(AvatarRenderState state) {
      SplitPlayerModel playerModel = (SplitPlayerModel) this.model;
      playerModel.bodyPart = state instanceof RenderState ragdollState ? ragdollState.bodyPart : RagdollDollEntity.BodyPart.TORSO;
   }

   public PlayerModel currentModel() {
      return this.model;
   }

   public PlayerModel selectBlockPart(RenderState state, RagdollDollEntity.BodyPart bodyPart) {
      state.bodyPart = bodyPart;
      this.model = state.skin.model() == PlayerModelType.SLIM ? this.slimModel : this.wideModel;
      this.prepareModel(state);
      return this.model;
   }

   public void extractStoredEquipment(RenderState state, RagdollDollEntity source) {
      state.rightHandItemStack = source.getItemHeldByArm(HumanoidArm.RIGHT).copy();
      state.leftHandItemStack = source.getItemHeldByArm(HumanoidArm.LEFT).copy();
      this.itemModelResolver.updateForLiving(state.rightHandItemState, state.rightHandItemStack, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, source);
      this.itemModelResolver.updateForLiving(state.leftHandItemState, state.leftHandItemStack, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND, source);
      state.headEquipment = source.getItemBySlot(EquipmentSlot.HEAD).copy();
      state.chestEquipment = source.getItemBySlot(EquipmentSlot.CHEST).copy();
      state.legsEquipment = source.getItemBySlot(EquipmentSlot.LEGS).copy();
      state.feetEquipment = source.getItemBySlot(EquipmentSlot.FEET).copy();
      state.rightArmPose = armPose(source, HumanoidArm.RIGHT);
      state.leftArmPose = armPose(source, HumanoidArm.LEFT);
   }

   @Override
   public Identifier getTextureLocation(AvatarRenderState state) {
      return state.skin.body().texturePath();
   }

   private static PlayerSkin skin(RagdollDollEntity entity) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.getSkinManager() == null
            ? net.minecraft.client.resources.DefaultPlayerSkin.get(entity.getSkinProfile())
            : minecraft.getSkinManager().createLookup(entity.getSkinProfile(), false).get();
   }

   private static HumanoidModel.ArmPose armPose(RagdollDollEntity entity, HumanoidArm arm) {
      ItemStack item = entity.getItemHeldByArm(arm);
      if (item.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
      if (HumanoidMobRenderer.usesSpearPose(item, arm, entity)) return HumanoidModel.ArmPose.SPEAR;
      if (item.is(Items.CROSSBOW) && net.minecraft.world.item.CrossbowItem.isCharged(item)) {
         return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      }
      return HumanoidModel.ArmPose.ITEM;
   }

   public static final class RenderState extends AvatarRenderState {
      public RagdollDollEntity.BodyPart bodyPart = RagdollDollEntity.BodyPart.TORSO;
   }

   private static final class SplitPlayerModel extends PlayerModel {
      private RagdollDollEntity.BodyPart bodyPart = RagdollDollEntity.BodyPart.TORSO;

      private SplitPlayerModel(ModelPart root, boolean slim) {
         super(root, slim);
      }

      @Override
      public void setupAnim(AvatarRenderState state) {
         super.setupAnim(state);
         RagdollDollEntity.BodyPart part = state instanceof RenderState ragdollState ? ragdollState.bodyPart : this.bodyPart;
         this.head.visible = part == RagdollDollEntity.BodyPart.HEAD;
         this.body.visible = part == RagdollDollEntity.BodyPart.TORSO;
         this.leftArm.visible = part == RagdollDollEntity.BodyPart.LEFT_ARM;
         this.rightArm.visible = part == RagdollDollEntity.BodyPart.RIGHT_ARM;
         this.leftLeg.visible = part == RagdollDollEntity.BodyPart.LEFT_LEG;
         this.rightLeg.visible = part == RagdollDollEntity.BodyPart.RIGHT_LEG;
         this.hat.visible &= part == RagdollDollEntity.BodyPart.HEAD;
         this.jacket.visible &= part == RagdollDollEntity.BodyPart.TORSO;
         this.leftSleeve.visible &= part == RagdollDollEntity.BodyPart.LEFT_ARM;
         this.rightSleeve.visible &= part == RagdollDollEntity.BodyPart.RIGHT_ARM;
         this.leftPants.visible &= part == RagdollDollEntity.BodyPart.LEFT_LEG;
         this.rightPants.visible &= part == RagdollDollEntity.BodyPart.RIGHT_LEG;
      }

      @Override
      public ModelPart getRandomBodyPart(net.minecraft.util.RandomSource random) {
         return switch (this.bodyPart) {
            case HEAD -> this.head;
            case TORSO -> this.body;
            case LEFT_ARM -> this.leftArm;
            case RIGHT_ARM -> this.rightArm;
            case LEFT_LEG -> this.leftLeg;
            case RIGHT_LEG -> this.rightLeg;
         };
      }
   }

   private static final class ModelTypeArmorLayer extends HumanoidArmorLayer<AvatarRenderState, PlayerModel, SplitPlayerModel> {
      private final PlayerModelType modelType;

      private ModelTypeArmorLayer(
            net.minecraft.client.renderer.entity.RenderLayerParent<AvatarRenderState, PlayerModel> parent,
            Context context,
            boolean slim,
            PlayerModelType modelType
      ) {
         super(
               parent,
               ArmorModelSet.bake(
                     slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
                     context.getModelSet(),
                     part -> new SplitPlayerModel(part, slim)
               ),
               context.getEquipmentRenderer()
         );
         this.modelType = modelType;
      }

      @Override
      public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
         if (state.skin.model() == this.modelType) {
            super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
         }
      }
   }
}
