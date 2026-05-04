package dev.hephaestus.glowcase.client.util;

//import dev.emi.emi.api.EmiApi;
//import dev.emi.emi.api.recipe.EmiRecipe;
//import dev.emi.emi.api.widget.Widget;
//import dev.emi.emi.widget.RecipeBackground;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import dev.hephaestus.glowcase.util.RequiresRRVLoaded;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class RRVClientUtils {
	public static void displayRecipe(Identifier recipeId) {
		if (recipeId == null) {
			return;
		}
		ItemViewOverlay.INSTANCE.openRecipeView(recipeId, false);
	}

	public static void updateWidgetHolder(String recipeId, AtomicReference<RequiresRRVLoaded> widgetHolder) {
		/*EmiRecipe recipe = EmiUtils.getRecipe(recipeId);

		GlowcaseWidgetHolder glowcaseWidgetHolder = (GlowcaseWidgetHolder) widgetHolder.get();

		if (recipe == null) {
			if (glowcaseWidgetHolder != null) {
				glowcaseWidgetHolder.clear();
			}
			return;
		}

		if (glowcaseWidgetHolder == null || recipe.getDisplayWidth() != glowcaseWidgetHolder.getWidth() || recipe.getDisplayHeight() != glowcaseWidgetHolder.getHeight()) {
			widgetHolder.set(new GlowcaseWidgetHolder(recipe.getDisplayWidth(), recipe.getDisplayHeight()));
		} else {
			glowcaseWidgetHolder.clear();
		}

		glowcaseWidgetHolder = (GlowcaseWidgetHolder) widgetHolder.get();

		glowcaseWidgetHolder.add(new RecipeBackground(-4, -4, recipe.getDisplayWidth() + 8, recipe.getDisplayHeight() + 8));
		recipe.addWidgets(glowcaseWidgetHolder);*/
	}

	public static void renderEmiRecipe(RequiresRRVLoaded widgetHolder, GuiGraphicsExtractor context, float delta) {
		/*GlowcaseWidgetHolder holder = (GlowcaseWidgetHolder) widgetHolder;

		for (Widget widget : holder.getWidgets()) {
			widget.render(context, -99, -99, delta);
		}*/
	}

	public static int getHolderWidth(RequiresRRVLoaded widgetHolder) {
		//return ((GlowcaseWidgetHolder) widgetHolder).getWidth();
		return 0;
	}

	public static int getHolderHeight(RequiresRRVLoaded widgetHolder) {
		//return ((GlowcaseWidgetHolder) widgetHolder).getHeight();
		return 0;
	}
}
