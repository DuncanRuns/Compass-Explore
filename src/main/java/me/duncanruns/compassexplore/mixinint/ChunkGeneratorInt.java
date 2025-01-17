package me.duncanruns.compassexplore.mixinint;

import com.mojang.datafixers.util.Pair;
import me.duncanruns.compassexplore.CompassExplore;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

public interface ChunkGeneratorInt {
    @Nullable
    Pair<BlockPos, RegistryEntry<Structure>> compassExplore$locateNewStructure(
            ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures, CompassExplore.SearchType searchType
    );
}
