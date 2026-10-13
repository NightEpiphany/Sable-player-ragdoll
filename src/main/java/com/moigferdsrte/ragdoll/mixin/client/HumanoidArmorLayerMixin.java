package com.moigferdsrte.ragdoll.mixin.client;

import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import com.moigferdsrte.ragdoll.fabric.client.RagdollPartBlockEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @SuppressWarnings("rawtypes")
    @WrapMethod(method = "setPartVisibility")
    private void onSetPartVisibility(HumanoidModel model, EquipmentSlot slot, Operation<Void> original) {
        original.call(model, slot);
        BodyPart bodyPart = RagdollPartBlockEntityRenderer.activeBodyPart();
        if (bodyPart == null) return;
        switch (bodyPart) {
            case TORSO -> {
                model.leftArm.visible  = false;
                model.rightArm.visible = false;
                model.leftLeg.visible  = false;
                model.rightLeg.visible = false;
            }
            case LEFT_ARM -> {
                model.body.visible     = false;
                model.rightArm.visible = false;
            }
            case RIGHT_ARM -> {
                model.body.visible    = false;
                model.leftArm.visible = false;
            }
            case LEFT_LEG -> {
                model.body.visible    = false;
                model.rightLeg.visible = false;
            }
            case RIGHT_LEG -> {
                model.body.visible    = false;
                model.leftLeg.visible = false;
            }
            default -> {}
        }
    }
}
