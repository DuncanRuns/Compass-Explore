package me.duncanruns.compassexplore.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import me.duncanruns.compassexplore.CompassExplore;
import me.duncanruns.compassexplore.mixinint.ChunkGeneratorInt;
import me.duncanruns.compassexplore.mixinint.ServerWorldInt;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EntityLookupView;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements EntityLookupView, StructureWorldAccess, ServerWorldInt {
    @Unique
    private final ThreadLocal<CompassExplore.SearchType> searchType = new ThreadLocal<>();

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Shadow
    public abstract @Nullable BlockPos locateStructure(TagKey<Structure> structureTag, BlockPos pos, int radius, boolean skipReferencedStructures);

    @WrapOperation(method = "locateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;locateStructure(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/registry/entry/RegistryEntryList;Lnet/minecraft/util/math/BlockPos;IZ)Lcom/mojang/datafixers/util/Pair;"))
    private @Nullable Pair<BlockPos, RegistryEntry<Structure>> replaceLocate(ChunkGenerator instance, ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures, Operation<Pair<BlockPos, RegistryEntry<Structure>>> original) {
        if (searchType.get() == null)
            return original.call(instance, world, structures, center, radius, skipReferencedStructures);
        return ((ChunkGeneratorInt) instance).compassExplore$locateNewStructure(world, structures, center, radius, skipReferencedStructures, searchType.get());
    }

    @Override
    public BlockPos compassExplore$locateNewStructure(TagKey<Structure> structureTag, BlockPos pos, int radius, boolean skipReferencedStructures, CompassExplore.SearchType searchType) {
        this.searchType.set(searchType);
        BlockPos out = locateStructure(structureTag, pos, radius, skipReferencedStructures);
        this.searchType.remove();
        return out;
    }
}
