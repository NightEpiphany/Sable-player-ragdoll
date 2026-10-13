package com.moigferdsrte.ragdoll.mob;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class MobRagdollBlacklistSavedData extends SavedData {
   private static final String DATA_NAME = "sable_player_ragdoll_mob_ragdoll_blacklist";
   private static final Codec<MobRagdollBlacklistSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.listOf().optionalFieldOf("Entities", List.of())
         .forGetter(data -> data.entities.stream().sorted().toList()),
      Codec.STRING.listOf().optionalFieldOf("Namespaces", List.of())
         .forGetter(data -> data.namespaces.stream().sorted().toList())
   ).apply(instance, MobRagdollBlacklistSavedData::new));
   private static final SavedDataType<MobRagdollBlacklistSavedData> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace(DATA_NAME), MobRagdollBlacklistSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE
   );

   private final Set<String> entities = new LinkedHashSet<>();
   private final Set<String> namespaces = new LinkedHashSet<>();

   public MobRagdollBlacklistSavedData() {
   }

   private MobRagdollBlacklistSavedData(Collection<String> entities, Collection<String> namespaces) {
      addLoadedStrings(entities, this.entities);
      addLoadedStrings(namespaces, this.namespaces);
   }

   public static MobRagdollBlacklistSavedData get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(TYPE);
   }

   public boolean isBlacklisted(Identifier entityId) {
      return entityId != null && (this.entities.contains(entityId.toString()) || this.namespaces.contains(entityId.getNamespace()));
   }

   public boolean addEntity(Identifier entityId) {
      if (entityId == null) {
         return false;
      }
      boolean changed = this.entities.add(entityId.toString());
      if (changed) {
         setDirty();
      }
      return changed;
   }

   public boolean removeEntity(Identifier entityId) {
      if (entityId == null) {
         return false;
      }
      boolean changed = this.entities.remove(entityId.toString());
      if (changed) {
         setDirty();
      }
      return changed;
   }

   public boolean addNamespace(String namespace) {
      String normalized = normalizeNamespace(namespace);
      if (!isValidNamespace(normalized)) {
         return false;
      }
      boolean changed = this.namespaces.add(normalized);
      if (changed) {
         setDirty();
      }
      return changed;
   }

   public boolean removeNamespace(String namespace) {
      String normalized = normalizeNamespace(namespace);
      if (!isValidNamespace(normalized)) {
         return false;
      }
      boolean changed = this.namespaces.remove(normalized);
      if (changed) {
         setDirty();
      }
      return changed;
   }

   public boolean clear() {
      if (this.entities.isEmpty() && this.namespaces.isEmpty()) {
         return false;
      }
      this.entities.clear();
      this.namespaces.clear();
      setDirty();
      return true;
   }

   public Set<String> entities() {
      return Set.copyOf(this.entities);
   }

   public Set<String> namespaces() {
      return Set.copyOf(this.namespaces);
   }

   public static boolean isValidNamespace(String namespace) {
      if (namespace == null || namespace.isBlank()) {
         return false;
      }
      for (int i = 0; i < namespace.length(); i++) {
         char c = namespace.charAt(i);
         if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
            continue;
         }
         return false;
      }
      return true;
   }

   public static String normalizeNamespace(String namespace) {
      return namespace == null ? "" : namespace.trim().toLowerCase(Locale.ROOT);
   }

   private static void addLoadedStrings(Collection<String> values, Set<String> target) {
      for (String serializedValue : values) {
         String value = serializedValue.trim();
         if (!value.isEmpty()) {
            target.add(value);
         }
      }
   }
}
