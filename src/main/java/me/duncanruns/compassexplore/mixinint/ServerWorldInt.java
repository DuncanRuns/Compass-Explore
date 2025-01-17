package me.duncanruns.compassexplore.mixinint;

import me.duncanruns.compassexplore.CompassExplore;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

public interface ServerWorldInt {
    @Nullable
    BlockPos compassExplore$locateNewStructure(TagKey<Structure> structureTag, BlockPos pos, int radius, boolean skipReferencedStructures, CompassExplore.SearchType searchType);
}
