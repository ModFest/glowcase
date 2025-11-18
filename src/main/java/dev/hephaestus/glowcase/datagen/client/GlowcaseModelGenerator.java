package dev.hephaestus.glowcase.datagen.client;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemAcceptorBlock;
//import dev.hephaestus.glowcase.client.render.item.tint.GlowcaseTintSource;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.data.*;
import net.minecraft.client.render.model.json.WeightedVariant;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class GlowcaseModelGenerator extends FabricModelProvider {
	public static final TexturedModel.Factory PARTICLE_FACTORY = TexturedModel.makeFactory(block -> TextureMap.all(Blocks.BEDROCK), Models.PARTICLE);

	public GlowcaseModelGenerator(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
		blockStateModelGenerator.registerSingleton(Glowcase.HYPERLINK_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.CONFIG_LINK_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.ITEM_DISPLAY_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.ITEM_PROVIDER_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.PARTICLE_DISPLAY.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.SOUND_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.TEXT_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.POPUP_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.SCREEN_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.SPRITE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.RECIPE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.OUTLINE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.registerSingleton(Glowcase.ENTITY_DISPLAY_BLOCK.get(), PARTICLE_FACTORY);

		registerItemAcceptor(blockStateModelGenerator);
	}

	@Override
	public void generateItemModels(ItemModelGenerator itemModelGenerator) {
		// Block items
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.HYPERLINK_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.CONFIG_LINK_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.ITEM_DISPLAY_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.ITEM_PROVIDER_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.PARTICLE_DISPLAY_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.SOUND_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.TEXT_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.POPUP_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.SCREEN_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.SPRITE_BLOCK_ITEM.get(), 0xFFFFFFFF);
		itemModelGenerator.register(Glowcase.RECIPE_BLOCK_ITEM.get(), Models.GENERATED);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.OUTLINE_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.ENTITY_DISPLAY_BLOCK_ITEM.get(), 0xFFFFFFFF);

		// Simple items
		itemModelGenerator.register(Glowcase.LOCK_ITEM.get(), Models.GENERATED);
		itemModelGenerator.registerDyeable(Glowcase.COLLECTION_CASE_ITEM.get(), 0xFFFFFFFF);
		itemModelGenerator.register(Glowcase.TABLET_ITEM.get(), Models.GENERATED);
		itemModelGenerator.register(Glowcase.NOTE_ITEM.get(), Models.GENERATED);
	}

	private void registerItemAcceptor(BlockStateModelGenerator generator) {
		ItemAcceptorBlock block = Glowcase.ITEM_ACCEPTOR_BLOCK.get();
		WeightedVariant weightedVariant = BlockStateModelGenerator.createWeightedVariant(ModelIds.getBlockModelId(block));
		WeightedVariant weightedVariant2 = BlockStateModelGenerator.createWeightedVariant(ModelIds.getBlockSubModelId(block, "_on"));
		generator.blockStateCollector
			.accept(
				VariantsBlockModelDefinitionCreator.of(block)
					.with(BlockStateModelGenerator.createBooleanModelMap(Properties.POWERED, weightedVariant2, weightedVariant))
					.coordinate(
						BlockStateVariantMap.operations(Properties.HORIZONTAL_FACING)
							.register(Direction.WEST, BlockStateModelGenerator.ROTATE_Y_270)
							.register(Direction.SOUTH, BlockStateModelGenerator.ROTATE_Y_180)
							.register(Direction.NORTH, BlockStateModelGenerator.NO_OP)
							.register(Direction.EAST, BlockStateModelGenerator.ROTATE_Y_90)
					)
			);
	}

	public final void registerGlowcaseDyeable(ItemModelGenerator generator, Item item, int defaultColor) {
		Identifier identifier = generator.upload(item, Models.GENERATED);
		// Todo
		generator.output.accept(item, ItemModels.tinted(identifier/*,/* new GlowcaseTintSource(defaultColor)*/));
	}
}
