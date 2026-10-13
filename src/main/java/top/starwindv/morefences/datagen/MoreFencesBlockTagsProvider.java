package top.starwindv.morefences.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import top.starwindv.morefences.MoreFencesContent;
import top.starwindv.morefences.tag.ModTags;

/**
 * <ul>
 *     <li>{@code minecraft:fences} / {@code minecraft:fence_gates} keep the generated blocks behaving like
 *     fences for the game itself (leash knots, mob path finding, entity support, ...).</li>
 *     <li>{@code morefences:fences} / {@code morefences:fence_gates} cover this mod as a whole and are what
 *     keeps the vanilla fences away from ours (see {@code FenceBlockMixin}).</li>
 *     <li>{@code morefences:<group>_fences} / {@code morefences:<group>_fence_gates} are what the
 *     connection logic of {@link top.starwindv.morefences.block.MoreFenceBlock} looks at: only fences of
 *     the same group connect. A datapack can pull an extra block into a group with them.</li>
 * </ul>
 *
 * <p>The generated fences are intentionally <b>not</b> part of {@code minecraft:wooden_fences}, which is
 * what keeps them from connecting to the wooden fences.
 */
public class MoreFencesBlockTagsProvider extends FabricTagsProvider<Block> {
	public MoreFencesBlockTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, Registries.BLOCK, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		TagAppender<Block> allFences = this.tag(ModTags.FENCES);
		TagAppender<Block> allFenceGates = this.tag(ModTags.FENCE_GATES);
		TagAppender<Block> vanillaFences = this.tag(BlockTags.FENCES);
		TagAppender<Block> vanillaFenceGates = this.tag(BlockTags.FENCE_GATES);

		for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
			allFences.add(entry.fence().builtInRegistryHolder().key());
			allFenceGates.add(entry.fenceGate().builtInRegistryHolder().key());
			vanillaFences.add(entry.fence().builtInRegistryHolder().key());
			vanillaFenceGates.add(entry.fenceGate().builtInRegistryHolder().key());
			this.tag(entry.group().fenceTag()).add(entry.fence().builtInRegistryHolder().key());
			this.tag(entry.group().fenceGateTag()).add(entry.fenceGate().builtInRegistryHolder().key());
		}
	}
}
