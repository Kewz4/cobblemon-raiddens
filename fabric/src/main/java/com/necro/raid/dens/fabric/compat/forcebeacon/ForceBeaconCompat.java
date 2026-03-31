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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility layer for the force-beacon-load mod.
 * Registers active raid crystals as phantom beacons in WorldBeaconData.
 * This class must only be loaded when force-beacon-load is present.
 *
 * We bypass WorldBeaconData.add() and put directly into the beacons map
 * to avoid the add() -> sendToAllPlayer() -> check() -> remove cycle.
 * Our phantoms are re-injected after each check() call via the WorldBeaconDataMixin.
 */
public class ForceBeaconCompat {
    private static final Map<BlockPos, BeaconBlockEntity> PHANTOM_BEACONS = new HashMap<>();

    public static void registerCrystal(ServerLevel level, BlockPos pos, RaidTier tier) {
        BeaconBlockEntity phantom = PHANTOM_BEACONS.get(pos);
        if (phantom == null) {
            phantom = createPhantom(level, pos, tier);
            PHANTOM_BEACONS.put(pos, phantom);
        }

        // Put directly into the beacons map, bypassing add() which triggers
        // sendToAllPlayer() -> check() -> removes our phantom
        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.getBeacons().put(pos, phantom);
    }

    public static void unregisterCrystal(ServerLevel level, BlockPos pos) {
        PHANTOM_BEACONS.remove(pos);
        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.remove(pos, level);
    }

    /**
     * Re-injects phantom beacons directly into the beacons map after check() removes them.
     * Called from WorldBeaconDataMixin at the RETURN of check().
     * This avoids the infinite recursion that occurs when using add().
     */
    public static void reRegisterPhantomsDirect(ServerLevel level, Map<BlockPos, BeaconBlockEntity> beaconsMap) {
        if (PHANTOM_BEACONS.isEmpty()) return;
        for (Map.Entry<BlockPos, BeaconBlockEntity> entry : PHANTOM_BEACONS.entrySet()) {
            BlockPos pos = entry.getKey();
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RaidCrystalBlockEntity) {
                beaconsMap.put(pos, entry.getValue());
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

        // Set beacon level via NBT
        int beaconLevel = tierToBeaconLevel(tier);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Levels", beaconLevel);
        phantom.loadCustomOnly(tag, level.registryAccess());

        // Mark as valid for force-beacon-load's IsLevelValid check
        ((IsLevelValid) phantom).setForcebeaconload$isLevelValid(true);

        // Populate beam sections so the client renders a beacon beam.
        // Without this, the phantom has empty beamSections and no beam appears.
        // Uses access-widened fields (beamSections, height).
        int beamHeight = level.getMaxBuildHeight() - pos.getY();
        BeaconBlockEntity.BeaconBeamSection section = new BeaconBlockEntity.BeaconBeamSection(0xF9FFFE);
        section.height = beamHeight;
        List<BeaconBlockEntity.BeaconBeamSection> sections = new ArrayList<>();
        sections.add(section);
        phantom.beamSections = sections;

        return phantom;
    }

    private static int tierToBeaconLevel(RaidTier tier) {
        return switch (tier) {
            case TIER_ONE, TIER_TWO -> 1;
            case TIER_THREE, TIER_FOUR -> 2;
            case TIER_FIVE, TIER_SIX -> 3;
            case TIER_SEVEN -> 4;
        };
    }
}
