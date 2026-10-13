package com.moigferdsrte.ragdoll.fabric;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.moigferdsrte.ragdoll.SablePlayerRagdoll;
import com.moigferdsrte.ragdoll.RagdollItemTags;
import com.moigferdsrte.ragdoll.DeletingStick;
import com.moigferdsrte.ragdoll.api.PlayerlessDespawnRule;
import com.moigferdsrte.ragdoll.api.PlayerlessRagdollSession;
import com.moigferdsrte.ragdoll.api.RagdollAPI;
import com.moigferdsrte.ragdoll.api.RagdollWailingOptions;
import com.moigferdsrte.ragdoll.block.entity.RagdollPartBlockEntity;
import com.moigferdsrte.ragdoll.mob.MobRagdollBlacklistSavedData;
import com.moigferdsrte.ragdoll.physics.RagdollRegistry;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public final class RagdollCommands {
   private RagdollCommands() {}
   public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(RagdollCommand.build());
      dispatcher.register(
         Commands.literal("sable_player_ragdoll")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("dummy")
               .executes(context -> spawnDummyRagdoll(
                  context.getSource(),
                  context.getSource().getPosition(),
                  context.getSource().getRotation().y
               ))
               .then(despawnOptions(context -> spawnDummyRagdoll(
                  context.getSource(),
                  context.getSource().getPosition(),
                  context.getSource().getRotation().y,
                  context.rule()
               )))
               .then(Commands.literal("profile")
                  .then(Commands.argument("profile", GameProfileArgument.gameProfile())
                     .executes(context -> spawnDummyRagdoll(
                        context.getSource(),
                        context.getSource().getPosition(),
                        context.getSource().getRotation().y,
                        firstProfile(GameProfileArgument.getGameProfiles(context, "profile"))
                     ))
                     .then(despawnOptions(context -> spawnDummyRagdoll(
                        context.getSource(),
                        context.getSource().getPosition(),
                        context.getSource().getRotation().y,
                        firstProfile(GameProfileArgument.getGameProfiles(context.context(), "profile")),
                        context.rule()
                     )))
                     .then(Commands.literal("elytra")
                        .executes(context -> spawnDummyRagdoll(
                           context.getSource(),
                           context.getSource().getPosition(),
                           context.getSource().getRotation().y,
                           firstProfile(GameProfileArgument.getGameProfiles(context, "profile")),
                           true
                        ))
                     )
                     .then(Commands.argument("pos", Vec3Argument.vec3())
                        .then(Commands.argument("heading", DoubleArgumentType.doubleArg())
                           .executes(context -> spawnDummyRagdoll(
                              context.getSource(),
                              Vec3Argument.getVec3(context, "pos"),
                              DoubleArgumentType.getDouble(context, "heading"),
                              firstProfile(GameProfileArgument.getGameProfiles(context, "profile"))
                           ))
                           .then(despawnOptions(context -> spawnDummyRagdoll(
                              context.getSource(),
                              Vec3Argument.getVec3(context.context(), "pos"),
                              DoubleArgumentType.getDouble(context.context(), "heading"),
                              firstProfile(GameProfileArgument.getGameProfiles(context.context(), "profile")),
                              context.rule()
                           )))
                        )
                     )
                  )
               )
               .then(Commands.argument("pos", Vec3Argument.vec3())
                  .then(Commands.argument("heading", DoubleArgumentType.doubleArg())
                     .executes(context -> spawnDummyRagdoll(
                        context.getSource(),
                        Vec3Argument.getVec3(context, "pos"),
                        DoubleArgumentType.getDouble(context, "heading")
                     ))
                     .then(despawnOptions(context -> spawnDummyRagdoll(
                        context.getSource(),
                        Vec3Argument.getVec3(context.context(), "pos"),
                        DoubleArgumentType.getDouble(context.context(), "heading"),
                        context.rule()
                     )))
                  )
               )
            )
            .then(Commands.literal("mobless")
               .then(Commands.argument("entity", IdentifierArgument.id())
                  .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                  .executes(context -> spawnMoblessRagdoll(
                     context.getSource(),
                     IdentifierArgument.getId(context, "entity"),
                     context.getSource().getPosition()
                  ))
                  .then(Commands.argument("pos", Vec3Argument.vec3())
                     .executes(context -> spawnMoblessRagdoll(
                        context.getSource(),
                        IdentifierArgument.getId(context, "entity"),
                        Vec3Argument.getVec3(context, "pos")
                     )))))
            .then(Commands.literal("wailing")
               .then(Commands.literal("start")
                  .executes(context -> startWailing(context.getSource().getPlayerOrException(), 100, 15.0, 10))
                  .then(Commands.argument("duration_ticks", IntegerArgumentType.integer(1))
                     .executes(context -> startWailing(
                        context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "duration_ticks"),
                        15.0,
                        10
                     ))
                     .then(Commands.argument("stiffness", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> startWailing(
                           context.getSource().getPlayerOrException(),
                           IntegerArgumentType.getInteger(context, "duration_ticks"),
                           DoubleArgumentType.getDouble(context, "stiffness"),
                           10
                        ))
                        .then(Commands.argument("interval_ticks", IntegerArgumentType.integer(1))
                           .executes(context -> startWailing(
                              context.getSource().getPlayerOrException(),
                              IntegerArgumentType.getInteger(context, "duration_ticks"),
                              DoubleArgumentType.getDouble(context, "stiffness"),
                              IntegerArgumentType.getInteger(context, "interval_ticks")
                           ))
                           .then(Commands.argument("targets", EntityArgument.players())
                              .executes(context -> startWailing(
                                 context.getSource(),
                                 EntityArgument.getPlayers(context, "targets"),
                                 IntegerArgumentType.getInteger(context, "duration_ticks"),
                                 DoubleArgumentType.getDouble(context, "stiffness"),
                                 IntegerArgumentType.getInteger(context, "interval_ticks")
                              ))
                           )
                        )
                     )
                  )
               )
               .then(Commands.literal("stop")
                  .executes(context -> stopWailing(context.getSource().getPlayerOrException()))
                  .then(Commands.argument("targets", EntityArgument.players())
                     .executes(context -> stopWailing(context.getSource(), EntityArgument.getPlayers(context, "targets"))))
               )
            )
            .then(Commands.literal("mob_blacklist")
               .then(Commands.literal("list")
                  .executes(context -> listMobRagdollBlacklist(context.getSource())))
               .then(Commands.literal("clear")
                  .executes(context -> clearMobRagdollBlacklist(context.getSource())))
               .then(Commands.literal("add")
                  .then(Commands.literal("entity")
                     .then(Commands.argument("entity", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                        .executes(context -> addMobRagdollBlacklistEntity(
                           context.getSource(),
                           IdentifierArgument.getId(context, "entity")
                        ))))
                  .then(Commands.literal("mod")
                     .then(Commands.argument("namespace", StringArgumentType.word())
                        .suggests(RagdollCommands::suggestEntityNamespaces)
                        .executes(context -> addMobRagdollBlacklistNamespace(
                           context.getSource(),
                           StringArgumentType.getString(context, "namespace")
                        )))))
               .then(Commands.literal("remove")
                  .then(Commands.literal("entity")
                     .then(Commands.argument("entity", IdentifierArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                        .executes(context -> removeMobRagdollBlacklistEntity(
                           context.getSource(),
                           IdentifierArgument.getId(context, "entity")
                        ))))
                  .then(Commands.literal("mod")
                     .then(Commands.argument("namespace", StringArgumentType.word())
                        .suggests(RagdollCommands::suggestEntityNamespaces)
                        .executes(context -> removeMobRagdollBlacklistNamespace(
                           context.getSource(),
                           StringArgumentType.getString(context, "namespace")
                        ))))))
            .then(Commands.literal("ragdolling_stick")
               .executes(context -> giveRagdollingStick(context.getSource())))
            .then(Commands.literal("deleting_stick")
               .executes(context -> giveDeletingStick(context.getSource())))
      );
   }

   private static int addMobRagdollBlacklistEntity(CommandSourceStack source, Identifier entityId) {
      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      if (!data.addEntity(entityId)) {
         source.sendFailure(Component.literal(entityId + " is already in the mob ragdoll blacklist."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Blacklisted mob ragdoll entity " + entityId + "."), true);
      return 1;
   }

   private static int removeMobRagdollBlacklistEntity(CommandSourceStack source, Identifier entityId) {
      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      if (!data.removeEntity(entityId)) {
         source.sendFailure(Component.literal(entityId + " was not in the mob ragdoll blacklist."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Removed mob ragdoll entity blacklist entry " + entityId + "."), true);
      return 1;
   }

   private static int addMobRagdollBlacklistNamespace(CommandSourceStack source, String namespace) {
      String normalized = MobRagdollBlacklistSavedData.normalizeNamespace(namespace);
      if (!MobRagdollBlacklistSavedData.isValidNamespace(normalized)) {
         source.sendFailure(Component.literal(namespace + " is not a valid mod id namespace."));
         return 0;
      }

      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      if (!data.addNamespace(normalized)) {
         source.sendFailure(Component.literal(normalized + " is already in the mob ragdoll mod blacklist."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Blacklisted mob ragdolls from mod namespace " + normalized + "."), true);
      return 1;
   }

   private static int removeMobRagdollBlacklistNamespace(CommandSourceStack source, String namespace) {
      String normalized = MobRagdollBlacklistSavedData.normalizeNamespace(namespace);
      if (!MobRagdollBlacklistSavedData.isValidNamespace(normalized)) {
         source.sendFailure(Component.literal(namespace + " is not a valid mod id namespace."));
         return 0;
      }

      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      if (!data.removeNamespace(normalized)) {
         source.sendFailure(Component.literal(normalized + " was not in the mob ragdoll mod blacklist."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Removed mob ragdoll mod blacklist entry " + normalized + "."), true);
      return 1;
   }

   private static int clearMobRagdollBlacklist(CommandSourceStack source) {
      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      if (!data.clear()) {
         source.sendFailure(Component.literal("The mob ragdoll blacklist is already empty."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Cleared the mob ragdoll blacklist."), true);
      return 1;
   }

   private static int listMobRagdollBlacklist(CommandSourceStack source) {
      MobRagdollBlacklistSavedData data = MobRagdollBlacklistSavedData.get(source.getLevel());
      String entities = formatBlacklistEntries(data.entities());
      String namespaces = formatBlacklistEntries(data.namespaces());
      source.sendSuccess(() -> Component.literal("Mob ragdoll blacklist: entities " + entities + "; mods " + namespaces + "."), false);
      return data.entities().size() + data.namespaces().size();
   }

   private static String formatBlacklistEntries(Collection<String> values) {
      if (values.isEmpty()) {
         return "none";
      }
      return String.join(", ", values.stream().sorted().toList());
   }

   private static CompletableFuture<Suggestions> suggestEntityNamespaces(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(
         BuiltInRegistries.ENTITY_TYPE.keySet().stream()
            .map(Identifier::getNamespace)
            .distinct()
            .sorted(),
         builder
      );
   }

   private static int startWailing(ServerPlayer player, int durationTicks, double stiffness, int intervalTicks) {
      return startWailing(
         player.createCommandSourceStack(),
         List.of(player),
         RagdollWailingOptions.builder()
            .durationTicks(durationTicks)
            .stiffness(stiffness)
            .intervalTicks(intervalTicks)
            .build()
      );
   }

   private static int startWailing(
      CommandSourceStack source,
      Collection<ServerPlayer> players,
      int durationTicks,
      double stiffness,
      int intervalTicks
   ) {
      return startWailing(
         source,
         players,
         RagdollWailingOptions.builder()
            .durationTicks(durationTicks)
            .stiffness(stiffness)
            .intervalTicks(intervalTicks)
            .build()
      );
   }

   private static int startWailing(CommandSourceStack source, Collection<ServerPlayer> players, RagdollWailingOptions options) {
      int applied = 0;
      for (ServerPlayer player : players) {
         var session = RagdollAPI.activeSession(player);
         if (session == null) {
            continue;
         }

         session.applyWailing(options);
         applied++;
      }

      int result = applied;
      if (result == 0) {
         source.sendFailure(Component.literal("No target players are currently ragdolled."));
      } else {
         source.sendSuccess(() -> Component.literal("Started wailing on " + result + " ragdoll(s)."), true);
      }
      return result;
   }

   private static int stopWailing(ServerPlayer player) {
      return stopWailing(player.createCommandSourceStack(), List.of(player));
   }

   private static int stopWailing(CommandSourceStack source, Collection<ServerPlayer> players) {
      int stopped = 0;
      for (ServerPlayer player : players) {
         var session = RagdollAPI.activeSession(player);
         if (session == null) {
            continue;
         }

         session.stopWailing();
         stopped++;
      }

      int result = stopped;
      if (result == 0) {
         source.sendFailure(Component.literal("No target players are currently ragdolled."));
      } else {
         source.sendSuccess(() -> Component.literal("Stopped wailing on " + result + " ragdoll(s)."), true);
      }
      return result;
   }

   private static int spawnDummyRagdoll(CommandSourceStack source, Vec3 position, double headingDegrees) {
      return spawnDummyRagdoll(source, position, headingDegrees, PlayerlessDespawnRule.defaultRule());
   }

   private static int spawnDummyRagdoll(CommandSourceStack source, Vec3 position, double headingDegrees, PlayerlessDespawnRule despawnRule) {
      PlayerlessRagdollSession session = RagdollAPI.spawnPlayerless(source.getLevel(), position, headingDegrees, Vec3.ZERO, despawnRule);
      return finishDummySpawn(source, session);
   }

   private static int spawnDummyRagdoll(CommandSourceStack source, Vec3 position, double headingDegrees, GameProfile profile) {
      return spawnDummyRagdoll(source, position, headingDegrees, profile, PlayerlessDespawnRule.defaultRule());
   }

   private static int spawnDummyRagdoll(CommandSourceStack source, Vec3 position, double headingDegrees, GameProfile profile, PlayerlessDespawnRule despawnRule) {
      GameProfile skinProfile = resolveSkinProfile(source, profile);
      PlayerlessRagdollSession session = RagdollAPI.spawnPlayerless(source.getLevel(), position, headingDegrees, skinProfile, Vec3.ZERO, despawnRule);
      return finishDummySpawn(source, session);
   }

   private static int spawnDummyRagdoll(CommandSourceStack source, Vec3 position, double headingDegrees, GameProfile profile, boolean withElytra) {
      GameProfile skinProfile = resolveSkinProfile(source, profile);
      PlayerlessRagdollSession session = RagdollAPI.spawnPlayerless(source.getLevel(), position, headingDegrees, skinProfile, Vec3.ZERO, PlayerlessDespawnRule.defaultRule());
      if (withElytra) equipTorso(source.getLevel(), session, new ItemStack(Items.ELYTRA));
      return finishDummySpawn(source, session);
   }

   private static void equipTorso(ServerLevel level, PlayerlessRagdollSession session, ItemStack stack) {
      if (session == null) return;
      SubLevel torsoSubLevel = SubLevelContainer.getContainer(level).getSubLevel(session.id());
      if (torsoSubLevel == null) return;
      if (torsoSubLevel.getLevel().getBlockEntity(torsoSubLevel.getPlot().getCenterBlock()) instanceof RagdollPartBlockEntity part) {
         part.setItemForSlot(EquipmentSlot.CHEST, stack);
      }
   }

   private static GameProfile firstProfile(Collection<NameAndId> profiles) {
      NameAndId profile = profiles.iterator().next();
      return new GameProfile(profile.id(), profile.name());
   }

   private static LiteralArgumentBuilder<CommandSourceStack> despawnOptions(DummyDespawnCommand command) {
      return Commands.literal("despawn")
         .then(Commands.literal("default")
            .executes(context -> command.run(new DummyDespawnContext(context, PlayerlessDespawnRule.defaultRule()))))
         .then(Commands.literal("never")
            .executes(context -> command.run(new DummyDespawnContext(context, PlayerlessDespawnRule.never()))))
         .then(Commands.literal("after_ticks")
            .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
               .executes(context -> command.run(new DummyDespawnContext(
                  context,
                  PlayerlessDespawnRule.afterTicks(IntegerArgumentType.getInteger(context, "ticks"))
               )))))
         .then(Commands.literal("below_speed")
            .then(Commands.argument("meters_per_second", DoubleArgumentType.doubleArg(0.0))
               .executes(context -> command.run(new DummyDespawnContext(
                  context,
                  PlayerlessDespawnRule.belowSpeed(DoubleArgumentType.getDouble(context, "meters_per_second"))
               )))));
   }

   private static GameProfile resolveSkinProfile(CommandSourceStack source, GameProfile profile) {
      if (!profile.properties().get("textures").isEmpty() || profile.id() == null) {
         return profile;
      }

      try {
         return source.getServer().services().profileResolver().fetchById(profile.id())
            .filter(resolved -> !resolved.properties().get("textures").isEmpty())
            .orElse(profile);
      } catch (Throwable error) {
         SablePlayerRagdoll.LOGGER.warn("[sable_player_ragdoll] failed to resolve skin for {}: {}", profile.name(), error.toString());
      }

      return profile;
   }

   private static int spawnMoblessRagdoll(CommandSourceStack source, Identifier entityId, Vec3 pos) {
      EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
      if (type == null) {
         source.sendFailure(Component.literal("Unknown entity type: " + entityId));
         return 0;
      }
      UUID id = RagdollAPI.spawnMobless(source.getLevel(), type, pos);
      if (id == null) {
         source.sendFailure(Component.literal("Could not ragdoll " + entityId + " (not a living entity, or not whitelisted)."));
         return 0;
      }
      source.sendSuccess(() -> Component.literal("Spawning mobless ragdoll " + RagdollRegistry.shortId(id) + " from " + entityId
         + " (needs a player nearby to capture the model)"), true);
      return 1;
   }

   private static int finishDummySpawn(CommandSourceStack source, PlayerlessRagdollSession session) {
      if (session == null) {
         source.sendFailure(Component.literal("Failed to spawn playerless ragdoll."));
         return 0;
      }

      String shortId = RagdollRegistry.shortId(session.id());
      source.sendSuccess(() -> Component.literal("Spawned playerless ragdoll " + shortId), true);
      return 1;
   }

   private static int giveRagdollingStick(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      ItemStack stack = new ItemStack(Items.STICK);
      RagdollItemTags.markTestItem(stack);
      stack.set(DataComponents.CUSTOM_NAME, Component.literal("Ragdolling Stick"));

      if (!player.getInventory().add(stack)) {
         player.drop(stack, false, Prediction.PREDICTED);
      }
      source.sendSuccess(() -> Component.literal("Gave ragdolling stick"), true);
      return 1;
   }

   private static int giveDeletingStick(CommandSourceStack source) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      ItemStack stack = DeletingStick.create();
      if (!player.getInventory().add(stack)) {
         player.drop(stack, false, Prediction.PREDICTED);
      }
      source.sendSuccess(() -> Component.literal("Gave deleting stick (left-click deletes, right-click dismembers)"), true);
      return 1;
   }

   @FunctionalInterface
   private interface DummyDespawnCommand {
      int run(DummyDespawnContext context) throws CommandSyntaxException;
   }

   private record DummyDespawnContext(CommandContext<CommandSourceStack> context, PlayerlessDespawnRule rule) {

      private CommandSourceStack getSource() {
         return this.context.getSource();
      }
   }

}
