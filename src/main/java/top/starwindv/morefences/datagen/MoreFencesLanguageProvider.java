package top.starwindv.morefences.datagen;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.world.level.block.Block;
import top.starwindv.morefences.FenceGroup;
import top.starwindv.morefences.MoreFencesContent;

/**
 * Names every generated block after its base block, e.g. {@code red_stained_glass} becomes "Red Stained
 * Glass Fence" and {@code white_wool} becomes "White Wool Fence".
 *
 * <p>The English name of the base block is taken from vanilla's own {@code en_us} file when it is known, a
 * title cased version of the registry path is used as a fallback for modded blocks.
 *
 * <p>It also names the group tags ({@code tag.item.morefences.glass_fences} becomes "Glass Fences"), which
 * is what recipe viewers display.
 */
public class MoreFencesLanguageProvider extends FabricLanguageProvider {
	public MoreFencesLanguageProvider(FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
		super(packOutput, registryLookup);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translationBuilder) {
		Set<String> written = new HashSet<>();
		BiConsumer<String, String> add = (key, value) -> {
			if (written.add(key)) {
				translationBuilder.add(key, value);
			}
		};

		for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
			String name = baseName(entry.base());

			// BlockItem usually resolves to the block description id, but write both to be safe.
			add.accept(entry.fence().getDescriptionId(), name + " Fence");
			add.accept(entry.fenceItem().getDescriptionId(), name + " Fence");
			add.accept(entry.fenceGate().getDescriptionId(), name + " Fence Gate");
			add.accept(entry.fenceGateItem().getDescriptionId(), name + " Fence Gate");
		}

		for (FenceGroup group : FenceGroup.all()) {
			// Group tags are looked up by recipe viewers, so they need a name as well.
			translationBuilder.add(group.fenceItemTag(), titleCase(group.name()) + " Fences");
			translationBuilder.add(group.fenceGateItemTag(), titleCase(group.name()) + " Fence Gates");
		}
	}

	private static String titleCase(String name) {
		return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static String baseName(Block base) {
		String key = base.getDescriptionId();
		Language language = Language.getInstance();
		return language.has(key) ? language.getOrDefault(key, fallbackName(base)) : fallbackName(base);
	}

	private static String fallbackName(Block base) {
		String path = BuiltInRegistries.BLOCK.getKey(base).getPath();
		StringBuilder name = new StringBuilder(path.length());

		for (String word : path.split("_")) {
			if (word.isEmpty()) {
				continue;
			}

			if (!name.isEmpty()) {
				name.append(' ');
			}

			name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}

		return name.toString();
	}
}
