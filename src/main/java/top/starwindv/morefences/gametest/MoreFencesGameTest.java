package top.starwindv.morefences.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import top.starwindv.morefences.FenceGroup;
import top.starwindv.morefences.MoreFencesContent;
import top.starwindv.morefences.api.MoreFencesApi;
import top.starwindv.morefences.tag.ModTags;

/**
 * Verifies the block interactions of the generated fences: what connects to what (which the
 * {@link FenceGroup} decides on its own), and that the gate keeps the vanilla redstone behaviour.
 *
 * <p>Placing a block recomputes the state of its <em>neighbours</em>, never its own, so the block that is
 * asserted about is always placed first and the block it should (or should not) connect to second.
 *
 * <p>Tests are picked up through the {@code fabric-gametest} entrypoint and run with
 * {@code ./gradlew runGameTest} (also part of {@code build}, since {@code check} depends on it).
 */
public class MoreFencesGameTest {
	/**
	 * Blocks added through the public API to prove that foreign blocks work the same way as the vanilla
	 * glass. Only done in the game test run: the datagen run also loads this class (in a dev environment),
	 * and generated resources for a terracotta fence would end up in the jar.
	 */
	private static final boolean API_BLOCKS_REGISTERED = System.getProperty("fabric-api.gametest") != null;
	private static final FenceGroup TEST_GROUP = API_BLOCKS_REGISTERED
		? MoreFencesApi.registerGroup("gametest", SoundType.WOOL, base -> Items.STRING, false)
		: null;
	private static final MoreFencesContent.Entry API_ENTRY = API_BLOCKS_REGISTERED ? MoreFencesApi.addGlass(Blocks.TERRACOTTA) : null;
	private static final Identifier EXPLICIT_SPRITE = Identifier.withDefaultNamespace("block/lapis_block");
	private static final MoreFencesContent.Entry SPRITE_ENTRY = API_BLOCKS_REGISTERED
		? MoreFencesApi.addGlass(Blocks.SMOOTH_STONE, null, EXPLICIT_SPRITE)
		: null;
	private static final MoreFencesContent.Entry GROUP_ENTRY = API_BLOCKS_REGISTERED ? MoreFencesApi.addBlock(TEST_GROUP, Blocks.OBSIDIAN) : null;

	private static final BlockPos WEST_END = new BlockPos(2, 1, 2);
	private static final BlockPos EAST_END = new BlockPos(3, 1, 2);

