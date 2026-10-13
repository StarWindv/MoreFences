package top.starwindv.morefences.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import top.starwindv.morefences.MoreFencesContent;

/**
 * Every base block of a group drops itself, so the generated fences do the same.
 */
public class MoreFencesLootProvider extends FabricBlockLootSubProvider {
	public MoreFencesLootProvider(FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(packOutput, registriesFuture);
	}

	@Override
	public void generate() {
		for (MoreFencesContent.Entry entry : MoreFencesContent.entries()) {
			this.dropSelf(entry.fence());
			this.dropSelf(entry.fenceGate());
		}
	}
}
