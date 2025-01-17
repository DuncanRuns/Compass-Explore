package me.duncanruns.compassexplore.mixin;

import com.mojang.datafixers.util.Pair;
import me.duncanruns.compassexplore.BastionType;
import me.duncanruns.compassexplore.CompassExplore;
import me.duncanruns.compassexplore.mixinint.ChunkGeneratorInt;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin implements ChunkGeneratorInt {
    @Unique
    private static final ThreadLocal<CompassExplore.SearchType> searchType = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Long> seed = new ThreadLocal<>();

    @Inject(method = "locateStructure(Ljava/util/Set;Lnet/minecraft/world/WorldView;Lnet/minecraft/world/gen/StructureAccessor;ZLnet/minecraft/world/gen/chunk/placement/StructurePlacement;Lnet/minecraft/util/math/ChunkPos;)Lcom/mojang/datafixers/util/Pair;", at = @At("RETURN"), cancellable = true)
    private static void skipSearchedStructures(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos, CallbackInfoReturnable<Pair<BlockPos, RegistryEntry<Structure>>> cir) {
        if (cir.getReturnValue() == null) return;
        if (searchType.get() == null) return;
        switch (searchType.get()) {
            case CITIES:
                if (CompassExplore.cities.contains(pos)) cir.setReturnValue(null);
                break;
            case TREASURES:
                System.out.println("T");
                BastionType bastionType = BastionType.calculateType(seed.get(), pos);
                System.out.println("bastionType = " + bastionType);
                System.out.println("pos = " + pos);
                if (bastionType != BastionType.TREASURE) cir.setReturnValue(null);
                else if (CompassExplore.treasures.contains(pos)) cir.setReturnValue(null);
                break;
        }
    }

    @Shadow
    public abstract @Nullable Pair<BlockPos, RegistryEntry<Structure>> locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures);

    @Override
    public @Nullable Pair<BlockPos, RegistryEntry<Structure>> compassExplore$locateNewStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures, CompassExplore.SearchType searchType) {
        ChunkGeneratorMixin.searchType.set(searchType);
        seed.set(world.getSeed());
        Pair<BlockPos, RegistryEntry<Structure>> out = locateStructure(world, structures, center, radius, skipReferencedStructures);
        ChunkGeneratorMixin.searchType.remove();
        seed.remove();
        return out;
    }
}