	@GameTest
	public void glassFencesConnect(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, fenceOf(Blocks.GLASS), Direction.EAST, true);
		helper.succeed();
	}

	@GameTest
	public void glassFencesOfDifferentColoursConnect(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, fenceOf(Blocks.STAINED_GLASS.pick(DyeColor.RED)), Direction.EAST, true);
		helper.succeed();
	}

	@GameTest
	public void woolFencesConnect(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.WOOL.pick(DyeColor.WHITE)), EAST_END, fenceOf(Blocks.WOOL.pick(DyeColor.BLACK)), Direction.EAST, true);
		helper.succeed();
	}

	/**
	 * The whole point of the groups: a wool fence and a glass fence belong to different groups and therefore
	 * never connect - without there being a single line of per material connection logic.
	 */
	@GameTest
	public void fencesOfDifferentGroupsDoNotConnect(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.WOOL.pick(DyeColor.WHITE)), EAST_END, fenceOf(Blocks.GLASS), Direction.EAST, false);
		assertConnection(helper, new BlockPos(2, 1, 4), fenceOf(Blocks.GLASS), new BlockPos(3, 1, 4), fenceOf(Blocks.WOOL.pick(DyeColor.WHITE)), Direction.EAST, false);
		helper.succeed();
	}

	@GameTest
	public void woolFenceDoesNotConnectToWoodenFence(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.WOOL.pick(DyeColor.WHITE)), EAST_END, Blocks.OAK_FENCE, Direction.EAST, false);
		helper.succeed();
	}

	/**
	 * Both directions are asserted: our fence deciding against the wooden fence, and the vanilla fence
	 * deciding against ours.
	 */
	@GameTest
	public void glassFenceDoesNotConnectToWoodenFence(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, Blocks.OAK_FENCE, Direction.EAST, false);
		assertConnection(helper, new BlockPos(2, 1, 4), Blocks.OAK_FENCE, new BlockPos(3, 1, 4), fenceOf(Blocks.GLASS), Direction.EAST, false);
		helper.succeed();
	}

	/**
	 * The nether brick fence is in {@code minecraft:fences} just like the glass fence, but not in
	 * {@code wooden_fences} - which is exactly the case where vanilla would connect the two.
	 */
	@GameTest
	public void glassFenceDoesNotConnectToNetherBrickFence(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, Blocks.NETHER_BRICK_FENCE, Direction.EAST, false);
		assertConnection(helper, new BlockPos(2, 1, 4), Blocks.NETHER_BRICK_FENCE, new BlockPos(3, 1, 4), fenceOf(Blocks.GLASS), Direction.EAST, false);
		helper.succeed();
	}

	@GameTest
	public void glassFenceDoesNotConnectToWoodenFenceGate(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, facingNorth(Blocks.OAK_FENCE_GATE), Direction.EAST, false);
		helper.succeed();
	}

	/**
	 * Vanilla fences attach to every fence gate that is aligned with them, including ours - which is
	 * exactly what has to be prevented.
	 */
	@GameTest
	public void woodenFenceDoesNotConnectToMoreFenceGate(GameTestHelper helper) {
		assertConnection(helper, WEST_END, Blocks.OAK_FENCE, EAST_END, facingNorth(gateOf(Blocks.GLASS)), Direction.EAST, false);
		helper.succeed();
	}

	/**
	 * A gate facing north has its posts on the east and west side, so an aligned fence connects - just
	 * like a wooden fence next to a wooden gate.
	 */
	@GameTest
	public void glassFenceConnectsToAlignedMoreFenceGate(GameTestHelper helper) {
		assertConnection(helper, WEST_END, fenceOf(Blocks.GLASS), EAST_END, facingNorth(gateOf(Blocks.GLASS)), Direction.EAST, true);
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void gateOpensAndClosesWithRedstone(GameTestHelper helper) {
		BlockPos gatePos = new BlockPos(3, 1, 3);
		BlockPos powerPos = gatePos.east();

		helper.setBlock(gatePos, gateOf(Blocks.GLASS));
		helper.assertFalse(helper.getBlockState(gatePos).getValue(FenceGateBlock.OPEN), "the gate must start closed");

		helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(helper.getBlockState(gatePos).getValue(FenceGateBlock.OPEN), "the gate must open when powered");
			helper.setBlock(powerPos, Blocks.AIR);
		});

		helper.runAfterDelay(6, () -> {
			helper.assertFalse(helper.getBlockState(gatePos).getValue(FenceGateBlock.OPEN), "the gate must close when unpowered");
			helper.succeed();
		});
	}

	/**
	 * Blocks added through {@link MoreFencesApi} get the very same treatment as the vanilla glass: the
	 * pair is created and connects like any other glass fence, even though the generated tags (which are
	 * data) do not know about it.
	 */
	@GameTest
	public void blocksAddedThroughTheApiWork(GameTestHelper helper) {
		if (API_ENTRY == null || SPRITE_ENTRY == null || GROUP_ENTRY == null) {
			helper.fail("the API blocks should have been registered by the game test entrypoint");
		}

		Identifier defaultSprite = Identifier.fromNamespaceAndPath("minecraft", "block/terracotta");
		helper.assertTrue(
			API_ENTRY.sprite().equals(defaultSprite),
			"the sprite should default to the block texture of the base block, got " + API_ENTRY.sprite()
		);
		helper.assertTrue(SPRITE_ENTRY.sprite().equals(EXPLICIT_SPRITE), "an explicitly given sprite should be kept, got " + SPRITE_ENTRY.sprite());

		assertConnection(helper, WEST_END, API_ENTRY.fence(), EAST_END, API_ENTRY.fence(), Direction.EAST, true);
		assertConnection(helper, WEST_END, API_ENTRY.fence(), EAST_END, fenceOf(Blocks.GLASS), Direction.EAST, true);

		// A fence of a group that was registered at runtime is not part of any generated tag, it still
		// stays inside its own group.
		assertConnection(helper, WEST_END, GROUP_ENTRY.fence(), EAST_END, GROUP_ENTRY.fence(), Direction.EAST, true);
		assertConnection(helper, WEST_END, GROUP_ENTRY.fence(), EAST_END, fenceOf(Blocks.GLASS), Direction.EAST, false);
		assertConnection(helper, new BlockPos(2, 1, 6), fenceOf(Blocks.GLASS), new BlockPos(3, 1, 6), GROUP_ENTRY.fence(), Direction.EAST, false);

		assertConnection(helper, new BlockPos(2, 1, 4), Blocks.OAK_FENCE, new BlockPos(3, 1, 4), API_ENTRY.fence(), Direction.EAST, false);
		helper.succeed();
	}

	/**
	 * Being part of {@code minecraft:fences} is what keeps leash knots, mob path finding and entity
	 * support working, while {@code wooden_fences} is deliberately left to the vanilla fences. The group
	 * tags are what the connection logic looks at, so they have to be right as well.
	 */
	@GameTest
	public void generatedFencesAreFencesButNotWoodenFences(GameTestHelper helper) {
		BlockPos pos = new BlockPos(2, 1, 2);
		helper.setBlock(pos, fenceOf(Blocks.GLASS));

		BlockState state = helper.getBlockState(pos);
		helper.assertTrue(state.is(BlockTags.FENCES), "the fence must be in #minecraft:fences");
		helper.assertFalse(state.is(BlockTags.WOODEN_FENCES), "the fence must not be in #minecraft:wooden_fences");
		helper.assertTrue(state.is(ModTags.FENCES), "the fence must be in #morefences:fences");
		helper.assertTrue(state.is(FenceGroup.GLASS.fenceTag()), "the fence must be in #morefences:glass_fences");
		helper.assertFalse(state.is(FenceGroup.WOOL.fenceTag()), "the fence must not be in #morefences:wool_fences");

		BlockPos woolPos = new BlockPos(4, 1, 2);
		helper.setBlock(woolPos, fenceOf(Blocks.WOOL.pick(DyeColor.WHITE)));
		BlockState woolState = helper.getBlockState(woolPos);
		helper.assertTrue(woolState.is(ModTags.FENCES), "the wool fence must be in #morefences:fences");
		helper.assertTrue(woolState.is(FenceGroup.WOOL.fenceTag()), "the wool fence must be in #morefences:wool_fences");
		helper.succeed();
	}

	/**
	 * Places {@code subject} first and {@code neighbour} next to it, so that the state of {@code subject}
	 * is (re)computed from the freshly placed neighbour, and asserts the connection property facing it.
	 */
	private static void assertConnection(
		GameTestHelper helper,
		BlockPos subjectPos,
		Block subject,
		BlockPos neighbourPos,
		BlockState neighbour,
		Direction direction,
		boolean expected
	) {
		helper.setBlock(subjectPos, subject);
		helper.setBlock(neighbourPos, neighbour);
		assertConnected(helper, subjectPos, direction, expected);
	}

	private static void assertConnection(
		GameTestHelper helper,
		BlockPos subjectPos,
		Block subject,
		BlockPos neighbourPos,
		Block neighbour,
		Direction direction,
		boolean expected
	) {
		assertConnection(helper, subjectPos, subject, neighbourPos, neighbour.defaultBlockState(), direction, expected);
	}

	private static void assertConnected(GameTestHelper helper, BlockPos pos, Direction direction, boolean expected) {
		BlockState state = helper.getBlockState(pos);
		boolean connected = state.getValue(propertyOf(direction));

		helper.assertTrue(
			connected == expected,
			"%s at %s (%s) is %s".formatted(state.getBlock(), pos, direction, connected ? "connected but should not be" : "not connected but should be")
		);
	}

	private static BooleanProperty propertyOf(Direction direction) {
		return switch (direction) {
			case NORTH -> CrossCollisionBlock.NORTH;
			case EAST -> CrossCollisionBlock.EAST;
			case SOUTH -> CrossCollisionBlock.SOUTH;
			case WEST -> CrossCollisionBlock.WEST;
			default -> throw new IllegalArgumentException("Not a horizontal direction: " + direction);
		};
	}

	private static BlockState facingNorth(Block block) {
		return block.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH);
	}

	private static Block fenceOf(Block base) {
		return entryOf(base).fence();
	}

	private static Block gateOf(Block base) {
		return entryOf(base).fenceGate();
	}

	private static MoreFencesContent.Entry entryOf(Block base) {
		MoreFencesContent.Entry entry = MoreFencesContent.entryOf(base);

		if (entry == null) {
			throw new IllegalStateException("No glass fence was generated for " + base);
		}

		return entry;
	}
}
