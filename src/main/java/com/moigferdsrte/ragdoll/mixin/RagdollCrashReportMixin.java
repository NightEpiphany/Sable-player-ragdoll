package com.moigferdsrte.ragdoll.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.moigferdsrte.ragdoll.fabric.util.RagdollDiagnostics;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CrashReport.class)
abstract class RagdollCrashReportMixin {
    @Unique private boolean ragdoll$diagnosticsAdded;

    @WrapMethod(method = "getFriendlyReport(Lnet/minecraft/ReportType;Ljava/util/List;)Ljava/lang/String;")
    private String ragdoll$diagnostics(ReportType type, List<String> extra, Operation<String> original) {
        if (!ragdoll$diagnosticsAdded) {
            ragdoll$diagnosticsAdded = true;
            RagdollDiagnostics.appendTo((CrashReport) (Object) this);
        }
        return original.call(type, extra);
    }
}
