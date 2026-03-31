package com.necro.raid.dens.fabric.mixins.forcebeacon;

import com.necro.raid.dens.fabric.compat.forcebeacon.ForceBeaconCompat;
import name.forcebeaconload.WorldBeaconData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Mixin into force-beacon-load's WorldBeaconData.
 *
 * force-beacon-load's check() removes entries where the block at that position
 * is not a BeaconBlockEntity. We redirect the removeIf predicate to skip
 * our phantom beacon entries (raid crystal positions), preventing their removal.
 */
@Mixin(value = WorldBeaconData.class, remap = false)
public class WorldBeaconDataMixin {

    @Redirect(method = "check",
        at = @At(value = "INVOKE", target = "Ljava/util/Set;removeIf(Ljava/util/function/Predicate;)Z"))
    private boolean raiddens$keepPhantomBeacons(Set<Map.Entry<BlockPos, BeaconBlockEntity>> entrySet,
                                                 Predicate<Map.Entry<BlockPos, BeaconBlockEntity>> predicate) {
        return entrySet.removeIf(entry -> {
            if (ForceBeaconCompat.isPhantomPosition(entry.getKey())) return false;
            return predicate.test(entry);
        });
    }
}
