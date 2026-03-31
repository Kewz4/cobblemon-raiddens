package com.necro.raid.dens.fabric.compat.forcebeacon;

import com.necro.raid.dens.common.blocks.entity.RaidCrystalBlockEntity;
import com.necro.raid.dens.common.data.raid.RaidTier;
import name.forcebeaconload.ForceBeaconLoad;
import name.forcebeaconload.IsLevelValid;
import name.forcebeaconload.WorldBeaconData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility layer for the force-beacon-load mod.
 * Registers active raid crystals as phantom beacons in WorldBeaconData.
 * This class must only be loaded when force-beacon-load is present.
 */
public class ForceBeaconCompat {
    private static final Map<BlockPos, BeaconBlockEntity> PHANTOM_BEACONS = new HashMap<>();

    public static void registerCrystal(ServerLevel level, BlockPos pos, RaidTier tier) {
        BeaconBlockEntity phantom = PHANTOM_BEACONS.get(pos);
        if (phantom == null) {
            phantom = createPhantom(level, pos, tier);
            PHANTOM_BEACONS.put(pos, phantom);
        }

        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.add(phantom, level);
    }

    public static void unregisterCrystal(ServerLevel level, BlockPos pos) {
        PHANTOM_BEACONS.remove(pos);
        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.remove(pos, level);
    }

    /**
     * Re-registers all known phantom beacons for a given world.
     * Called after WorldBeaconData.check() removes our entries.
     */
    public static void reRegisterPhantoms(ServerLevel level) {
        if (PHANTOM_BEACONS.isEmpty()) return;
        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        for (Map.Entry<BlockPos, BeaconBlockEntity> entry : PHANTOM_BEACONS.entrySet()) {
            BlockPos pos = entry.getKey();
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RaidCrystalBlockEntity) {
                data.add(entry.getValue(), level);
            }
        }
    }

    /**
     * Cleans up phantom entries for crystals that no longer exist.
     */
    public static void cleanupPhantoms(ServerLevel level) {
        PHANTOM_BEACONS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            return level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof RaidCrystalBlockEntity);
        });
    }

    private static BeaconBlockEntity createPhantom(ServerLevel level, BlockPos pos, RaidTier tier) {
        BeaconBlockEntity phantom = new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());

        // Set beacon level via NBT to avoid private field access
        int beaconLevel = tierToBeaconLevel(tier);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Levels", beaconLevel);
        phantom.loadCustomOnly(tag, level.registryAccess());

        // Mark as valid for force-beacon-load's IsLevelValid check
        ((IsLevelValid) phantom).setForcebeaconload$isLevelValid(true);

        return phantom;
    }

    /**
     * Maps RaidTier to beacon level (1-4).
     * Beacon range formula: level * 50 + 100 blocks.
     */
    private static int tierToBeaconLevel(RaidTier tier) {
        return switch (tier) {
            case TIER_ONE, TIER_TWO -> 1;       // 150 block range
            case TIER_THREE, TIER_FOUR -> 2;     // 200 block range
            case TIER_FIVE, TIER_SIX -> 3;       // 250 block range
            case TIER_SEVEN -> 4;                 // 300 block range
        };
    }
}
