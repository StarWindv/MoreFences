package top.starwindv.morefences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import top.starwindv.morefences.block.MoreFenceBlock;
import top.starwindv.morefences.block.MoreFenceGateBlock;

/**
 * A family of materials, for instance "glass" or "wool".
 *
 * <p><b>A group is what decides what connects to what:</b> fences of the same group connect to each other,
 * fences of different groups never do - and neither do they connect to the vanilla wooden or nether brick
 * fences (see {@code FenceBlockMixin}). Nothing needs to be written down per material, being in the same
 * group is enough.
 *
 * <p>Built in groups: {@link #GLASS} and {@link #WOOL}. Other mods can add their own material family with
 * {@link #register(String, SoundType, Function)}, which is what makes their fences connect to each other
 * without leaking into the other groups.
 *
 * <p>Every group owns a set of tags ({@code morefences:<name>_fences} / {@code morefences:<name>_fence_gates},
 * block and item side) which take part in the connection check, so a datapack can pull an extra block into
 * a group. Tags are data though, they only contain what was present while the jar was built; blocks added
 * at runtime are matched by their group reference instead.
 */
public final class FenceGroup {
	// Has to be declared before the groups below, static fields are initialized in declaration order.
	private static final Map<String, FenceGroup> GROUPS = new LinkedHashMap<>();

	/**
	 * How much of the burn time of the raw materials survives crafting, taken from the vanilla wooden
	 * fences ({@code u} is the base smelt time, the ratios do not depend on it):
	 *
	 * <pre>
	 * wooden fence:      4 planks (1.5u) + 2 sticks (0.5u) = 7u  -&gt; 3 fences (1.5u) = 4.5u   keep 9/14
	 * wooden fence gate: 1 plank  (1.5u) + 2 sticks (0.5u) = 2.5u -&gt; 1 gate   (1.5u)        keep 3/5
	 * </pre>
	 *
	 * Feeding the raw materials of a wooden fence through this formula gives exactly 300 ticks back, which
	 * is what vanilla pays; the wool group ends up with 85 ticks per fence and 60 per fence gate.
	 */
	private static final int FENCE_FUEL_KEPT = 9, FENCE_FUEL_RAW = 14;
	private static final int GATE_FUEL_KEPT = 3, GATE_FUEL_RAW = 5;

	/**
	 * The recipes of {@link top.starwindv.morefences.datagen.MoreFencesRecipeProvider}: {@code W#W / W#W} is
	 * 4 base blocks + 2 rods -&gt; 3 fences and {@code #W# / #W#} is 1 base block + 2 rods -&gt; 1 gate. Keep
	 * in sync with the patterns there.
	 */
	private static final int FENCE_BASE_MATERIALS = 4, FENCE_ROD_MATERIALS = 2, FENCE_PRODUCTS = 3;
	private static final int GATE_BASE_MATERIALS = 1, GATE_ROD_MATERIALS = 2, GATE_PRODUCTS = 1;

	/**
	 * Glass and every stained / tinted glass block. Neither glass nor glass panes are fuel in vanilla, so
	 * this group stays non flammable.
	 */
	public static final FenceGroup GLASS = register("glass", SoundType.GLASS, MoreFencesContent::paneFor, true);

	/**
	 * The sixteen wool blocks; string takes the role of the stick of the vanilla recipes. Wool burns like
	 * the planks do (half a base smelt time), string does not burn at all.
	 */
	public static final FenceGroup WOOL = register("wool", SoundType.WOOL, base -> Items.STRING, false)
		.fuelFromMaterials(baseSmeltTime -> baseSmeltTime / 2, baseSmeltTime -> 0);

	private final String name;
	private final SoundType sound;
	private final Function<Block, Item> rodFor;
	private final boolean translucent;
	private final TagKey<Block> fenceTag;
	private final TagKey<Block> fenceGateTag;
	private final TagKey<Item> fenceItemTag;
	private final TagKey<Item> fenceGateItemTag;
	private @Nullable WoodType gateWoodType;
	private IntUnaryOperator fenceBurnTime = baseSmeltTime -> 0;
	private IntUnaryOperator gateBurnTime = baseSmeltTime -> 0;

	private FenceGroup(final String name, final SoundType sound, final Function<Block, Item> rodFor, final boolean translucent) {
		this.name = name;
		this.sound = sound;
		this.rodFor = rodFor;
		this.translucent = translucent;

		Identifier fences = MoreFences.id(name + "_fences");
		this.fenceTag = TagKey.create(Registries.BLOCK, fences);
		this.fenceItemTag = TagKey.create(Registries.ITEM, fences);

		Identifier fenceGates = MoreFences.id(name + "_fence_gates");
		this.fenceGateTag = TagKey.create(Registries.BLOCK, fenceGates);
		this.fenceGateItemTag = TagKey.create(Registries.ITEM, fenceGates);
	}

	/**
	 * Registers a new material family. Must be called while the game starts up (a {@code "main"} entrypoint),
	 * before the tags are loaded.
	 *
	 * @param name       the group name, used for the tags ({@code morefences:<name>_fences}) and the recipe
	 *                   book grouping. Must be a valid identifier path.
	 * @param sound      the sound the fence gate of this group makes when it is placed or broken
	 * @param rodFor     returns the item that plays the role of the stick in the vanilla fence recipes for
	 *                   the given base block ({@code #} in {@code W#W})
	 * @param translucent whether the fences of this group are rendered in the translucent layer, which is
	 *                   what glass needs and wool does not
	 * @return the registered group
	 */
	public static FenceGroup register(final String name, final SoundType sound, final Function<Block, Item> rodFor, final boolean translucent) {
		if (!Identifier.isValidPath(name)) {
			throw new IllegalArgumentException("'" + name + "' is not a valid fence group name");
		}

		FenceGroup existing = GROUPS.get(name);

		if (existing != null) {
			throw new IllegalStateException("A fence group named '" + name + "' is already registered");
		}

		FenceGroup group = new FenceGroup(name, sound, rodFor, translucent);
		GROUPS.put(name, group);
		return group;
	}

