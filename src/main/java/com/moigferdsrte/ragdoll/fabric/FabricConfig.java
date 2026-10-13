package com.moigferdsrte.ragdoll.fabric;

import com.google.gson.JsonObject;
import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.config.RagdollSettings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sable_player_ragdoll.json5");
    private static long lastModified;
    private static long nextCheckTick;

    private FabricConfig() {
    }

    public static void load() {
        try {
            if (Files.notExists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, defaultConfig(), StandardCharsets.UTF_8);
            }
            lastModified = Files.getLastModifiedTime(FILE).toMillis();
            JsonObject root = Json5Parser.parse(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            boolean enabled = bool(root, "enabled", true);
            boolean grabEnabled = bool(root, "grabEnabled", true);
            double minVelocityDelta = number(root, "minVelocityDelta", 15.0);
            double maxVelocityDelta = number(root, "maxVelocityDelta", 120.0);
            int cooldownTicks = integer(root, "cooldownTicks", 60);
            double maxFlingSpeed = number(root, "maxFlingSpeed", 128.0);
            double ragdollMaxLaunchSpeed = number(root, "ragdollMaxLaunchSpeed", 128.0);
            boolean partSelfCollision = bool(root, "partSelfCollision", true);
            boolean grabBreakEnabled = bool(root, "grabBreakEnabled", true);
            double grabBreakDistance = number(root, "grabBreakDistance", 0.275);
            boolean affectCreative = bool(root, "affectCreative", true);
            boolean autoSeatOnTrigger = bool(root, "autoSeatOnTrigger", true);
            boolean allowManualTrigger = bool(root, "allowManualTrigger", true);
            int minDismountTicks = integer(root, "minDismountTicks", 60);
            boolean impactDamageEnabled = bool(root, "impactDamageEnabled", true);
            double impactFeedbackThreshold = number(root, "impactFeedbackThreshold", 4.0);
            double impactDamageThreshold = number(root, "impactDamageThreshold", 12.0);
            double impactDamageMultiplier = number(root, "impactDamageMultiplier", 0.75);
            double impactDamageMax = number(root, "impactDamageMax", 20.0);
            int impactDamageCooldownTicks = integer(root, "impactDamageCooldownTicks", 10);
            boolean expireAfterDuration = bool(root, "expireAfterDuration", false);
            int ragdollDurationTicks = integer(root, "ragdollDurationTicks", 40);
            boolean expireWhenSlow = bool(root, "expireWhenSlow", false);
            double releaseSpeedThreshold = number(root, "releaseSpeedThreshold", 0.1);
            boolean expireAfterSafetyTimeout = bool(root, "expireAfterSafetyTimeout", false);
            int step1BodyLifetimeTicks = integer(root, "step1BodyLifetimeTicks", 200);
            boolean debugLogging = bool(root, "debugLogging", true);
            if (maxVelocityDelta < minVelocityDelta) throw new IllegalArgumentException("maxVelocityDelta must be >= minVelocityDelta");
            RagdollSettings.setEnabled(enabled);
            RagdollSettings.setGrabEnabled(grabEnabled);
            RagdollSettings.setMinVelocityDelta(minVelocityDelta);
            RagdollSettings.setMaxVelocityDelta(maxVelocityDelta);
            RagdollSettings.setCooldownTicks(cooldownTicks);
            RagdollSettings.setMaxFlingSpeed(maxFlingSpeed);
            RagdollSettings.setRagdollMaxLaunchSpeed(ragdollMaxLaunchSpeed);
            RagdollSettings.setPartSelfCollision(partSelfCollision);
            RagdollSettings.setGrabBreakEnabled(grabBreakEnabled);
            RagdollSettings.setGrabBreakDistance(grabBreakDistance);
            RagdollSettings.setAffectCreative(affectCreative);
            RagdollSettings.setAutoSeatOnTrigger(autoSeatOnTrigger);
            RagdollSettings.setAllowManualTrigger(allowManualTrigger);
            RagdollSettings.setMinDismountTicks(minDismountTicks);
            RagdollSettings.setImpactDamageEnabled(impactDamageEnabled);
            RagdollSettings.setImpactFeedbackThreshold(impactFeedbackThreshold);
            RagdollSettings.setImpactDamageThreshold(impactDamageThreshold);
            RagdollSettings.setImpactDamageMultiplier(impactDamageMultiplier);
            RagdollSettings.setImpactDamageMax(impactDamageMax);
            RagdollSettings.setImpactDamageCooldownTicks(impactDamageCooldownTicks);
            RagdollSettings.setExpireAfterDuration(expireAfterDuration);
            RagdollSettings.setRagdollDurationTicks(ragdollDurationTicks);
            RagdollSettings.setExpireWhenSlow(expireWhenSlow);
            RagdollSettings.setReleaseSpeedThreshold(releaseSpeedThreshold);
            RagdollSettings.setExpireAfterSafetyTimeout(expireAfterSafetyTimeout);
            RagdollSettings.setStep1BodyLifetimeTicks(step1BodyLifetimeTicks);
            RagdollSettings.setDebugLogging(debugLogging);
            lastModified = Files.getLastModifiedTime(FILE).toMillis();
            SablePlayerRagdoll.LOGGER.info("[sable_player_ragdoll] loaded Fabric JSON5 config");
        } catch (Exception exception) {
            SablePlayerRagdoll.LOGGER.error("[sable_player_ragdoll] failed to load config {}; keeping current values", FILE, exception);
        }
    }

    public static Path path() { return FILE; }
    public static void resetPolling() { nextCheckTick = 0; }

    public static void tick(long serverTick) {
        if (serverTick < nextCheckTick) return;
        nextCheckTick = serverTick + 20;
        try {
            long modified = Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : 0L;
            if (modified != 0L && modified != lastModified) load();
        } catch (IOException exception) {
            SablePlayerRagdoll.LOGGER.warn("[sable_player_ragdoll] could not check config timestamp", exception);
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return com.moigferdsrte.ragdoll.fabric.util.JsonConfigValues.bool(root, key, fallback);
    }

    private static int integer(JsonObject root, String key, int fallback) {
        return com.moigferdsrte.ragdoll.fabric.util.JsonConfigValues.integer(root, key, fallback);
    }

    private static double number(JsonObject root, String key, double fallback) {
        return com.moigferdsrte.ragdoll.fabric.util.JsonConfigValues.number(root, key, fallback, 0.0, Double.MAX_VALUE);
    }

    private static String defaultConfig() {
        return "{\n"
                + "  // Values are reloaded while the server is running.\n"
                + "  \"enabled\": true,\n"
                + "  \"grabEnabled\": true,\n"
                + "  \"minVelocityDelta\": 15.0,\n"
                + "  \"maxVelocityDelta\": 120.0,\n"
                + "  \"cooldownTicks\": 60,\n"
                + "  \"maxFlingSpeed\": 128.0,\n"
                + "  \"ragdollMaxLaunchSpeed\": 128.0,\n"
                + "  \"partSelfCollision\": true,\n"
                + "  \"grabBreakEnabled\": true,\n"
                + "  \"grabBreakDistance\": 0.275,\n"
                + "  \"affectCreative\": true,\n"
                + "  \"autoSeatOnTrigger\": true,\n"
                + "  \"allowManualTrigger\": true,\n"
                + "  \"minDismountTicks\": 60,\n"
                + "  \"impactDamageEnabled\": true,\n"
                + "  \"impactFeedbackThreshold\": 4.0,\n"
                + "  \"impactDamageThreshold\": 12.0,\n"
                + "  \"impactDamageMultiplier\": 0.75,\n"
                + "  \"impactDamageMax\": 20.0,\n"
                + "  \"impactDamageCooldownTicks\": 10,\n"
                + "  \"expireAfterDuration\": false,\n"
                + "  \"ragdollDurationTicks\": 40,\n"
                + "  \"expireWhenSlow\": false,\n"
                + "  \"releaseSpeedThreshold\": 0.1,\n"
                + "  \"expireAfterSafetyTimeout\": false,\n"
                + "  \"step1BodyLifetimeTicks\": 200,\n"
                + "  \"debugLogging\": true\n"
                + "}\n";
    }

}
