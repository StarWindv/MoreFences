package top.starwindv.morefences.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import top.starwindv.morefences.FenceGroup;

/**
 * A fence made of some material of a {@linkplain FenceGroup group}.
 *
 * <p>The group is what decides the connections, there is no per material logic: the fence connects to
 * every fence and fence gate of <b>its own</b> group (any colour, any block of that group), and to
 * anything else only when it exposes a solid face - which fences and gates never do. Wooden fences, the
 * nether brick fence and the vanilla fence gates are therefore kept out automatically; the opposite
 * direction (a vanilla fence next to one of ours) is handled by {@code FenceBlockMixin}.
 */
public class MoreFenceBlock extends FenceBlock {
	private final FenceGroup group;

	public MoreFenceBlock(final BlockBehaviour.Properties properties, final FenceGroup group) {
		super(properties);
		this.group = group;
	}

	public FenceGroup group() {
		return this.group;
	}

	@Override
	public boolean connectsTo(final BlockState state, final boolean faceSolid, final Direction direction) {
		if (this.group.matchesFence(state)) {
			return true;
		}

		if (this.group.matchesFenceGate(state) && state.hasProperty(FenceGateBlock.FACING) && FenceGateBlock.connectsToDirection(state, direction)) {
			return true;
		}

		return !isExceptionForConnection(state) && faceSolid;
	}
}
