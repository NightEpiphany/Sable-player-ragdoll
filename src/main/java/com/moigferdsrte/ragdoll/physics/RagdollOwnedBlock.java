package com.moigferdsrte.ragdoll.physics;

import java.util.UUID;

public interface RagdollOwnedBlock {
    RagdollBlockIdentity ragdollIdentity();
    void setRagdollIdentity(UUID owner, UUID limb, String kind);
    void markSevered();
}
