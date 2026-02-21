package dev.hephaestus.glowcase.util;

//import dev.emi.emi.api.EmiApi;
//import dev.emi.emi.api.recipe.EmiRecipe;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Objects;

public class EmiUtils {
	public static final NotSoConstant<List<Identifier>> RECIPE_LIST = new NotSoConstant<>(() -> List.of()/*EmiApi.getRecipeManager().getRecipes().stream().map(EmiRecipe::getId).filter(Objects::nonNull).toList(), list -> !list.isEmpty()*/);

	/*public static EmiRecipe getRecipe(String recipeString) {
		Identifier recipeId = Identifier.tryParse(recipeString);

		if (recipeId == null) {
			return null;
		}

		return EmiApi.getRecipeManager().getRecipe(recipeId);
	}

	public static void registerDevCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
				CommandManager.literal("randomizerecipes").then(
					CommandManager.argument("from", BlockPosArgumentType.blockPos()).then(
						CommandManager.argument("to", BlockPosArgumentType.blockPos()).executes(context -> {
							ServerCommandSource source = context.getSource();

							List<Identifier> list = EmiUtils.RECIPE_LIST.get();
							ServerWorld world = source.getWorld();

							BlockBox range = BlockBox.create(BlockPosArgumentType.getLoadedBlockPos(context, "from"), BlockPosArgumentType.getLoadedBlockPos(context, "to"));
							for (BlockPos blockPos : BlockPos.iterate(range.getMinX(), range.getMinY(), range.getMinZ(), range.getMaxX(), range.getMaxY(), range.getMaxZ())) {
								BlockEntity blockEntity = world.getBlockEntity(blockPos);
								if (blockEntity instanceof RecipeBlockEntity recipeBlockEntity) {
									recipeBlockEntity.setRecipe(list.get(world.random.nextInt(list.size())).toString());
								}
							}

							return 0;
						})
					)
				)
			);
		});
	}*/
}
