package com.moigferdsrte.ragdoll.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.fabric.client.RagdollPartBlockEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
   @WrapMethod(method = "submit")
   private void ragdoll$limitEquipmentToCurrentPart(
         PoseStack poseStack,
         SubmitNodeCollector collector,
         int lightCoords,
         HumanoidRenderState state,
         float yRot,
         float xRot,
         Operation<Void> original
   ) {
      BodyPart part = RagdollPartBlockEntityRenderer.activeBodyPart();
      if (part == null) {
         original.call(poseStack, collector, lightCoords, state, yRot, xRot);
         return;
      }

      ItemStack head = state.headEquipment;
      ItemStack chest = state.chestEquipment;
      ItemStack legs = state.legsEquipment;
      ItemStack feet = state.feetEquipment;
      state.headEquipment = part == BodyPart.HEAD ? head : ItemStack.EMPTY;
      state.chestEquipment = part == BodyPart.TORSO || part == BodyPart.LEFT_ARM || part == BodyPart.RIGHT_ARM ? chest : ItemStack.EMPTY;
      state.legsEquipment = part == BodyPart.TORSO || part == BodyPart.LEFT_LEG || part == BodyPart.RIGHT_LEG ? legs : ItemStack.EMPTY;
      state.feetEquipment = part == BodyPart.LEFT_LEG || part == BodyPart.RIGHT_LEG ? feet : ItemStack.EMPTY;
      try {
         original.call(poseStack, collector, lightCoords, state, yRot, xRot);
      } finally {
         state.headEquipment = head;
         state.chestEquipment = chest;
         state.legsEquipment = legs;
         state.feetEquipment = feet;
      }
   }
}
