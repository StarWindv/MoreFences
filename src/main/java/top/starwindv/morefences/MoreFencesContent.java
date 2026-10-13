package top.starwindv.morefences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import top.starwindv.morefences.block.MoreFenceBlock;
import top.starwindv.morefences.block.MoreFenceGateBlock;
import top.starwindv.morefences.tag.ModTags;

/**
 * The table of blocks that own a fence + fence gate pair, together with the pipeline that turns a block
 * into that pair.
 *
 * <p>The table is filled from two places:
 * <ul>
 *     <li>every block of the {@code minecraft} namespace that matches a {@linkplain FenceGroup group's}
 *     discovery rule - nothing is hardcoded, so glass or wool added by a future Minecraft version is
 *     picked up automatically (rebuild to get its resources), and</li>
 *     <li>whatever other mods push in through
 *     {@link top.starwindv.morefences.api.MoreFencesApi}.</li>
 * </ul>
 *
 * <p>Other namespaces are <b>not</b> scanned automatically: the blockstates, models and recipes are
 * generated while the jar is built, so a block that is only registered at runtime would end up without
 * them. Mods that want a fence of their own are expected to call the API and to be present while the jar
 * is built (see {@link top.starwindv.morefences.api.MoreFencesApi}).
 */
public final class MoreFencesContent {
	/**
	 * @param group      the {@linkplain FenceGroup group} the pair belongs to, which is what decides what
	 *                   the fence connects to
	 * @param base       the block this pair was generated for
	 * @param baseItem   the item form of {@code base}, used as the "planks" of the vanilla fence recipes
	 * @param rod        the item used as the "stick" of the vanilla fence recipes, {@code <base>_pane} /
	 *                   string depending on the group unless one was given explicitly
	 * @param sprite     the block atlas sprite the fence is rendered with, {@code <ns>:block/<path>} of the
	 *                   base block unless one was given explicitly
	 * @param fence      the generated fence
	 * @param fenceGate  the generated fence gate
	 */
	public record Entry(
		FenceGroup group,
		Block base,
		Item baseItem,
		Item rod,
		Identifier sprite,
		Block fence,
		Block fenceGate,
		Item fenceItem,
		Item fenceGateItem
	) {
	}

	/**
	 * How a group finds its blocks in the vanilla registry, together with the order they are listed in.
	 */
	private record Discovery(FenceGroup group, Predicate<Block> rule, ToIntFunction<Block> rank) {
	}

	private static final Map<Block, Entry> ENTRIES = new LinkedHashMap<>();
	private static boolean vanillaScanned;

	private MoreFencesContent() {
	}

	/**
	 * @return an immutable, insertion ordered view of every generated fence pair. Triggers the scan of the
	 *         vanilla blocks on first access.
	 */
	public static List<Entry> entries() {
		scanVanilla();
		return List.copyOf(ENTRIES.values());
	}

	/**
	 * @return the pair generated for the given block, or {@code null} when there is none.
	 */
	public static @Nullable Entry entryOf(Block base) {
		scanVanilla();
		return ENTRIES.get(base);
	}

