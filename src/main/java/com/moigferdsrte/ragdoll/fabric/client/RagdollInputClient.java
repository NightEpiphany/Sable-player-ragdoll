package com.moigferdsrte.ragdoll.fabric.client;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import com.moigferdsrte.ragdoll.fabric.network.FabricClientNetwork;
import com.moigferdsrte.ragdoll.fabric.network.RagdollArmInputPacket;
import com.moigferdsrte.ragdoll.fabric.network.RagdollInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class RagdollInputClient {
    private static float lastStrafe;
    private static float lastForward;
    private static int keepAliveTicks;
    private static boolean lastArmActive;

    private RagdollInputClient() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isPassenger()) {
            sendMovement(0.0F, 0.0F, true);
            sendArms(false, false, false, false, Vec3.ZERO);
            return;
        }
        float strafe = axis(player.input.keyPresses.right(), player.input.keyPresses.left());
        float forward = axis(player.input.keyPresses.backward(), player.input.keyPresses.forward());
        sendMovement(strafe, forward, false);
        if (player.getVehicle() instanceof RagdollSeatEntity) {
            var look = minecraft.gameRenderer.mainCamera().forwardVector();
            sendArms(minecraft.options.keyAttack.isDown(), minecraft.options.keyUse.isDown(),
                    minecraft.options.keySprint.isDown(), minecraft.options.keyJump.isDown(),
                    new Vec3(look.x(), look.y(), look.z()));
        } else {
            sendArms(false, false, false, false, Vec3.ZERO);
        }
    }

    private static void sendMovement(float strafe, float forward, boolean forceZero) {
        boolean changed = strafe != lastStrafe || forward != lastForward;
        if (!changed && !forceZero && ++keepAliveTicks < 5) return;
        if (!changed && forceZero && lastStrafe == 0.0F && lastForward == 0.0F) return;
        keepAliveTicks = 0;
        lastStrafe = strafe;
        lastForward = forward;
        FabricClientNetwork.sendToServer(new RagdollInputPacket(strafe, forward));
    }

    private static void sendArms(boolean leftReach, boolean rightReach, boolean leftGrab, boolean rightGrab, Vec3 look) {
        boolean active = leftReach || rightReach || leftGrab || rightGrab;
        if (!active && !lastArmActive) return;
        lastArmActive = active;
        FabricClientNetwork.sendToServer(new RagdollArmInputPacket(leftReach, rightReach, leftGrab, rightGrab,
                (float) look.x, (float) look.y, (float) look.z));
    }

    private static float axis(boolean negative, boolean positive) {
        if (positive == negative) return 0.0F;
        return positive ? 1.0F : -1.0F;
    }
}
