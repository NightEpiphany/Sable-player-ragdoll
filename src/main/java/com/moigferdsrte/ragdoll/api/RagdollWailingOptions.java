package com.moigferdsrte.ragdoll.api;

public record RagdollWailingOptions(double stiffness, int durationTicks, int intervalTicks, int startDelayTicks) {

   public static RagdollWailingOptions defaults() {
      return builder().build();
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private double stiffness = 65.0;
      private int durationTicks = 100;
      private int intervalTicks = 5;
      private int startDelayTicks = 3;

      private Builder() {
      }

      public Builder stiffness(double stiffness) {
         this.stiffness = Math.max(0.0, stiffness);
         return this;
      }

      public Builder durationTicks(int durationTicks) {
         this.durationTicks = Math.max(1, durationTicks);
         return this;
      }

      public Builder intervalTicks(int intervalTicks) {
         this.intervalTicks = Math.max(1, intervalTicks);
         return this;
      }

      public Builder startDelayTicks(int startDelayTicks) {
         this.startDelayTicks = Math.max(0, startDelayTicks);
         return this;
      }

      public RagdollWailingOptions build() {
         return new RagdollWailingOptions(stiffness, durationTicks, intervalTicks, startDelayTicks);
      }
   }
}
