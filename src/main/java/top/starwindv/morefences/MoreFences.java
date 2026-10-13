package top.starwindv.morefences;

import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;

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

		LOGGER.info(
			"Registered {} fence(s) and fence gate(s) in {} group(s): {}",
			entries.size(),
			FenceGroup.all().size(),
			FenceGroup.all().stream().map(FenceGroup::name).toList()
		);
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
