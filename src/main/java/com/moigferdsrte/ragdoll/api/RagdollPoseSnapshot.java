package com.moigferdsrte.ragdoll.api;

public record RagdollPoseSnapshot(RagdollLimbOptions limbs, float bodyYawDegrees) {
   public RagdollPoseSnapshot {
      limbs = limbs == null ? RagdollLimbOptions.defaults() : limbs;
   }
}
