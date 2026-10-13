package top.starwindv.morefences.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Registers every data provider of this mod. Run it with {@code ./gradlew runDatagen} (or just
 * {@code ./gradlew build}, which runs it as well); the result is written to {@code build/datagen} and
 * packaged into the jar. It is a build artifact and not part of the repository.
 *
 * <p>All providers iterate over {@link top.starwindv.morefences.MoreFencesContent}, which discovers the
 * vanilla blocks of every {@linkplain top.starwindv.morefences.FenceGroup group} at runtime - regenerating
 * after a Minecraft update is enough to cover newly added glass or wool.
 */
public class MoreFencesDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(MoreFencesModelProvider::new);
		pack.addProvider(MoreFencesRecipeProvider::new);
		pack.addProvider(MoreFencesLanguageProvider::new);
		pack.addProvider(MoreFencesLootProvider::new);
		pack.addProvider(MoreFencesBlockTagsProvider::new);
		pack.addProvider(MoreFencesItemTagsProvider::new);
	}
}
