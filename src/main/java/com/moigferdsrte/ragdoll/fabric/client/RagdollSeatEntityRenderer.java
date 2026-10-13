package com.moigferdsrte.ragdoll.fabric.client;

import com.moigferdsrte.ragdoll.entity.RagdollSeatEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class RagdollSeatEntityRenderer extends EntityRenderer<RagdollSeatEntity, EntityRenderState> {
   public RagdollSeatEntityRenderer(Context context) {
      super(context);
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }

   @Override
   public boolean shouldRender(RagdollSeatEntity entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
      return false;
   }

}
