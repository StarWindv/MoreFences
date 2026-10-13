package top.starwindv.morefences.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Item;
import top.starwindv.morefences.MoreFencesContent;

/**
 * Crafts the fences and fence gates exactly like the vanilla wooden ones, with the material of the group
 * in place of the planks and the group's rod in place of the sticks:
 *
 * <pre>
 * W#W       #W#
 * W#W       #W#
 * </pre>
 *
 * where {@code W} is the base block and {@code #} the rod of its group: glass panes for the glass group
 * (glass blocks without a pane of their own, tinted glass for instance, use plain glass panes), string for
 * the wool group.
 */
public class MoreFencesRecipeProvider extends FabricRecipeProvider {
	public MoreFencesRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	public String getName() {
		return "Fence Recipes";
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput recipeOutput) {
		return new RecipeProvider(registries, recipeOutput) {
			@Override
			public void buildRecipes() {
				for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
					Item rod = entry.rod();

					this.shaped(RecipeCategory.DECORATIONS, entry.fenceItem(), 3)
						.define('W', entry.baseItem())
						.define('#', rod)
						.pattern("W#W")
						.pattern("W#W")
						.group(entry.group().recipeGroup(false))
						.unlockedBy(getHasName(entry.baseItem()), this.has(entry.baseItem()))
						.save(this.output);

					this.shaped(RecipeCategory.REDSTONE, entry.fenceGateItem())
						.define('W', entry.baseItem())
						.define('#', rod)
						.pattern("#W#")
						.pattern("#W#")
						.group(entry.group().recipeGroup(true))
						.unlockedBy(getHasName(entry.baseItem()), this.has(entry.baseItem()))
						.save(this.output);
				}
			}
		};
	}
}
