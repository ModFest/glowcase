package dev.hephaestus.glowcase;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.hephaestus.glowcase.client.render.bakedbe.section.RenderSectionPos;
import dev.hephaestus.glowcase.util.DataFlow;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

import static net.minecraft.commands.Commands.*;

class DevCommands {
	static void registerDevCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			copyFill(dispatcher);
		});
	}

	private static void copyFill(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("copyfill")
			.requires(hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(literal("render_section")
				.then(argument("pos", BlockPosArgument.blockPos())
					.suggests((context, builder) -> {
						Vec3 position = context.getSource().getPosition();
						RenderSectionPos pos = new RenderSectionPos(BlockPos.containing(position));
						return SharedSuggestionProvider.suggest(List.of(pos.toCommandString()), builder);
					})
					.then(argument("source", BlockPosArgument.blockPos())
						.then(argument("strict", BoolArgumentType.bool())
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
			).then(argument("from", BlockPosArgument.blockPos())
				.then(argument("to", BlockPosArgument.blockPos())
					.then(argument("source", BlockPosArgument.blockPos())
						.then(argument("strict", BoolArgumentType.bool())
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
