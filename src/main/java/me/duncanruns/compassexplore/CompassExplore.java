package me.duncanruns.compassexplore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

public class CompassExplore implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("compass-explore");

    public static final TagKey<Structure> END_CITY = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("compass-explore", "end_city"));
    public static final TagKey<Structure> BASTION_REMNANT = TagKey.of(RegistryKeys.STRUCTURE, Identifier.of("compass-explore", "bastion_remnant"));

    private static int structCheckTick = 20;

    public static HashSet<ChunkPos> cities;
    public static HashSet<ChunkPos> treasures;

    private static Path getWorldExploredTxt(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).normalize().resolve("explored.txt");
    }

    private static void loadExploredTxt(Path path) throws IOException {
        cities = new HashSet<>();
        treasures = new HashSet<>();

        if (!Files.exists(path)) {
            LOGGER.info("explored.txt not found, considering all cities and treasures unexplored.");
            return;
        }

        for (String line : Files.readAllLines(path)) {
            if (!(line.startsWith("cities:") || line.startsWith("treasures:"))) {
                continue;
            }

            String[] lineParts = line.split(":");
            if (lineParts.length != 2 || lineParts[1].isEmpty()) {
                continue;
            }
            String[] posStrings = lineParts[1].split(";");

            HashSet<ChunkPos> positions = line.startsWith("cities:") ? cities : treasures;
            for (String posString : posStrings) {
                if (posString.isEmpty()) continue;
                String[] numStrings = posString.split(",");
                if (numStrings.length != 2) continue;
                positions.add(new ChunkPos(Integer.parseInt(numStrings[0]), Integer.parseInt(numStrings[1])));
            }
        }
        LOGGER.info("Loaded explored file");
    }

    private static void saveExploredTxt(Path path) throws IOException {
        FileWriter writer = new FileWriter(path.toFile());
        writer.write("cities:");
        for (ChunkPos city : cities) {
            writer.write(String.format("%d,%d;", city.x, city.z));
        }
        writer.write("\ntreasures:");
        for (ChunkPos treasureBastion : treasures) {
            writer.write(String.format("%d,%d;", treasureBastion.x, treasureBastion.z));
        }
        writer.close();
        LOGGER.info("Saved explored file");
    }

    private static void trySaveExploredTxt(Path path) {
        try {
            saveExploredTxt(path);
        } catch (IOException e) {
            LOGGER.error("Failed to save explored.txt: " + e);
        }
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ExploreCommand::register);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                loadExploredTxt(getWorldExploredTxt(server));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            trySaveExploredTxt(getWorldExploredTxt(server));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            structCheckTick--;
            if (structCheckTick > 0) {
                return;
            }
            structCheckTick += 20;

            long seed = server.getWorld(World.OVERWORLD).getSeed();

            // Get non-spectator players
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList().stream().filter(serverPlayerEntity -> !serverPlayerEntity.isSpectator()).toList();

            // For all players in the end
            // THE_END
            players.stream().filter(player -> player.getWorld().getRegistryKey() == World.END).forEach(player -> {
                StructureAccessor structureAccessor = ((ServerWorld) player.getWorld()).getStructureAccessor();
                StructureStart ss = structureAccessor.getStructureContaining(player.getBlockPos(), END_CITY);
                if (ss == StructureStart.DEFAULT) {
                    return;
                }
                ChunkPos pos = ss.getPos();
                if (cities.add(pos)) {
                    player.sendMessage(Text.literal(String.format("New End City discovered at chunk [%d, %d]", pos.x, pos.z)).styled(style -> style.withColor(Formatting.GRAY)));
                    trySaveExploredTxt(getWorldExploredTxt(server));
                }
            });

            // For all players in the nether
            players.stream().filter(player -> player.getWorld().getRegistryKey() == World.NETHER).forEach(player -> {
                StructureAccessor structureAccessor = ((ServerWorld) player.getWorld()).getStructureAccessor();
                StructureStart ss = structureAccessor.getStructureContaining(player.getBlockPos(), BASTION_REMNANT);
                if (ss == StructureStart.DEFAULT) {
                    return;
                }
                ChunkPos pos = ss.getPos();

                if (BastionType.calculateType(seed, pos) == BastionType.TREASURE && treasures.add(pos)) {
                    player.sendMessage(Text.literal(String.format("New Treasure Bastion discovered at chunk [%d, %d]", pos.x, pos.z)).styled(style -> style.withColor(Formatting.GRAY)));
                    trySaveExploredTxt(getWorldExploredTxt(server));
                }
            });
        });
    }

    public enum SearchType {
        CITIES,
        TREASURES
    }
}