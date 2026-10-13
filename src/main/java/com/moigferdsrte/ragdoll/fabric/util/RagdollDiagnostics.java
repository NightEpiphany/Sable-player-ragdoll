package com.moigferdsrte.ragdoll.fabric.util;

import com.moigferdsrte.ragdoll.fabric.FabricConfig;
import com.moigferdsrte.ragdoll.config.RagdollSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.CrashReport;

public final class RagdollDiagnostics {
    private RagdollDiagnostics() {}

    public static void appendTo(CrashReport report) {
        // System details do not alter the exception's stack trace during report formatting.
        report.getSystemReport().setDetail("PlayerRagdoll Fabric", () ->
                "environment=" + FabricLoader.getInstance().getEnvironmentType().name()
                + ", configuration=" + FabricConfig.path()
                + ", enabled=" + RagdollSettings.enabled()
                + ", grabEnabled=" + RagdollSettings.grabEnabled()
                + ", Sable=" + FabricLoader.getInstance().getModContainer("sable")
                        .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("missing"));
    }
}
