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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compatibility layer for the force-beacon-load mod.
 * Creates phantom BeaconBlockEntity instances for active raid crystals
 * and registers them with force-beacon-load's WorldBeaconData.
 *
 * The WorldBeaconDataMixin prevents check() from removing our phantom entries,
 * so we can safely use the normal add() API without recursion issues.
 */
public class ForceBeaconCompat {
    private static final Set<BlockPos> PHANTOM_POSITIONS = new HashSet<>();
    private static BeaconBlockEntity lastPhantom;
    private static BlockPos lastPos;

    /**
     * Check if a position has a registered phantom beacon (used by WorldBeaconDataMixin).
     */
    public static boolean isPhantomPosition(BlockPos pos) {
        return PHANTOM_POSITIONS.contains(pos);
    }

    public static void registerCrystal(ServerLevel level, BlockPos pos, RaidTier tier) {
        if (PHANTOM_POSITIONS.contains(pos)) return; // Already registered

        BeaconBlockEntity phantom = createPhantom(level, pos, tier);
        PHANTOM_POSITIONS.add(pos);

        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.add(phantom, level);
    }

    public static void unregisterCrystal(ServerLevel level, BlockPos pos) {
        PHANTOM_POSITIONS.remove(pos);
        WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
        data.remove(pos, level);
    }

    /**
     * Cleans up phantom entries for crystals that no longer exist.
     * Called periodically from the tick mixin.
     */
    public static void cleanupPhantoms(ServerLevel level) {
        PHANTOM_POSITIONS.removeIf(pos -> {
            if (!level.isLoaded(pos)) return false;
            if (level.getBlockEntity(pos) instanceof RaidCrystalBlockEntity) return false;
            // Crystal gone - also remove from WorldBeaconData
            WorldBeaconData data = ForceBeaconLoad.INSTANCE.getBeaconData(level);
            data.remove(pos, level);
            return true;
        });
    }

    private static BeaconBlockEntity createPhantom(ServerLevel level, BlockPos pos, RaidTier tier) {
        BeaconBlockEntity phantom = new BeaconBlockEntity(pos, Blocks.BEACON.defaultBlockState());

        int beaconLevel = tierToBeaconLevel(tier);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Levels", beaconLevel);
        phantom.loadCustomOnly(tag, level.registryAccess());

        ((IsLevelValid) phantom).setForcebeaconload$isLevelValid(true);

        // Populate beam sections so force-beacon-load's client renders a beam.
        // The phantom is never ticked by vanilla, so beamSections would be empty.
        int beamHeight = level.getMaxBuildHeight() - pos.getY();
        populateBeamSections(phantom, beamHeight, 0xF9FFFE);

        return phantom;
    }

    /**
     * Populates the beamSections field on a BeaconBlockEntity using reflection.
     * Handles both Mojang-mapped (dev) and intermediary (prod) field names.
     */
    @SuppressWarnings("unchecked")
    private static void populateBeamSections(BeaconBlockEntity phantom, int height, int color) {
        try {
            // Find the BeaconBeamSection inner class by checking all inner classes
            Class<?> sectionClass = findBeamSectionClass();
            if (sectionClass == null) return;

            // Create a beam section with the given color
            Constructor<?> ctor = sectionClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            Object section = ctor.newInstance(color);

            // Set the height field (it's the only non-static int field that isn't color)
            setHeightField(section, sectionClass, height);

            // Create the sections list
            List<Object> sections = new ArrayList<>();
            sections.add(section);

            // Set the beamSections field on the phantom
            setBeamSectionsField(phantom, sections);
        } catch (Exception ignored) {
            // If reflection fails, beam just won't render on force-beacon-load clients
        }
    }

    private static Class<?> findBeamSectionClass() {
        for (Class<?> inner : BeaconBlockEntity.class.getDeclaredClasses()) {
            // Check if this class has a single-int constructor (the beam section constructor)
            try {
                inner.getDeclaredConstructor(int.class);
                return inner;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static void setHeightField(Object section, Class<?> sectionClass, int height) throws Exception {
        // The beam section has two int fields: color (final) and height (non-final).
        // We want the non-final one (height).
        for (Field f : sectionClass.getDeclaredFields()) {
            if (f.getType() == int.class
                && !java.lang.reflect.Modifier.isStatic(f.getModifiers())
                && !java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                f.setAccessible(true);
                f.setInt(section, height);
                return;
            }
        }
    }

    private static void setBeamSectionsField(BeaconBlockEntity phantom, List<?> sections) throws Exception {
        // BeaconBlockEntity has two List fields: beamSections and checkingBeamSections.
        // beamSections is the first one declared (the one used for rendering/serialization).
        // We set both to be safe.
        int listFieldCount = 0;
        for (Field f : BeaconBlockEntity.class.getDeclaredFields()) {
            if (f.getType() == List.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                f.set(phantom, new ArrayList<>(sections));
                listFieldCount++;
                if (listFieldCount >= 2) break; // Set both list fields
            }
        }
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
