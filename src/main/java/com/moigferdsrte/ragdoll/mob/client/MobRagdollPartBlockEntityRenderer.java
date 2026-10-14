package com.moigferdsrte.ragdoll.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.ragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MobRagdollPartBlockEntityRenderer
        implements BlockEntityRenderer<MobRagdollPartBlockEntity, MobRagdollPartRenderState> {
    private final BlockModelResolver blockModelResolver;

    public MobRagdollPartBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public MobRagdollPartRenderState createRenderState() {
        return new MobRagdollPartRenderState();
    }

    @Override
    public boolean shouldRender(MobRagdollPartBlockEntity blockEntity, Vec3 cameraPosition) {
        // Sable passes a transformed local camera position during sub-level
        // extraction, but this block entity still has global plot coordinates.
        return !blockEntity.isRemoved();
    }

    @Override
    public void extractRenderState(
            MobRagdollPartBlockEntity blockEntity,
            MobRagdollPartRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.extract(blockEntity);
        if (state.renderAnchor && state.quads.isEmpty()) {
            this.blockModelResolver.update(
                    state.fallbackModel,
                    Blocks.STAINED_GLASS.lightBlue().defaultBlockState(),
                    BlockDisplayContext.create());
        }
    }

    @Override
    public void submit(
            MobRagdollPartRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (!state.renderAnchor) {
            return;
        }

        if (!state.quads.isEmpty()) {
            MobRagdollQuadRenderer.submit(state, poseStack, submitNodeCollector, state.lightCoords);
            return;
        }

        poseStack.pushPose();
        poseStack.translate((1.0F - state.xSize) * 0.5F, (1.0F - state.ySize) * 0.5F, (1.0F - state.zSize) * 0.5F);
        poseStack.scale(state.xSize, state.ySize, state.zSize);
        state.fallbackModel.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
