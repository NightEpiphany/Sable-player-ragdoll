package com.moigferdsrte.ragdoll.api;

import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity.BodyPart;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;


public final class RagdollLimbOptions {
   private static final RagdollLimbOptions DEFAULTS = new RagdollLimbOptions(Map.of());

   private final Map<BodyPart, RagdollLimbConfig> configs;

   private RagdollLimbOptions(Map<BodyPart, RagdollLimbConfig> configs) {
      this.configs = configs;
   }

   public static RagdollLimbOptions defaults() {
      return DEFAULTS;
   }

   public static Builder builder() {
      return new Builder();
   }

   @Nullable
   public RagdollLimbConfig get(BodyPart part) {
      return this.configs.get(part);
   }

   public boolean isEmpty() {
      return this.configs.isEmpty();
   }

   public static final class Builder {
      private final Map<BodyPart, RagdollLimbConfig> configs = new EnumMap<>(BodyPart.class);

      private Builder() {
      }

      public Builder limb(BodyPart part, RagdollLimbConfig config) {
         this.configs.put(part, config);
         return this;
      }

      public Builder limb(BodyPart part, RagdollLimbConfig.Builder config) {
         return this.limb(part, config.build());
      }

      public RagdollLimbOptions build() {
         return this.configs.isEmpty() ? DEFAULTS : new RagdollLimbOptions(new EnumMap<>(this.configs));
      }
   }
}
