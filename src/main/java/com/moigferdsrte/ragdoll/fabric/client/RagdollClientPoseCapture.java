package com.moigferdsrte.ragdoll.fabric.client;

import com.moigferdsrte.ragdoll.api.RagdollLimbConfig;
import com.moigferdsrte.ragdoll.api.RagdollLimbOptions;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.mob.client.EmfVanillaModelCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

public final class RagdollClientPoseCapture {

   private RagdollClientPoseCapture() {
   }

   public static RagdollLimbOptions capture() {
      Minecraft minecraft = Minecraft.getInstance();
      if (!(minecraft.player instanceof AbstractClientPlayer player)) {
         return RagdollLimbOptions.defaults();
      }

      if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof AvatarRenderer<?> playerRenderer)) {
         return RagdollLimbOptions.defaults();
      }

      float partialTick = 1.0F;
      AvatarRenderState state = new AvatarRenderState();
      extractState(playerRenderer, player, state, partialTick);
      PlayerModel model = playerRenderer.getModel();
      try (EmfVanillaModelCompat.Session ignored = EmfVanillaModelCompat.enter(model)) {
         model.setupAnim(state);

         return RagdollLimbOptions.builder()
            .limb(BodyPart.HEAD, pose(model.head))
            .limb(BodyPart.TORSO, pose(model.body))
            .limb(BodyPart.LEFT_ARM, pose(model.leftArm))
            .limb(BodyPart.RIGHT_ARM, pose(model.rightArm))
            .limb(BodyPart.LEFT_LEG, pose(model.leftLeg))
            .limb(BodyPart.RIGHT_LEG, pose(model.rightLeg))
            .build();
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void extractState(AvatarRenderer renderer, AbstractClientPlayer player, AvatarRenderState state, float partialTick) {
      renderer.extractRenderState(player, state, partialTick);
   }

   private static RagdollLimbConfig pose(ModelPart part) {
      return RagdollLimbConfig.builder()
         .initialRotation(degrees(part.xRot), -degrees(part.yRot), degrees(part.zRot))
         .build();
   }

   private static double degrees(float radians) {
      return radians * Mth.RAD_TO_DEG;
   }
}
