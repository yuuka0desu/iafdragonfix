package com.iafdragonfix.mixin;

import com.iafdragonfix.IafDragonFix;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into WorldGenRegion to protect against crashes when world generation features
 * (such as IAF dragon dens/roosts) attempt to access blocks outside the region's chunk cache.
 *
 * The crash path:
 * 1. Dragon den/roost generation extends beyond the WorldGenRegion's chunk cache
 * 2. getBlockState() calls getChunk() for a chunk not in the region
 * 3. getChunk() throws RuntimeException("We are asking a region for a chunk out of bound")
 *
 * Fix: intercept getBlockState and return AIR when the target chunk is not in the region.
 */
@Mixin(value = WorldGenRegion.class, remap = false)
public abstract class MixinWorldGenRegion {

    /**
     * Shadow the hasChunk method using its SRG name to avoid refmap issues.
     */
    @Shadow(remap = false)
    public abstract boolean m_7232_(int chunkX, int chunkZ);

    /**
     * Inject at HEAD of getBlockState to return AIR for positions outside the region's chunk cache.
     * This prevents the RuntimeException from getChunk when IAF generation extends beyond the region.
     */
    @Inject(method = "m_8055_", at = @At("HEAD"), cancellable = true, remap = false)
    private void iafdragonfix$safeGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());

        if (!this.m_7232_(chunkX, chunkZ)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }

    /**
     * Inject at HEAD of addFreshEntity to prevent entity spawning outside the region's chunk cache.
     */
    @Inject(method = "m_7967_", at = @At("HEAD"), cancellable = true, remap = false)
    private void iafdragonfix$preventOutOfBoundsEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        BlockPos entityPos = entity.blockPosition();
        int chunkX = SectionPos.blockToSectionCoord(entityPos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(entityPos.getZ());

        if (!this.m_7232_(chunkX, chunkZ)) {
            IafDragonFix.LOGGER.debug(
                    "Prevented out-of-bounds entity spawn: {} at [{}, {}, {}]",
                    entity.getType().getDescriptionId(),
                    String.format("%.1f", entity.getX()),
                    String.format("%.1f", entity.getY()),
                    String.format("%.1f", entity.getZ())
            );
            entity.discard();
            cir.setReturnValue(false);
        }
    }
}
