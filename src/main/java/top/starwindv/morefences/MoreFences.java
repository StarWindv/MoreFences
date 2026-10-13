package top.starwindv.morefences;

import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.entity.FuelValues;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreFences implements ModInitializer {
	public static final String MOD_ID = "morefences";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		List<MoreFencesContent.Entry> entries = MoreFencesContent.entries();
		registerCreativeTabEntries();
		registerFuelValues();

		LOGGER.info(
			"Registered {} fence(s) and fence gate(s) in {} group(s): {}",
			entries.size(),
			FenceGroup.all().size(),
			FenceGroup.all().stream().map(FenceGroup::name).toList()
		);
	}

	/**
	 * Hands the burn times of the groups to the furnace, through the fabric-content-registries event so
	 * that no tag has to be misused (the vanilla fuel lists are {@code minecraft:wooden_fences} and
	 * {@code minecraft:fence_gates}, both of which would mean something else for our blocks).
	 */
	private static void registerFuelValues() {
		FuelValueEvents.BUILD.register((builder, context) -> {
			int baseSmeltTime = context.baseSmeltTime();

			for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
				addFuel(builder, entry.fenceItem(), entry.group().fenceBurnTime(baseSmeltTime));
				addFuel(builder, entry.fenceGateItem(), entry.group().gateBurnTime(baseSmeltTime));
			}
		});
	}

	private static void addFuel(FuelValues.Builder builder, Item item, int burnTime) {
		// A burn time of zero would still make the item a fuel, just one that never burns anything.
		if (burnTime > 0) {
			builder.add(item, burnTime);
		}
	}

	/**
	 * Adds the generated items to the building blocks tab, right behind the last vanilla fence or fence gate.
	 *
	 * <p>The stacks are created lazily when the tab is filled: components cannot be read this early during
	 * startup.
	 */
	private static void registerCreativeTabEntries() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			List<ItemStack> stacks = MoreFencesContent
				.entries()
				.stream()
				.flatMap(entry -> Stream.of(new ItemStack(entry.fenceItem()), new ItemStack(entry.fenceGateItem())))
				.toList();

			output.insertAfter(MoreFences::isFenceOrGate, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
		});
	}

	private static boolean isFenceOrGate(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			return block instanceof FenceBlock || block instanceof FenceGateBlock;
		}

		return false;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
