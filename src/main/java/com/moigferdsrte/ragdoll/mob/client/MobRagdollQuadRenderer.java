package com.moigferdsrte.ragdoll.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

final class MobRagdollQuadRenderer {
    private MobRagdollQuadRenderer() {
    }

    static void submit(
            MobRagdollPartRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight
    ) {
        List<MobRagdollPartBlockEntity.Quad> quads = state.quads;
        Identifier texture = state.texture;
        float qx = state.renderQx;
        float qy = state.renderQy;
        float qz = state.renderQz;
        float qw = state.renderQw;
        Bounds bounds = bounds(quads);
        float centerX = (bounds.minX() + bounds.maxX()) * 0.5F;
        float centerY = (bounds.minY() + bounds.maxY()) * 0.5F;
        float centerZ = (bounds.minZ() + bounds.maxZ()) * 0.5F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Matrix4f().rotation(renderCorrectionBlockSpace(qx, qy, qz, qw)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, vertices) -> {
            for (MobRagdollPartBlockEntity.Quad quad : quads) {
                for (MobRagdollPartBlockEntity.Vertex vertex : quad.vertices()) {
                    float x = 0.5F - (vertex.x() - centerX) / 16.0F;
                    float y = 0.5F - (vertex.y() - centerY) / 16.0F;
                    float z = 0.5F + (vertex.z() - centerZ) / 16.0F;
                    vertices.addVertex(pose, x, y, z)
                            .setColor(-1)
                            .setUv(vertex.u(), vertex.v())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(packedLight)
                            .setNormal(pose, quad.normalX(), -quad.normalY(), quad.normalZ());
                }
            }
        });
        poseStack.popPose();
    }

    private static Bounds bounds(List<MobRagdollPartBlockEntity.Quad> quads) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (MobRagdollPartBlockEntity.Quad quad : quads) {
            for (MobRagdollPartBlockEntity.Vertex vertex : quad.vertices()) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
                maxZ = Math.max(maxZ, vertex.z());
            }
        }
        if (!Float.isFinite(minX)) {
            return new Bounds(-4.0F, -4.0F, -4.0F, 4.0F, 4.0F, 4.0F);
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Quaternionf renderCorrectionBlockSpace(float qx, float qy, float qz, float qw) {
        Quaternionf initial = new Quaternionf(
                -qx,
                -qy,
                qz,
                qw);
        if (!Float.isFinite(initial.x) || !Float.isFinite(initial.y)
                || !Float.isFinite(initial.z) || !Float.isFinite(initial.w)
                || initial.lengthSquared() < 1.0E-6F) {
            return new Quaternionf();
        }
        return initial.normalize().invert();
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }
}