	/**
	 * Creates (or returns the already created) fence + fence gate pair of a block.
	 *
	 * <p>Prefer {@link top.starwindv.morefences.api.MoreFencesApi}, this is the shared implementation.
	 *
	 * @param group  the group the fence belongs to
	 * @param base   the block, must be registered and have an item form
	 * @param rod    the item used in the recipes, {@code null} for the group's default
	 * @param sprite the sprite the fence is rendered with, {@code null} for {@code <ns>:block/<path>} of
	 *               the base block
	 */
	public static Entry register(FenceGroup group, Block base, @Nullable Item rod, @Nullable Identifier sprite) {
		Entry existing = ENTRIES.get(base);

		if (existing != null) {
			return existing;
		}

		Identifier baseId = BuiltInRegistries.BLOCK.getKey(base);

		if (base == Blocks.AIR || baseId.getPath().equals("air")) {
			throw new IllegalArgumentException("The block has to be registered before it can get a fence: " + base);
		}

		if (!(base.asItem() instanceof BlockItem baseItem) || baseItem.getBlock() != base) {
			throw new IllegalArgumentException("The block needs an item form so that it can be used in the recipes: " + baseId);
		}

		Identifier fenceId = MoreFences.id(baseId.getPath() + "_fence");
		Identifier fenceGateId = MoreFences.id(baseId.getPath() + "_fence_gate");

		// BuiltInRegistries.BLOCK is a defaulted registry (unknown ids resolve to air), so the already
		// registered ids are collected explicitly instead of probing the registry for them.
		if (isRegistered(fenceId) || isRegistered(fenceGateId)) {
			throw new IllegalStateException("Cannot create a fence for " + baseId + ", " + fenceId + " is already taken");
		}

		if (!"minecraft".equals(baseId.getNamespace())) {
			MoreFences.LOGGER.info(
				"Adding a {} fence for the foreign block {}. Note that its models, recipes and tags are generated while the jar"
					+ " is built, so it only shows up completely in jars that were built with that block present.",
				group.name(),
				baseId
			);
		}

		// The properties need the registry key of the block they belong to (loot table and translation key
		// are derived from it when the block is constructed).
		ResourceKey<Block> fenceKey = ResourceKey.create(Registries.BLOCK, fenceId);
		ResourceKey<Block> fenceGateKey = ResourceKey.create(Registries.BLOCK, fenceGateId);

		Block fence;
		Block fenceGate;

		try {
			fence = Registry.register(
				BuiltInRegistries.BLOCK,
				fenceKey,
				new MoreFenceBlock(BlockBehaviour.Properties.ofLegacyCopy(base).setId(fenceKey), group)
			);
			fenceGate = Registry.register(
				BuiltInRegistries.BLOCK,
				fenceGateKey,
				new MoreFenceGateBlock(BlockBehaviour.Properties.ofLegacyCopy(base).setId(fenceGateKey), group)
			);
		} catch (RuntimeException exception) {
			throw new IllegalStateException(
				"Fences have to be created while the game is starting up (a \"main\" entrypoint), before the registries are"
					+ " frozen. Failed to create " + fenceId,
				exception
			);
		}

		Item fenceItem = Registry.register(
			BuiltInRegistries.ITEM,
			ResourceKey.create(Registries.ITEM, fenceId),
			new BlockItem(fence, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, fenceId)))
		);
		Item fenceGateItem = Registry.register(
			BuiltInRegistries.ITEM,
			ResourceKey.create(Registries.ITEM, fenceGateId),
			new BlockItem(fenceGate, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, fenceGateId)))
		);

		Entry entry = new Entry(
			group,
			base,
			baseItem,
			rod != null ? rod : group.rodFor(base),
			sprite != null ? sprite : defaultSprite(baseId),
			fence,
			fenceGate,
			fenceItem,
			fenceGateItem
		);
		ENTRIES.put(base, entry);
		return entry;
	}

	/**
	 * Whether the block state is a fence of this mod, no matter the group. Used to keep the vanilla fences
	 * away from ours; the class check covers blocks that were registered at runtime, the tag covers the
	 * ones a datapack pulled in.
	 */
	public static boolean isOurFence(BlockState state) {
		return state.is(ModTags.FENCES) || state.getBlock() instanceof MoreFenceBlock;
	}

	/**
	 * The counterpart of {@link #isOurFence(BlockState)} for fence gates.
	 */
	public static boolean isOurFenceGate(BlockState state) {
		return state.is(ModTags.FENCE_GATES) || state.getBlock() instanceof MoreFenceGateBlock;
	}

	/**
	 * The sprite vanilla conventions imply for a block: {@code <namespace>:block/<path>}. This is what the
	 * vanilla models use, so a resource pack that retextures the base block retextures our fence too.
	 */
	public static Identifier defaultSprite(Identifier blockId) {
		return blockId.withPrefix("block/");
	}

	/**
	 * The pane of a glass block ({@code <base>_pane}), plain glass panes when there is none. Used as the
	 * {@code #} of the glass group's recipes.
	 */
	public static Item paneFor(Block base) {
		Identifier baseId = BuiltInRegistries.BLOCK.getKey(base);
		Block pane = findBlock(Identifier.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_pane"));

		if (pane != null && pane.asItem() instanceof BlockItem) {
			return pane.asItem();
		}

		MoreFences.LOGGER.debug("No pane block found for {}, the recipes fall back to glass panes", baseId);
		return Items.GLASS_PANE;
	}

	/**
	 * Scans the vanilla blocks once, group by group. Only the {@code minecraft} namespace is considered,
	 * everything else has to be added explicitly through the API.
	 */
	private static void scanVanilla() {
		if (vanillaScanned) {
			return;
		}

		vanillaScanned = true;

		List<Discovery> discoveries = List.of(
			new Discovery(FenceGroup.GLASS, MoreFencesContent::isMinecraftGlass, MoreFencesContent::glassRank),
			new Discovery(FenceGroup.WOOL, MoreFencesContent::isMinecraftWool, MoreFencesContent::woolRank)
		);

		for (Discovery discovery : discoveries) {
			Comparator<Block> order = Comparator.<Block>comparingInt(discovery.rank())
				.thenComparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString());

			BuiltInRegistries.BLOCK
				.listElements()
				.map(holder -> holder.value())
				.filter(block -> "minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
				.filter(discovery.rule())
				.sorted(order)
				.forEach(block -> register(discovery.group(), block, null, null));
		}
	}

	private static boolean isMinecraftGlass(Block block) {
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath();

		// Glass panes and bars are the thin variants of a glass block, they keep using the vanilla pane
		// models instead of getting their own fence.
		return path.contains("glass") && !path.contains("pane") && !path.contains("bars") && block.asItem() instanceof BlockItem;
	}

	private static boolean isMinecraftWool(Block block) {
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
		return path.endsWith("_wool") && block.asItem() instanceof BlockItem;
	}

	/**
	 * Plain glass first, tinted glass second, the coloured ones after that.
	 */
	private static int glassRank(Block block) {
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath();

		if (path.equals("glass")) {
			return 0;
		}

		return path.equals("tinted_glass") ? 1 : 2;
	}

	/**
	 * In the vanilla dye order, white first and black last.
	 */
	private static int woolRank(Block block) {
		String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
		String color = path.substring(0, path.length() - "_wool".length());

		try {
			return DyeColor.valueOf(color.toUpperCase(Locale.ROOT)).ordinal();
		} catch (IllegalArgumentException ignored) {
			return Integer.MAX_VALUE;
		}
	}

	private static @Nullable Block findBlock(Identifier id) {
		return BuiltInRegistries.BLOCK
			.listElements()
			.filter(holder -> holder.key().identifier().equals(id))
			.map(holder -> holder.value())
			.findFirst()
			.orElse(null);
	}

	private static boolean isRegistered(Identifier id) {
		return findBlock(id) != null;
	}
}
