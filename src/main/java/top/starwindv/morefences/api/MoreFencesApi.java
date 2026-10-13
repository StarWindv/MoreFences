package top.starwindv.morefences.api;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import top.starwindv.morefences.FenceGroup;
import top.starwindv.morefences.MoreFencesContent;
import top.starwindv.morefences.MoreFencesContent.Entry;

/**
 * Public API for other mods that want fences of their own material.
 *
 * <p>Call it while the game starts up, i.e. from a {@code "main"} entrypoint; order does not matter, the
 * pair is created as soon as the method is called:
 *
 * <pre>{@code
 * public class MyMod implements ModInitializer {
 *     @Override
 *     public void onInitialize() {
 *         // a fence of an existing material family ...
 *         MoreFencesApi.addGlass(MyBlocks.CRYSTAL_GLASS);
 *
 *         // ... or a whole new one, which only connects to itself
 *         FenceGroup crystal = MoreFencesApi.registerGroup("crystal", SoundType.AMETHYST, base -> MyItems.CRYSTAL_ROD, true);
 *         MoreFencesApi.addBlock(crystal, MyBlocks.CRYSTAL_BLOCK);
 *     }
 * }
 * }</pre>
 *
 * <p>What to keep in mind:
 * <ul>
 *     <li>Blocks registered later than this call (or after startup) cannot get a fence.</li>
 *     <li>The block needs an item form, that item is the "planks" of the recipe. The {@code #} of the
 *     recipe comes from the group (glass panes for {@link FenceGroup#GLASS}, string for
 *     {@link FenceGroup#WOOL}), pass one explicitly to change it.</li>
 *     <li>The sprite defaults to {@code <namespace>:block/<path>} of the block, which is what the vanilla
 *     models use. Pass one explicitly when the block's texture lives elsewhere - otherwise the fence ends
 *     up with the missing texture. It has to be a sprite of the block atlas, i.e. something that exists as
 *     a {@code textures/block/*.png} in one of the loaded resource packs.</li>
 *     <li>Models, recipes, loot tables and tags are generated while the MoreFences jar is built, so a
 *     complete result (working recipe, tags, ...) needs the foreign block to be present at build time too.
 *     Runtime only registers the blocks and items.</li>
 * </ul>
 */
public final class MoreFencesApi {
	private MoreFencesApi() {
	}

	/**
	 * Creates a fence and a fence gate of the glass group for the given block.
	 *
	 * @param block the glass block, must already be registered and have an item form
	 * @return the generated blocks and items
	 * @throws IllegalArgumentException if the block is not registered or has no item form
	 * @throws IllegalStateException    if called after startup, or if the ids are already taken
	 */
	public static Entry addGlass(Block block) {
		return addBlock(FenceGroup.GLASS, block, null, null);
	}

	/**
	 * @see #addGlass(Block)
	 * @param pane the pane used as the {@code #} of the recipes, {@code null} for {@code <base>_pane} or
	 *             plain glass panes
	 */
	public static Entry addGlass(Block block, @Nullable Item pane) {
		return addBlock(FenceGroup.GLASS, block, pane, null);
	}

	/**
	 * @see #addBlock(FenceGroup, Block, Item, Identifier)
	 */
	public static Entry addGlass(Block block, @Nullable Item pane, @Nullable Identifier sprite) {
		return addBlock(FenceGroup.GLASS, block, pane, sprite);
	}

	/**
	 * Creates a fence and a fence gate of the given group for the given block.
	 *
	 * @param group the material family the fence belongs to; it decides what the fence connects to
	 * @param block the block, must already be registered and have an item form
	 * @return the generated blocks and items
	 */
	public static Entry addBlock(FenceGroup group, Block block) {
		return addBlock(group, block, null, null);
	}

	/**
	 * Creates a fence and a fence gate of the given group for the given block.
	 *
	 * @param group  the material family the fence belongs to
	 * @param block  the block, must already be registered and have an item form
	 * @param rod    the {@code #} of the recipes, {@code null} for the group's default
	 * @param sprite the block atlas sprite the fence is rendered with, {@code null} for
	 *               {@code <namespace>:block/<path>} of the block
	 * @return the generated blocks and items
	 * @throws IllegalArgumentException if the block is not registered or has no item form
	 * @throws IllegalStateException    if called after startup, or if the ids are already taken
	 */
	public static Entry addBlock(FenceGroup group, Block block, @Nullable Item rod, @Nullable Identifier sprite) {
		if (group == null) {
			throw new IllegalArgumentException("The fence group must not be null");
		}

		if (block == Blocks.AIR) {
			throw new IllegalArgumentException("Cannot create a fence for the air block");
		}

		if (!(block.asItem() instanceof BlockItem)) {
			throw new IllegalArgumentException(
				"Cannot create a " + group.name() + " fence for " + BuiltInRegistries.BLOCK.getKey(block) + ", it needs an item form (a BlockItem) so that it can be crafted"
			);
		}

		return MoreFencesContent.register(group, block, rod, sprite);
	}

	/**
	 * Registers a whole new material family. Fences of the same group connect to each other and to nothing
	 * else, so a group keeps a mod's fences out of the glass and wool ones (and vice versa).
	 *
	 * @param name        the group name, used for the {@code morefences:<name>_fences} tags and the recipe
	 *                    book
	 * @param sound       the sound the fence gate of this group makes when placed or broken
	 * @param rodFor      returns the item that plays the role of the stick in the vanilla fence recipes
	 * @param translucent whether the fences are rendered in the translucent layer - what glass needs and
	 *                    wool does not
	 * @return the registered group
	 */
	public static FenceGroup registerGroup(String name, SoundType sound, Function<Block, Item> rodFor, boolean translucent) {
		return FenceGroup.register(name, sound, rodFor, translucent);
	}

	/**
	 * @return the generated pair of the given block, or {@code null} when it has none.
	 */
	public static @Nullable Entry getEntry(Block base) {
		return MoreFencesContent.entryOf(base);
	}

	/**
	 * @return whether a fence and fence gate were generated for the given block.
	 */
	public static boolean hasFenceFor(Block base) {
		return MoreFencesContent.entryOf(base) != null;
	}
}
