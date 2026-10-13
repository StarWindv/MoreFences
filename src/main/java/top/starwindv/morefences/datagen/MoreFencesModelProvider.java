package top.starwindv.morefences.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.BlockModelGenerators.BlockFamilyProvider;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;

import top.starwindv.morefences.MoreFencesContent;

/**
 * Generates the blockstates and models by reusing the vanilla fence/fence gate templates
 * ({@code block/fence_post}, {@code block/fence_side}, {@code block/fence_inventory},
 * {@code block/template_fence_gate*}), only the {@code texture} slot is filled in.
 *
 * <p>The texture slot points at the sprite the fence was generated for - {@code <ns>:block/<path>} of the
 * base block unless the API said otherwise, e.g. {@code minecraft:block/red_stained_glass}. Translucent
 * groups (glass) get {@code force_translucent} on top, exactly like the vanilla glass models, opaque ones
 * (wool) do not. Resource packs that retexture the base blocks therefore retexture the fences as well.
 */
public class MoreFencesModelProvider extends FabricModelProvider {
	public MoreFencesModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {
		for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
			// Translucent groups (glass) are rendered in the translucent layer, like the vanilla glass
			// models; opaque ones (wool) are not.
			TextureMapping mapping = new TextureMapping().put(TextureSlot.TEXTURE, new Material(entry.sprite(), entry.group().isTranslucent()));

			blockModelGenerators.new BlockFamilyProvider(mapping).fence(entry.fence());
			blockModelGenerators.new BlockFamilyProvider(mapping).fenceGate(entry.fenceGate());
		}
	}

	@Override
	public void generateItemModels(ItemModelGenerators itemModelGenerators) {
		// The fence item model is registered by BlockFamilyProvider#fence, the fence gate falls back to the
		// closed gate block model - both matching how vanilla does it for its wooden fences.
	}
}