	/**
	 * @return every registered group, in registration order.
	 */
	public static List<FenceGroup> all() {
		return List.copyOf(GROUPS.values());
	}

	/**
	 * @return the group with the given name, or {@code null}.
	 */
	public static @Nullable FenceGroup byName(final String name) {
		return GROUPS.get(name);
	}

	/**
	 * Whether the block state is a fence of this very group - either because it is tagged with this group's
	 * tag or because it was created for this group. The class check keeps fences that were registered at
	 * runtime working even though tags are data and therefore only cover what was present at build time.
	 */
	public boolean matchesFence(final BlockState state) {
		return state.is(this.fenceTag) || state.getBlock() instanceof MoreFenceBlock fence && fence.group() == this;
	}

	/**
	 * The counterpart of {@link #matchesFence(BlockState)} for fence gates.
	 */
	public boolean matchesFenceGate(final BlockState state) {
		return state.is(this.fenceGateTag) || state.getBlock() instanceof MoreFenceGateBlock gate && gate.group() == this;
	}

	/**
	 * The item used as {@code #} in this group's recipes for the given base block.
	 */
	public Item rodFor(final Block base) {
		return this.rodFor.apply(base);
	}

	/**
	 * Makes the fences of this group burn, with the given burn times in ticks. Groups are non flammable by
	 * default, which is what the glass group relies on.
	 *
	 * @param fenceBurnTime how long one fence burns
	 * @param gateBurnTime  how long one fence gate burns
	 */
	public FenceGroup fuel(final int fenceBurnTime, final int gateBurnTime) {
		this.fenceBurnTime = baseSmeltTime -> fenceBurnTime;
		this.gateBurnTime = baseSmeltTime -> gateBurnTime;
		return this;
	}

	/**
	 * Makes the fences of this group burn, splitting the burn time of their raw materials over the products
	 * with the loss the vanilla wooden fences have - see {@link #FENCE_FUEL_KEPT}. That keeps a group honest:
	 * burning the materials directly is always at least as good as crafting and burning the fence.
	 *
	 * @param baseBurnTime how long one base block burns, given the base smelt time
	 * @param rodBurnTime  how long one rod burns, given the base smelt time
	 */
	public FenceGroup fuelFromMaterials(final IntUnaryOperator baseBurnTime, final IntUnaryOperator rodBurnTime) {
		this.fenceBurnTime = baseSmeltTime -> {
			int raw = FENCE_BASE_MATERIALS * baseBurnTime.applyAsInt(baseSmeltTime) + FENCE_ROD_MATERIALS * rodBurnTime.applyAsInt(baseSmeltTime);
			return raw * FENCE_FUEL_KEPT / (FENCE_FUEL_RAW * FENCE_PRODUCTS);
		};
		this.gateBurnTime = baseSmeltTime -> {
			int raw = GATE_BASE_MATERIALS * baseBurnTime.applyAsInt(baseSmeltTime) + GATE_ROD_MATERIALS * rodBurnTime.applyAsInt(baseSmeltTime);
			return raw * GATE_FUEL_KEPT / (GATE_FUEL_RAW * GATE_PRODUCTS);
		};
		return this;
	}

	/**
	 * @return how long one fence of this group burns, in ticks; {@code 0} means it is no fuel at all.
	 */
	public int fenceBurnTime(final int baseSmeltTime) {
		return this.fenceBurnTime.applyAsInt(baseSmeltTime);
	}

	/**
	 * @return how long one fence gate of this group burns, in ticks; {@code 0} means it is no fuel at all.
	 */
	public int gateBurnTime(final int baseSmeltTime) {
		return this.gateBurnTime.applyAsInt(baseSmeltTime);
	}

	/**
	 * The recipe book group of this group's recipes, mirroring the vanilla {@code wooden_fence} /
	 * {@code wooden_fence_gate} pair.
	 */
	public String recipeGroup(final boolean fenceGate) {
		return fenceGate ? this.name + "_fence_gate" : this.name + "_fence";
	}

	public String name() {
		return this.name;
	}

	public Identifier id() {
		return MoreFences.id(this.name);
	}

	public SoundType sound() {
		return this.sound;
	}

	/**
	 * Whether the fences of this group are rendered in the translucent layer, like the vanilla glass models.
	 */
	public boolean isTranslucent() {
		return this.translucent;
	}

	public TagKey<Block> fenceTag() {
		return this.fenceTag;
	}

	public TagKey<Block> fenceGateTag() {
		return this.fenceGateTag;
	}

	public TagKey<Item> fenceItemTag() {
		return this.fenceItemTag;
	}

	public TagKey<Item> fenceGateItemTag() {
		return this.fenceGateItemTag;
	}

	/**
	 * The {@link WoodType} that gives the fence gate of this group its sounds. A fence gate only uses the
	 * {@link SoundType} and the opening / closing sound of it, so one per group is enough.
	 */
	public WoodType gateWoodType() {
		if (this.gateWoodType == null) {
			this.gateWoodType = new WoodType(this.name, BlockSetType.OAK, this.sound, this.sound, SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);
		}

		return this.gateWoodType;
	}

	@Override
	public String toString() {
		return MoreFences.MOD_ID + ":" + this.name;
	}
}
