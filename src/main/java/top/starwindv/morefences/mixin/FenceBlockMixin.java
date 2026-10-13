package top.starwindv.morefences.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.morefences.MoreFencesContent;

/**
 * Keeps vanilla fences away from the fences of this mod.
 *
 * <p>{@link FenceBlock#connectsTo(BlockState, boolean, Direction)} connects a fence to
 * <ul>
 *     <li>another fence that is in {@code minecraft:fences} and shares its {@code wooden_fences}
 *     membership - which would make the nether brick fence attach to glass or wool fences, and</li>
 *     <li>every {@link net.minecraft.world.level.block.FenceGateBlock} - which would make every wooden
 *     fence attach to our fence gates.</li>
 * </ul>
 *
 * <p>Which of our fences connect to each other is decided by their {@link top.starwindv.morefences.FenceGroup},
 * vanilla fences simply never connect to ours, so it is enough to cancel the vanilla check whenever the
 * neighbour is one of ours. The check is tag <b>or</b> class based, see
 * {@link MoreFencesContent#isOurFence(BlockState)}.
 */
@Mixin(FenceBlock.class)
public class FenceBlockMixin {
	@Inject(method = "connectsTo", at = @At("HEAD"), cancellable = true)
	private void morefences$doNotConnectToFencesOfThisMod(BlockState state, boolean faceSolid, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (MoreFencesContent.isOurFence(state) || MoreFencesContent.isOurFenceGate(state)) {
			cir.setReturnValue(false);
		}
	}
}
