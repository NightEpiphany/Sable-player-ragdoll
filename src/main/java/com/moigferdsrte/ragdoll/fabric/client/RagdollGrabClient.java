package com.moigferdsrte.ragdoll.fabric.client;

import com.moigferdsrte.ragdoll.RagdollCollisionRules;
import com.moigferdsrte.ragdoll.config.RagdollSettings;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.fabric.network.FabricClientNetwork;
import com.moigferdsrte.ragdoll.fabric.network.RagdollGrabPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class RagdollGrabClient {
    private static BlockPos activePos;
    private static int collisionGraceTicks;

    private RagdollGrabClient() {
    }

    public static void init() {
        RagdollCollisionRules.setLocalGrabActive(RagdollGrabClient::isGrabbing);
    }

    public static void tick(Minecraft minecraft) {
        if (activePos == null && collisionGraceTicks > 0) collisionGraceTicks--;
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isSpectator()
                || !RagdollSettings.grabEnabled() || player.isPassenger()
                || !minecraft.options.keyUse.isDown() || !player.getMainHandItem().isEmpty()) {
            stopGrab();
            return;
        }
        if (activePos != null) {
            player.setSprinting(false);
            return;
        }
        BlockPos target = targetedPart(minecraft);
        if (target != null) {
            activePos = target;
            FabricClientNetwork.sendToServer(new RagdollGrabPacket(target, false));
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
    }

    private static void stopGrab() {
        if (activePos != null) {
            FabricClientNetwork.sendToServer(new RagdollGrabPacket(activePos, true));
            activePos = null;
            collisionGraceTicks = 20;
        }
    }

    public static void clearActive() {
        activePos = null;
        collisionGraceTicks = 20;
    }

    public static boolean isGrabbing() {
        return activePos != null || collisionGraceTicks > 0;
    }

    private static BlockPos targetedPart(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() == HitResult.Type.MISS) return null;
        BlockEntity blockEntity = minecraft.level.getBlockEntity(hit.getBlockPos());
        return blockEntity instanceof RagdollPartBlockEntity || blockEntity instanceof MobRagdollPartBlockEntity
                ? hit.getBlockPos() : null;
    }
}
