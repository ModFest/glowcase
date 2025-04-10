package dev.hephaestus.glowcase.util;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.widget.RecipeBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EmiClientUtils {
	private static final BufferBuilderStorage SORRY = new BufferBuilderStorage(1);
	private static Map<String, Framebuffer> FB_CACHE = new HashMap<>();

	public static void displayRecipe(Identifier rid) {
		if (rid == null) {
			return;
		}
		EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(rid);
		if (recipe == null) {
			return;
		}
		EmiApi.displayRecipe(recipe);
	}

	public static boolean renderRecipe(MatrixStack matrices, String recipeString, BlockPos pos) {
		EmiRecipe recipe = getRecipeToDisplay(recipeString, pos);
		if (recipe == null) {
			return false;
		}

		int fullWidth = recipe.getDisplayWidth() + 8;
		int fullHeight = recipe.getDisplayHeight() + 8;
		Framebuffer fb = createFramebuffer(recipe);

		fb.beginRead();
		RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
		RenderSystem.setShaderTexture(0, fb.getColorAttachment());
		RenderSystem.enableDepthTest();
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder builder = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		MatrixStack.Entry entry = matrices.peek();
		float xMin = -0.15f / 16 * fullWidth;
		float xMax =  0.15f / 16 * fullWidth;
		float yMin = -0.15f / 16 * fullHeight;
		float yMax =  0.15f / 16 * fullHeight;
		builder.vertex(entry, xMin, yMax, 0).color(255, 255, 255, 255).texture(1, 1);
		builder.vertex(entry, xMax, yMax, 0).color(255, 255, 255, 255).texture(0, 1);
		builder.vertex(entry, xMax, yMin, 0).color(255, 255, 255, 255).texture(0, 0);
		builder.vertex(entry, xMin, yMin, 0).color(255, 255, 255, 255).texture(1, 0);
		BufferRenderer.drawWithGlobalProgram(builder.end());
		MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
		return true;
	}

	public static EmiRecipe getRecipeToDisplay(String recipeString, BlockPos pos) {
		Identifier rid = Identifier.tryParse(recipeString);
		EmiRecipe recipe;
		if ((rid == null || EmiApi.getRecipeManager().getRecipe(rid) == null) && recipeString.startsWith("xyzzy")) {
			String[] parts = recipeString.split(" ");
			List<EmiRecipe> recipes = EmiApi.getRecipeManager().getRecipes();
			if (parts.length == 2) {
				for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
					if (category.getId().toString().equals(parts[1])) {
						recipes = EmiApi.getRecipeManager().getRecipes(category);
						break;
					}
				}
			}
			int c = (int) (pos.hashCode() ^ (System.currentTimeMillis() / 769));
			if (recipes.isEmpty()) {
				return null;
			}
			recipe = recipes.get(new Random(c).nextInt(recipes.size()));
		} else {
			if (rid == null) {
				return null;
			}
			recipe = EmiApi.getRecipeManager().getRecipe(rid);
		}
		return recipe;
	}

	private static Framebuffer createFramebuffer(EmiRecipe recipe) {
		MinecraftClient client = MinecraftClient.getInstance();

		int width = recipe.getDisplayWidth() + 8;
		int height = recipe.getDisplayHeight() + 8;
		int scale = 4;

		String key = width * scale + "x" + height * scale;
		Framebuffer framebuffer = FB_CACHE.get(key);

		if (framebuffer == null) {
			framebuffer = new SimpleFramebuffer(width * scale, height * scale, true, MinecraftClient.IS_SYSTEM_MAC);
			framebuffer.setClearColor(0f, 0f, 0f, 0f);

			FB_CACHE.put(key, framebuffer);
		} else {
			framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
		}

		framebuffer.beginWrite(true);

		Matrix4fStack view = RenderSystem.getModelViewStack();
		view.pushMatrix();
		view.identity();
		view.translate(-1.0f, 1.0f, 0.0f);
		view.scale(2f / width, -2f / height, -1f / 1000f);
		view.translate(0.0f, 0.0f, 10.0f);
		RenderSystem.applyModelViewMatrix();

		float originalFogEnd = RenderSystem.getShaderFogEnd();
		RenderSystem.setShaderFogEnd(Float.MAX_VALUE);

		Matrix4f backupProj = RenderSystem.getProjectionMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f().identity(), VertexSorter.BY_Z);
		GlowcaseWidgetHolder holder = new GlowcaseWidgetHolder(recipe.getDisplayWidth(), recipe.getDisplayHeight());
		holder.widgets.add(new RecipeBackground(-4, -4, recipe.getDisplayWidth() + 8, recipe.getDisplayHeight() + 8));
		recipe.addWidgets(holder);
		// getEffectVertexConsumers doesn't cause random rendering issues like getEntityVertexConsumers
		DrawContext context = new DrawContext(client, SORRY.getEntityVertexConsumers());
		context.getMatrices().translate(4, 4, 0);
		for (Widget widget : holder.widgets) {
			widget.render(context, -9999, -9999, 0);
		}
		// Magic incantation/desperate prayer
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		DiffuseLighting.enableForLevel();

		RenderSystem.setProjectionMatrix(backupProj, VertexSorter.BY_DISTANCE);
		view.popMatrix();
		RenderSystem.applyModelViewMatrix();
		SORRY.getEntityVertexConsumers().draw();

		framebuffer.endWrite();
		RenderSystem.setShaderFogEnd(originalFogEnd);
		client.getFramebuffer().beginWrite(true);
		return framebuffer;
	}

	private static class GlowcaseWidgetHolder implements WidgetHolder {
		private final int width, height;
		public final List<Widget> widgets = Lists.newArrayList();

		public GlowcaseWidgetHolder(int width, int height) {
			this.width = width;
			this.height = height;
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}

		@Override
		public <T extends Widget> T add(T widget) {
			widgets.add(widget);
			return widget;
		}
	}
}
