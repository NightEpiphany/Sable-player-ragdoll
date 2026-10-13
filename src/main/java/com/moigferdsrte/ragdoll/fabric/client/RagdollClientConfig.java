package com.moigferdsrte.ragdoll.fabric.client;

import com.google.gson.JsonObject;
import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.fabric.Json5Parser;
import com.moigferdsrte.ragdoll.fabric.util.JsonConfigValues;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class RagdollClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sable_player_ragdoll-client.json5");
    private static boolean firstPerson;
    private static boolean controlsHint = true;
    private static double cameraDistance = 4.0;
    private static long modified;
    private static int checkTicks;
    private RagdollClientConfig() {}
    public static void load() {
        try {
            if (Files.notExists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, "{\n  // Changes reload while playing.\n  useFirstPersonCamera: false,\n  subLevelCameraDistance: 4.0,\n  showControlsHint: true,\n}\n");
            }
            modified = Files.getLastModifiedTime(FILE).toMillis();
            JsonObject root = Json5Parser.parse(Files.readString(FILE)).getAsJsonObject();
            boolean newFirstPerson = JsonConfigValues.bool(root, "useFirstPersonCamera", false);
            boolean newHint = JsonConfigValues.bool(root, "showControlsHint", true);
            double newDistance = JsonConfigValues.number(root, "subLevelCameraDistance", 4.0, 0.5, 16.0);
            firstPerson = newFirstPerson;
            controlsHint = newHint;
            cameraDistance = newDistance;
            SablePlayerRagdoll.LOGGER.info("Loaded client ragdoll config {}", FILE);
        } catch (Exception error) { SablePlayerRagdoll.LOGGER.error("Failed to load {}; keeping current config", FILE, error); }
    }
    public static void tick() {
        if (++checkTicks < 20) return;
        checkTicks = 0;
        try { if (Files.exists(FILE) && Files.getLastModifiedTime(FILE).toMillis() != modified) load(); }
        catch (Exception error) { SablePlayerRagdoll.LOGGER.warn("Could not check client config {}", FILE, error); }
    }
    public static boolean useFirstPersonCamera() { return firstPerson; }
    public static double subLevelCameraDistance() { return cameraDistance; }
    public static boolean showControlsHint() { return controlsHint; }
}
