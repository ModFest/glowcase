package dev.hephaestus.glowcase;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.hephaestus.glowcase.client.render.bakedbe.section.RenderSectionPos;
import dev.hephaestus.glowcase.util.DataFlow;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.coordinates.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

class DevCommands {
	static void registerDevCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			copyFill(dispatcher);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			rebake(dispatcher);
		});
	}

	private static void rebake(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("rebake")
			.then(ClientCommands.literal("render_section")
				.then(ClientCommands.argument("pos", BlockPosArgument.blockPos())
					.suggests((context, builder) -> {
						Vec3 position = context.getSource().getPosition();
						RenderSectionPos pos = new RenderSectionPos(BlockPos.containing(position));
						return SharedSuggestionProvider.suggest(List.of(pos.toCommandString()), builder);
					})
					.executes(context -> {
						var source = context.getSource();
						var coordinates = context.getArgument("pos", Coordinates.class);
						Vec3 sourcePos = source.getPosition();
						BlockPos pos;
						if (coordinates instanceof WorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z)) {
							pos = BlockPos.containing(x.get(sourcePos.x), y.get(sourcePos.y), z.get(sourcePos.z));
						} else if (coordinates instanceof LocalCoordinates(double left, double up, double forwards)){
							pos = BlockPos.containing(Vec3.applyLocalCoordinatesToRotation(source.getRotation(), new Vec3(left, up, forwards)).add(sourcePos.x, sourcePos.y, sourcePos.z));
						} else {
							throw new IllegalStateException("Invalid input");
						}

						RenderSectionPos sectionPos = RenderSectionPos.fromVec3i(pos);
						source.getLevel().setSectionRangeDirty(sectionPos.getX(), sectionPos.getY(), sectionPos.getZ(), sectionPos.getX(), sectionPos.getY(), sectionPos.getZ());

						return 0;
					})
				)
			).then(ClientCommands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> {
					var source = context.getSource();
					var coordinates = context.getArgument("pos", Coordinates.class);
					Vec3 sourcePos = source.getPosition();
					BlockPos pos;
					if (coordinates instanceof WorldCoordinates(WorldCoordinate x, WorldCoordinate y, WorldCoordinate z)) {
						pos = BlockPos.containing(x.get(sourcePos.x), y.get(sourcePos.y), z.get(sourcePos.z));
					} else if (coordinates instanceof LocalCoordinates(double left, double up, double forwards)){
						pos = BlockPos.containing(Vec3.applyLocalCoordinatesToRotation(source.getRotation(), new Vec3(left, up, forwards)).add(sourcePos.x, sourcePos.y, sourcePos.z));
					} else {
						throw new IllegalStateException("Invalid input");
					}

					ClientLevel level = source.getLevel();
					BlockState state = level.getBlockState(pos);
					level.sendBlockUpdated(pos, state, state, 0);

					return 0;
				})
			)
		);
	}

	private static void copyFill(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("copyfill")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("render_section")
				.then(Commands.argument("pos", BlockPosArgument.blockPos())
					.suggests((context, builder) -> {
						Vec3 position = context.getSource().getPosition();
						RenderSectionPos pos = new RenderSectionPos(BlockPos.containing(position));
						return SharedSuggestionProvider.suggest(List.of(pos.toCommandString()), builder);
					})
					.then(Commands.argument("source", BlockPosArgument.blockPos())
						.then(Commands.argument("strict", BoolArgumentType.bool())
							.executes(context -> {
								CommandSourceStack source = context.getSource();
								BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
								BlockPos sourcePos = BlockPosArgument.getLoadedBlockPos(context, "source");
								ServerLevel level = source.getLevel();
								BlockState blockState = level.getBlockState(sourcePos);
								var nbt = DataFlow.nullable(level.getBlockEntity(sourcePos), blockEntity -> blockEntity.saveWithoutMetadata(source.registryAccess()));
								BlockInput input = new BlockInput(blockState, new HashSet<>(blockState.getProperties()), nbt);
								return fillBlocks(
									source,
									RenderSectionPos.fromVec3i(pos).structuralBoundingBox(),
									input,
									BoolArgumentType.getBool(context, "strict")
								);
							})
						)
					)
				)
			).then(Commands.argument("from", BlockPosArgument.blockPos())
				.then(Commands.argument("to", BlockPosArgument.blockPos())
					.then(Commands.argument("source", BlockPosArgument.blockPos())
						.then(Commands.argument("strict", BoolArgumentType.bool())
							.executes(context -> {
								CommandSourceStack source = context.getSource();
								BlockPos sourcePos = BlockPosArgument.getLoadedBlockPos(context, "source");
								ServerLevel level = source.getLevel();
								BlockState blockState = level.getBlockState(sourcePos);
								var nbt = DataFlow.nullable(level.getBlockEntity(sourcePos), blockEntity -> blockEntity.saveWithoutMetadata(source.registryAccess()));
								BlockInput input = new BlockInput(blockState, new HashSet<>(blockState.getProperties()), nbt);
								return fillBlocks(
									source,
									BoundingBox.fromCorners(
										BlockPosArgument.getLoadedBlockPos(context, "from"),
										BlockPosArgument.getLoadedBlockPos(context, "to")
									),
									input,
									BoolArgumentType.getBool(context, "strict")
								);
							})
						)
					)
				)
			)
		);
	}

	private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((max, count) -> Component.translatableEscape("commands.fill.toobig", max, count));
	private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

	private static int fillBlocks(
		final CommandSourceStack source,
		final BoundingBox region,
		final BlockInput target,
		final boolean strict
	) throws CommandSyntaxException {
		long area = (long) region.getXSpan() * region.getYSpan() * region.getZSpan();
		int limit = source.getLevel().getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
		if (area > limit) {
			throw ERROR_AREA_TOO_LARGE.create(limit, area);
		} else {
			record UpdatedPosition(BlockPos pos, BlockState oldState) {
			}

			List<UpdatedPosition> updatePositions = Lists.<UpdatedPosition>newArrayList();
			ServerLevel level = source.getLevel();
			if (level.isDebug()) {
				throw ERROR_FAILED.create();
			} else {
				int count = 0;

				for (BlockPos pos : BlockPos.betweenClosed(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ())) {
					BlockState oldState = level.getBlockState(pos);

					if (target != null && target.place(level, pos, 2 | (strict ? 816 : 256))) {
						if (!strict) {
							updatePositions.add(new UpdatedPosition(pos.immutable(), oldState));
						}

						count++;
					}
				}

				for (UpdatedPosition updatedPosition : updatePositions) {
					level.updateNeighboursOnBlockSet(updatedPosition.pos, updatedPosition.oldState);
				}

				if (count == 0) {
					throw ERROR_FAILED.create();
				} else {
					int finalCount = count;
					source.sendSuccess(() -> Component.translatable("commands.fill.success", finalCount), true);
					return count;
				}
			}
		}
	}
}
