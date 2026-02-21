package dev.hephaestus.glowcase.datagen.client;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.ItemAcceptorBlock;
import dev.hephaestus.glowcase.client.render.item.tint.GlowcaseTintSource;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GlowcaseModelGenerator extends FabricModelProvider {
	public static final TexturedModel.Provider PARTICLE_FACTORY = TexturedModel.createDefault(block -> TextureMapping.cube(Blocks.BEDROCK), ModelTemplates.PARTICLE_ONLY);

	public GlowcaseModelGenerator(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
		blockStateModelGenerator.createTrivialBlock(Glowcase.HYPERLINK_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.CONFIG_LINK_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.ITEM_DISPLAY_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.ITEM_PROVIDER_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.PARTICLE_DISPLAY.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.SOUND_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.TEXT_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.POPUP_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.SCREEN_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.SPRITE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.RECIPE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.OUTLINE_BLOCK.get(), PARTICLE_FACTORY);
		blockStateModelGenerator.createTrivialBlock(Glowcase.ENTITY_DISPLAY_BLOCK.get(), PARTICLE_FACTORY);

		registerItemAcceptor(blockStateModelGenerator);
	}

	@Override
	public void generateItemModels(ItemModelGenerators itemModelGenerator) {
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
		itemModelGenerator.generateFlatItem(Glowcase.RECIPE_BLOCK_ITEM.get(), ModelTemplates.FLAT_ITEM);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.OUTLINE_BLOCK_ITEM.get(), 0xFFFFFFFF);
		registerGlowcaseDyeable(itemModelGenerator, Glowcase.ENTITY_DISPLAY_BLOCK_ITEM.get(), 0xFFFFFFFF);

		// Simple items
		itemModelGenerator.generateFlatItem(Glowcase.LOCK_ITEM.get(), ModelTemplates.FLAT_ITEM);
		itemModelGenerator.generateDyedItem(Glowcase.COLLECTION_CASE_ITEM.get(), 0xFFFFFFFF);
		itemModelGenerator.generateFlatItem(Glowcase.TABLET_ITEM.get(), ModelTemplates.FLAT_ITEM);
		itemModelGenerator.generateFlatItem(Glowcase.NOTE_ITEM.get(), ModelTemplates.FLAT_ITEM);
	}

	private void registerItemAcceptor(BlockModelGenerators generator) {
		ItemAcceptorBlock block = Glowcase.ITEM_ACCEPTOR_BLOCK.get();
		MultiVariant weightedVariant = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block));
		MultiVariant weightedVariant2 = BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(block, "_on"));
		generator.blockStateOutput
			.accept(
				MultiVariantGenerator.dispatch(block)
					.with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.POWERED, weightedVariant2, weightedVariant))
					.with(
						PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
							.select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
							.select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
							.select(Direction.NORTH, BlockModelGenerators.NOP)
							.select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
					)
			);
	}

	public final void registerGlowcaseDyeable(ItemModelGenerators generator, Item item, int defaultColor) {
		Identifier identifier = generator.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
		generator.itemModelOutput.accept(item, ItemModelUtils.tintedModel(identifier, new GlowcaseTintSource(defaultColor)));
	}
}
