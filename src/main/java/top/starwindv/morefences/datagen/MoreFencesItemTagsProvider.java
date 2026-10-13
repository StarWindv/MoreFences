package top.starwindv.morefences.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.item.Item;
import top.starwindv.morefences.MoreFencesContent;

/**
 * The item side of {@link MoreFencesBlockTagsProvider}.
 *
 * <p>The fence items join {@code minecraft:fences} (the item tag mirrors the block tag), the group tags let
 * recipe viewers name the fences of one group. The gate items deliberately stay out of
 * {@code minecraft:fence_gates}: that tag is the list of items that can be burned as furnace fuel, and
 * glass and wool do not burn.
 */
public class MoreFencesItemTagsProvider extends FabricTagsProvider<Item> {
	public MoreFencesItemTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, Registries.ITEM, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		TagAppender<Item> vanillaFences = this.tag(BlockItemTags.FENCES.item());

		for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
			vanillaFences.add(entry.fenceItem().builtInRegistryHolder().key());
			this.tag(entry.group().fenceItemTag()).add(entry.fenceItem().builtInRegistryHolder().key());
			this.tag(entry.group().fenceGateItemTag()).add(entry.fenceGateItem().builtInRegistryHolder().key());
		}
	}
}
