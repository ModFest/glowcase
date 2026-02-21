package dev.hephaestus.glowcase;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.block.*;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
//import dev.hephaestus.glowcase.compat.PolydexCompatibility;
import dev.hephaestus.glowcase.item.CollectionCaseItem;
import dev.hephaestus.glowcase.item.LockItem;
import dev.hephaestus.glowcase.item.NoteItem;
import dev.hephaestus.glowcase.item.TabletItem;
import dev.hephaestus.glowcase.item.component.CollectionComponent;
import dev.hephaestus.glowcase.item.component.NoteComponent;
import dev.hephaestus.glowcase.item.component.TabletComponents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class Glowcase implements ModInitializer {
	public static final String MODID = "glowcase";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static final GlowcaseConfig CONFIG = GlowcaseConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", MODID, GlowcaseConfig.class);
	public static GlowcaseCommonProxy proxy = new GlowcaseCommonProxy(); //Overridden in GlowcaseClient

	public static final TagKey<Item> ITEM_TAG = TagKey.create(Registries.ITEM, id("items"));

	public static final Supplier<HyperlinkBlock> HYPERLINK_BLOCK = registerBlock("hyperlink_block", HyperlinkBlock::new);
	public static final Supplier<BlockItem> HYPERLINK_BLOCK_ITEM = registerBlockItem("hyperlink_block", HYPERLINK_BLOCK);
	public static final Supplier<BlockEntityType<HyperlinkBlockEntity>> HYPERLINK_BLOCK_ENTITY = registerBlockEntity("hyperlink_block", () -> FabricBlockEntityTypeBuilder.create(HyperlinkBlockEntity::new, HYPERLINK_BLOCK.get()).build(null));

	public static final Supplier<ConfigLinkBlock> CONFIG_LINK_BLOCK = registerBlock("config_link_block", ConfigLinkBlock::new);
	public static final Supplier<BlockItem> CONFIG_LINK_BLOCK_ITEM = registerBlockItem("config_link_block", CONFIG_LINK_BLOCK);
	public static final Supplier<BlockEntityType<ConfigLinkBlockEntity>> CONFIG_LINK_BLOCK_ENTITY = registerBlockEntity("config_link_block", () -> FabricBlockEntityTypeBuilder.create(ConfigLinkBlockEntity::new, CONFIG_LINK_BLOCK.get()).build(null));

	public static final Supplier<ItemDisplayBlock> ITEM_DISPLAY_BLOCK = registerBlock("item_display_block", ItemDisplayBlock::new);
	public static final Supplier<BlockItem> ITEM_DISPLAY_BLOCK_ITEM = registerBlockItem("item_display_block", ITEM_DISPLAY_BLOCK);
	public static final Supplier<BlockEntityType<ItemDisplayBlockEntity>> ITEM_DISPLAY_BLOCK_ENTITY = registerBlockEntity("item_display_block", () -> FabricBlockEntityTypeBuilder.create(ItemDisplayBlockEntity::new, ITEM_DISPLAY_BLOCK.get()).build(null));

	public static final Supplier<ItemProviderBlock> ITEM_PROVIDER_BLOCK = registerBlock("item_provider_block", ItemProviderBlock::new);
	public static final Supplier<BlockItem> ITEM_PROVIDER_BLOCK_ITEM = registerBlockItem("item_provider_block", ITEM_PROVIDER_BLOCK);
	public static final Supplier<BlockEntityType<ItemProviderBlockEntity>> ITEM_PROVIDER_BLOCK_ENTITY = registerBlockEntity("item_provider_block", () -> FabricBlockEntityTypeBuilder.create(ItemProviderBlockEntity::new, ITEM_PROVIDER_BLOCK.get()).build(null));

	public static final Supplier<ParticleDisplayBlock> PARTICLE_DISPLAY = registerBlock("particle_display", ParticleDisplayBlock::new);
	public static final Supplier<BlockItem> PARTICLE_DISPLAY_ITEM = registerBlockItem("particle_display", PARTICLE_DISPLAY);
	public static final Supplier<BlockEntityType<ParticleDisplayBlockEntity>> PARTICLE_DISPLAY_BLOCK_ENTITY = registerBlockEntity("particle_display", () -> FabricBlockEntityTypeBuilder.create(ParticleDisplayBlockEntity::new, PARTICLE_DISPLAY.get()).build(null));

	public static final Supplier<SoundPlayerBlock> SOUND_BLOCK = registerBlock("sound_block", SoundPlayerBlock::new);
	public static final Supplier<BlockItem> SOUND_BLOCK_ITEM = registerBlockItem("sound_block", SOUND_BLOCK);
	public static final Supplier<BlockEntityType<SoundPlayerBlockEntity>> SOUND_BLOCK_ENTITY = registerBlockEntity("sound_block", () -> FabricBlockEntityTypeBuilder.create(SoundPlayerBlockEntity::new, SOUND_BLOCK.get()).build(null));

	public static final Supplier<TextBlock> TEXT_BLOCK = registerBlock("text_block", TextBlock::new);
	public static final Supplier<BlockItem> TEXT_BLOCK_ITEM = registerBlockItem("text_block", TEXT_BLOCK);
	public static final Supplier<BlockEntityType<TextBlockEntity>> TEXT_BLOCK_ENTITY = registerBlockEntity("text_block", () -> FabricBlockEntityTypeBuilder.create(TextBlockEntity::new, TEXT_BLOCK.get()).build(null));

	public static final Supplier<PopupBlock> POPUP_BLOCK = registerBlock("popup_block", PopupBlock::new);
	public static final Supplier<BlockItem> POPUP_BLOCK_ITEM = registerBlockItem("popup_block", POPUP_BLOCK);
	public static final Supplier<BlockEntityType<PopupBlockEntity>> POPUP_BLOCK_ENTITY = registerBlockEntity("popup_block", () -> FabricBlockEntityTypeBuilder.create(PopupBlockEntity::new, POPUP_BLOCK.get()).build(null));

	public static final Supplier<ScreenBlock> SCREEN_BLOCK = registerBlock("screen_block", ScreenBlock::new);
	public static final Supplier<BlockItem> SCREEN_BLOCK_ITEM = registerBlockItem("screen_block", SCREEN_BLOCK);
	public static final Supplier<BlockEntityType<ScreenBlockEntity>> SCREEN_BLOCK_ENTITY = registerBlockEntity("screen_block", () -> FabricBlockEntityTypeBuilder.create(ScreenBlockEntity::new, SCREEN_BLOCK.get()).build(null));

	public static final Supplier<SpriteBlock> SPRITE_BLOCK = registerBlock("sprite_block", SpriteBlock::new);
	public static final Supplier<BlockItem> SPRITE_BLOCK_ITEM = registerBlockItem("sprite_block", SPRITE_BLOCK);
	public static final Supplier<BlockEntityType<SpriteBlockEntity>> SPRITE_BLOCK_ENTITY = registerBlockEntity("sprite_block", () -> FabricBlockEntityTypeBuilder.create(SpriteBlockEntity::new, SPRITE_BLOCK.get()).build(null));

	public static final Supplier<RecipeBlock> RECIPE_BLOCK = registerBlock("recipe_block", RecipeBlock::new);
	public static final Supplier<BlockItem> RECIPE_BLOCK_ITEM = registerBlockItem("recipe_block", RECIPE_BLOCK);
	public static final Supplier<BlockEntityType<RecipeBlockEntity>> RECIPE_BLOCK_ENTITY = registerBlockEntity("recipe_block", () -> FabricBlockEntityTypeBuilder.create(RecipeBlockEntity::new, RECIPE_BLOCK.get()).build(null));

	public static final Supplier<OutlineBlock> OUTLINE_BLOCK = registerBlock("outline_block", OutlineBlock::new);
	public static final Supplier<BlockItem> OUTLINE_BLOCK_ITEM = registerBlockItem("outline_block", OUTLINE_BLOCK);
	public static final Supplier<BlockEntityType<OutlineBlockEntity>> OUTLINE_BLOCK_ENTITY = registerBlockEntity("outline_block", () -> FabricBlockEntityTypeBuilder.create(OutlineBlockEntity::new, OUTLINE_BLOCK.get()).build(null));

	public static final Supplier<ItemAcceptorBlock> ITEM_ACCEPTOR_BLOCK = registerBlock("item_acceptor_block", ItemAcceptorBlock::new);
	public static final Supplier<BlockItem> ITEM_ACCEPTOR_BLOCK_ITEM = registerBlockItem("item_acceptor_block", ITEM_ACCEPTOR_BLOCK);
	public static final Supplier<BlockEntityType<ItemAcceptorBlockEntity>> ITEM_ACCEPTOR_BLOCK_ENTITY = registerBlockEntity("item_acceptor_block", () -> FabricBlockEntityTypeBuilder.create(ItemAcceptorBlockEntity::new, ITEM_ACCEPTOR_BLOCK.get()).build(null));

	public static final Supplier<Item> LOCK_ITEM = registerItem("lock", LockItem::new);

	public static final Supplier<DataComponentType<CollectionComponent>> COLLECTION_COMPONENT = registerComponent("collection", () -> CollectionComponent.TYPE);
	public static final Supplier<Item> COLLECTION_CASE_ITEM = registerItem("collection_case", (settings) -> new CollectionCaseItem(settings.component(COLLECTION_COMPONENT.get(), new CollectionComponent()).component(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFF))));

	public static final Supplier<Item> TABLET_ITEM = registerItem("tablet", TabletItem::new);
	public static final Supplier<DataComponentType<Pair<UUID, BlockPos>>> LINKED_SCREEN_COMPONENT = registerComponent("linked_screen", () -> TabletComponents.LINKED_SCREEN_TYPE);
	public static final Supplier<DataComponentType<Integer>> CURRENT_SLIDE_COMPONENT = registerComponent("current_slide", () -> TabletComponents.CURRENT_SLIDE_TYPE);
	public static final Supplier<DataComponentType<List<Pair<String, String>>>> SLIDESHOW_COMPONENT = registerComponent("slideshow", () -> TabletComponents.SLIDESHOW_COMPONENT_TYPE);

	public static final Supplier<Item> NOTE_ITEM = registerItem("note", NoteItem::new);
	public static final Supplier<DataComponentType<NoteComponent>> NOTE_COMPONENT = registerComponent("note", () -> NoteComponent.TYPE);

	public static final Supplier<EntityDisplayBlock> ENTITY_DISPLAY_BLOCK = registerBlock("entity_display_block", EntityDisplayBlock::new);
	public static final Supplier<BlockItem> ENTITY_DISPLAY_BLOCK_ITEM = registerBlockItem("entity_display_block", ENTITY_DISPLAY_BLOCK);
	public static final Supplier<BlockEntityType<EntityDisplayBlockEntity>> ENTITY_DISPLAY_BLOCK_ENTITY = registerBlockEntity("entity_display_block", () -> FabricBlockEntityTypeBuilder.create(EntityDisplayBlockEntity::new, ENTITY_DISPLAY_BLOCK.get()).build(null));

	public static final Supplier<CreativeModeTab> ITEM_GROUP = registerItemGroup("items", () -> FabricCreativeModeTab.builder()
		.title(Component.translatable("itemGroup.glowcase.items"))
		.icon(() -> new ItemStack(SPRITE_BLOCK_ITEM.get()))
		.displayItems((displayContext, entries) -> {
			entries.accept(TEXT_BLOCK_ITEM.get());
			entries.accept(ENTITY_DISPLAY_BLOCK_ITEM.get());
			entries.accept(ITEM_DISPLAY_BLOCK_ITEM.get());
			entries.accept(RECIPE_BLOCK_ITEM.get());
			entries.accept(SPRITE_BLOCK_ITEM.get());
			entries.accept(PARTICLE_DISPLAY_ITEM.get());
			entries.accept(SOUND_BLOCK_ITEM.get());
			entries.accept(SCREEN_BLOCK_ITEM.get());
			entries.accept(OUTLINE_BLOCK_ITEM.get());
			entries.accept(HYPERLINK_BLOCK_ITEM.get());
			entries.accept(CONFIG_LINK_BLOCK_ITEM.get());
			entries.accept(POPUP_BLOCK_ITEM.get());
			entries.accept(ITEM_PROVIDER_BLOCK_ITEM.get());
			entries.accept(ITEM_ACCEPTOR_BLOCK_ITEM.get());
			entries.accept(LOCK_ITEM.get());
			entries.accept(COLLECTION_CASE_ITEM.get());
			entries.accept(TABLET_ITEM.get());
			entries.accept(NOTE_ITEM.get());
		})
		.build()
	);

	public static Identifier id(String... path) {
		return Identifier.fromNamespaceAndPath(MODID, String.join("/", path));
	}

	public static <T extends Block> Supplier<T> registerBlock(String path, Function<BlockBehaviour.Properties, T> supplier) {
		Identifier identifier = id(path);
		BlockBehaviour.Properties settings = GlowcaseBlock.defaultSettings().setId(ResourceKey.create(Registries.BLOCK, identifier));
		
		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.BLOCK, identifier, supplier.apply(settings)));
	}

	public static Supplier<BlockItem> registerBlockItem(String path, Supplier<? extends Block> block) {
		return registerBlockItem(path, block, settings -> {});
	}

	public static Supplier<BlockItem> registerBlockItem(String path, Supplier<? extends Block> blockSupplier, Consumer<Item.Properties> seetingsConsumer) {
		Identifier identifier = id(path);
		Block block = blockSupplier.get();
		Item.Properties settings = defaultItemSettings().setId(ResourceKey.create(Registries.ITEM, identifier));
		seetingsConsumer.accept(settings);
		BlockItem blockItem;
		if (block instanceof GlowcaseBlock glowcaseBlock) {
			blockItem = new BlockItem(block, settings) {
				@Override
				public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
					glowcaseBlock.appendTooltip(stack, context, displayComponent, textConsumer, type);
				}
			};
		} else {
			blockItem = new BlockItem(block, settings);
		}

		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.ITEM, identifier, blockItem));
	}

	public static <T extends Item> Supplier<T> registerItem(String path, Function<Item.Properties, T> supplier) {
		Identifier identifier = id(path);
		Item.Properties settings = defaultItemSettings().setId(ResourceKey.create(Registries.ITEM, identifier));

		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.ITEM, identifier, supplier.apply(settings)));
	}

	public static <T extends DataComponentType<U>, U> Supplier<T> registerComponent(String path, Supplier<T> supplier) {
		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(path), supplier.get()));
	}

	public static <T extends CreativeModeTab> Supplier<T> registerItemGroup(String path, Supplier<T> supplier) {
		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id(path), supplier.get()));
	}

	public static <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntity(String path, Supplier<BlockEntityType<T>> supplier) {
		return Suppliers.ofInstance(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(path), supplier.get()));
	}
	
	private static Item.Properties defaultItemSettings() {
		return new Item.Properties().stacksTo(1);
	}

	@Override
	public void onInitialize() {
		GlowcaseNetworking.init();

//		if (FabricLoader.getInstance().isModLoaded("polydex2")) {
//			PolydexCompatibility.onInitialize();
//		}

		// Never make this command available outside of dev
		/*if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			if (FabricLoader.getInstance().isModLoaded("emi")) {
				EmiUtils.registerDevCommands();
			}
		}*/
	}
}
