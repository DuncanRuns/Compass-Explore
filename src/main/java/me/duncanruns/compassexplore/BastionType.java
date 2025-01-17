package me.duncanruns.compassexplore;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;

public enum BastionType {
    HOUSING,
    STABLES,
    TREASURE,
    BRIDGE;

    public static BastionType calculateType(long worldSeed, ChunkPos pos) {
        // Thanks to matthew bolan code buried in Monkeys server giving me a starting point
        ChunkRandom random = new ChunkRandom(new CheckedRandom(0L));
        random.setCarverSeed(worldSeed, pos.x, pos.z);
        random.nextInt(4); // First RNG call is junk
        return values()[random.nextInt(4)];
    }
}
