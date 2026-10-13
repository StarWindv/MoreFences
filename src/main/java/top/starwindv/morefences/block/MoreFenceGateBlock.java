package top.starwindv.morefences.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import top.starwindv.morefences.FenceGroup;

/**
 * A fence gate made of some material of a {@linkplain FenceGroup group}.
 *
 * <p>All vanilla behaviour (redstone powered opening, explosion toggling, {@code IN_WALL} calculation,
 * waterlogging via placement, ...) is inherited from {@link FenceGateBlock}. Only the material related
 * parts are specialised: the block sounds come from the group, so a glass gate tinkers and a wool gate
 * rustles.
 */
public class MoreFenceGateBlock extends FenceGateBlock {
	/**
	 * The group is part of the codec because {@link #MoreFenceGateBlock(BlockBehaviour.Properties, FenceGroup)}
	 * needs it; vanilla's own fence gate codec would silently build a wooden gate.
	 */
	public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(
		i -> i.group(Codec.STRING.fieldOf("group").forGetter(MoreFenceGateBlock::groupName), propertiesCodec()).apply(i, MoreFenceGateBlock::create)
	);

	private final FenceGroup group;

	public MoreFenceGateBlock(final BlockBehaviour.Properties properties, final FenceGroup group) {
		super(group.gateWoodType(), properties);
		this.group = group;
	}

	public FenceGroup group() {
		return this.group;
	}

	@Override
	public MapCodec<FenceGateBlock> codec() {
		return CODEC;
	}

	private static String groupName(final FenceGateBlock gate) {
		return ((MoreFenceGateBlock) gate).group().name();
	}

	private static MoreFenceGateBlock create(final String groupName, final BlockBehaviour.Properties properties) {
		FenceGroup group = FenceGroup.byName(groupName);

		if (group == null) {
			throw new IllegalArgumentException("Unknown fence group: " + groupName);
		}

		return new MoreFenceGateBlock(properties, group);
	}
}
