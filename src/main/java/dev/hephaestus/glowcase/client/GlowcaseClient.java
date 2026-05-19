package dev.hephaestus.glowcase.client;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.render.block.entity.ConfigLinkBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.EntityDisplayBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.HyperlinkBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.ItemAcceptorBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.ItemDisplayBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.ItemProviderBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.OutlineBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.ParticleDisplayBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.PopupBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.RecipeBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.ScreenBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.SoundPlayerBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.SpriteBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.block.entity.TextBlockEntityRenderer;
import dev.hephaestus.glowcase.client.render.item.ItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.NoteItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.TabletItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.tint.GlowcaseTintSource;
import dev.hephaestus.glowcase.client.util.NoteTextColorResource;
import dev.hephaestus.glowcase.item.ScrollableItem;
import dev.hephaestus.glowcase.mixin.AbstractContainerScreenInvoker;
import dev.hephaestus.glowcase.packet.C2SSlotScrolled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GlowcaseClient implements ClientModInitializer {
	public static final Boolean RRV_LOADED = FabricLoader.getInstance().isModLoaded("rrv");
	public static final Identifier PROVIDER_CROSSHAIR_TEXTURE = Glowcase.id("hud/provider_crosshair");
	public static final ScreenImageCache screenImageCache = new ScreenImageCache();

	private double accScroll = 0;

	@Override
	public void onInitializeClient() {
		Glowcase.proxy = new GlowcaseClientProxy();

		BlockEntityRenderers.register(Glowcase.TEXT_BLOCK_ENTITY.get(), TextBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.HYPERLINK_BLOCK_ENTITY.get(), HyperlinkBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.CONFIG_LINK_BLOCK_ENTITY.get(), ConfigLinkBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.ITEM_DISPLAY_BLOCK_ENTITY.get(), ItemDisplayBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.POPUP_BLOCK_ENTITY.get(), PopupBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.SCREEN_BLOCK_ENTITY.get(), ScreenBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.SPRITE_BLOCK_ENTITY.get(), SpriteBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.RECIPE_BLOCK_ENTITY.get(), RecipeBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.OUTLINE_BLOCK_ENTITY.get(), OutlineBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.PARTICLE_DISPLAY_BLOCK_ENTITY.get(), ParticleDisplayBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.SOUND_BLOCK_ENTITY.get(), SoundPlayerBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.ITEM_ACCEPTOR_BLOCK_ENTITY.get(), ItemAcceptorBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.ITEM_PROVIDER_BLOCK_ENTITY.get(), ItemProviderBlockEntityRenderer::new);
		BlockEntityRenderers.register(Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), EntityDisplayBlockEntityRenderer::new);

		ItemHandRenderer.register(Glowcase.TABLET_ITEM.get().asItem(), new TabletItemHandRenderer());
		ItemHandRenderer.register(Glowcase.NOTE_ITEM.get().asItem(), new NoteItemHandRenderer());

		ItemTintSources.ID_MAPPER.put(Glowcase.id("auto"), GlowcaseTintSource.CODEC);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Glowcase.id("note_txt_color"), new NoteTextColorResource());

		/*ModelPredicateProviderRegistryAccessor.callRegister(Identifier.of("glowcase:awakened"), (stack, world, entity, seed) -> {
			if (!EMI_LOADED) {
				return 0;
			}
			List<ItemStack> testStacks = Lists.newArrayList();
			if (entity != null) {
				testStacks.add(entity.getMainHandStack());
				testStacks.add(entity.getOffHandStack());
			}
			MinecraftClient client = MinecraftClient.getInstance();
			ClientPlayerEntity player = client.player;
			if (player != null) {
				ScreenHandler handler = player.currentScreenHandler;
				if (handler != null) {
					testStacks.add(handler.getCursorStack());
				}
			}
			for (ItemStack s : testStacks) {
				if (s == stack) {
					return 1;
				}
			}
			return 0;
		});*/

		ScreenEvents.BEFORE_INIT.register(((client, sc, scaledWidth, scaledHeight) -> {
			if (sc instanceof AbstractContainerScreen<?> hs) {
				ScreenMouseEvents.allowMouseScroll(hs).register((screen, x, y, h, v) -> allowMouseScroll((AbstractContainerScreen<?>) screen, x, y, v));
			}
		}));

		/*if (EMI_LOADED) {
			ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
				EmiWorldRenderUtils.disposeCache();
				EmiUtils.RECIPE_LIST.dispose();
			});
		}*/

		GlowcaseClientNetworking.init();
	}

	/**
	 * @author zacharybarbanell
	 */
	private boolean allowMouseScroll(AbstractContainerScreen<?> screen, double x, double y, double scroll) {
		Slot slot = ((AbstractContainerScreenInvoker) screen).invokeGetSlotAt(x, y);
		if (slot == null) return true;
		ItemStack stack = slot.getItem();
		if (!(stack.getItem() instanceof ScrollableItem si)) return true;
		if (accScroll * scroll < 0) {
			accScroll = 0;
		}
		accScroll += scroll;
		int amount = (int) accScroll;
		if (amount == 0) return true;
		accScroll -= amount;
		si.scroll(stack, Minecraft.getInstance().player, amount);
		ClientPlayNetworking.send(new C2SSlotScrolled(screen.getMenu().containerId, screen.getMenu().getStateId(), slot.index, amount));
		return false;
	}
}
