package top.starwindv.morefences.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import top.starwindv.morefences.MoreFences;

/**
 * Tags that cover <b>every</b> fence and fence gate of this mod, no matter the group.
 *
 * <p>The per group tags live on {@link top.starwindv.morefences.FenceGroup} and are what decides which
 * fences connect to each other; these two are the coarse ones, used to keep the vanilla fences away from
 * ours (see {@code FenceBlockMixin}) and for other mods and datapacks to address all of them at once.
 */
public final class ModTags {
	public static final TagKey<Block> FENCES = TagKey.create(Registries.BLOCK, MoreFences.id("fences"));
	public static final TagKey<Block> FENCE_GATES = TagKey.create(Registries.BLOCK, MoreFences.id("fence_gates"));

	private ModTags() {
	}
}
