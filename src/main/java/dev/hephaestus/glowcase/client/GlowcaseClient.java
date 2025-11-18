package dev.hephaestus.glowcase.client;

import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.client.render.block.entity.*;
import dev.hephaestus.glowcase.client.render.item.ItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.NoteItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.TabletItemHandRenderer;
import dev.hephaestus.glowcase.client.render.item.tint.GlowcaseTintSource;
import dev.hephaestus.glowcase.client.util.NoteTextColorResource;
import dev.hephaestus.glowcase.item.ScrollableItem;
import dev.hephaestus.glowcase.mixin.HandledScreenInvoker;
import dev.hephaestus.glowcase.packet.C2SSlotScrolled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class GlowcaseClient implements ClientModInitializer {
	public static final Boolean EMI_LOADED = FabricLoader.getInstance().isModLoaded("emi");
	public static final ScreenImageCache screenImageCache = new ScreenImageCache();
	public static final Identifier PROVIDER_CROSSHAIR_TEXTURE = Glowcase.id("hud/provider_crosshair");

	private double accScroll = 0;

	@Override
	public void onInitializeClient() {
		Glowcase.proxy = new GlowcaseClientProxy();

		BlockEntityRendererFactories.register(Glowcase.TEXT_BLOCK_ENTITY.get(), TextBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.HYPERLINK_BLOCK_ENTITY.get(), HyperlinkBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.CONFIG_LINK_BLOCK_ENTITY.get(), ConfigLinkBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.ITEM_DISPLAY_BLOCK_ENTITY.get(), ItemDisplayBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.POPUP_BLOCK_ENTITY.get(), PopupBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.SCREEN_BLOCK_ENTITY.get(), ScreenBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.SPRITE_BLOCK_ENTITY.get(), SpriteBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.RECIPE_BLOCK_ENTITY.get(), RecipeBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.OUTLINE_BLOCK_ENTITY.get(), OutlineBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.PARTICLE_DISPLAY_BLOCK_ENTITY.get(), ParticleDisplayBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.SOUND_BLOCK_ENTITY.get(), SoundPlayerBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.ITEM_ACCEPTOR_BLOCK_ENTITY.get(), ItemAcceptorBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.ITEM_PROVIDER_BLOCK_ENTITY.get(), ItemProviderBlockEntityRenderer::new);
		BlockEntityRendererFactories.register(Glowcase.ENTITY_DISPLAY_BLOCK_ENTITY.get(), EntityDisplayBlockEntityRenderer::new);

		ItemHandRenderer.register(Glowcase.TABLET_ITEM.get().asItem(), new TabletItemHandRenderer());
		ItemHandRenderer.register(Glowcase.NOTE_ITEM.get().asItem(), new NoteItemHandRenderer());

		TintSourceTypes.ID_MAPPER.put(Glowcase.id("auto"), GlowcaseTintSource.CODEC);

		WorldRenderEvents.AFTER_ENTITIES.register(BakedBlockEntityRenderer.Manager::render);
		InvalidateRenderStateCallback.EVENT.register(BakedBlockEntityRenderer.Manager::reset);

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new NoteTextColorResource());

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
			if (sc instanceof HandledScreen<?> hs) {
				ScreenMouseEvents.allowMouseScroll(hs).register((screen, x, y, h, v) -> allowMouseScroll((HandledScreen<?>) screen, x, y, v));
			}
		}));

		/*if (EMI_LOADED) {
			ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
				EmiWorldRenderUtils.disposeCache();
				EmiUtils.RECIPE_LIST.dispose();
			});
		}*/
	}

	/**
	 * @author zacharybarbanell
	 */
	private boolean allowMouseScroll(HandledScreen<?> screen, double x, double y, double scroll) {
		Slot slot = ((HandledScreenInvoker) screen).invokeGetSlotAt(x, y);
		if (slot == null) return true;
		ItemStack stack = slot.getStack();
		if (!(stack.getItem() instanceof ScrollableItem si)) return true;
		if (accScroll * scroll < 0) {
			accScroll = 0;
		}
		accScroll += scroll;
		int amount = (int) accScroll;
		if (amount == 0) return true;
		accScroll -= amount;
		si.scroll(stack, MinecraftClient.getInstance().player, amount);
		ClientPlayNetworking.send(new C2SSlotScrolled(screen.getScreenHandler().syncId, screen.getScreenHandler().getRevision(), slot.id, amount));
		return false;
	}
}
