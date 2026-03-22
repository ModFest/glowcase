package dev.hephaestus.glowcase.client.gui.screen.ingame;

import com.mojang.datafixers.util.Pair;
import dev.hephaestus.glowcase.Glowcase;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.client.GlowcaseClient;
import dev.hephaestus.glowcase.client.ScreenImageCache.ScreenTexture;
import dev.hephaestus.glowcase.packet.C2SEditTabletItem;
import dev.hephaestus.glowcase.util.TextUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class TabletEditScreen extends GlowcaseScreen {
	private static final Identifier TEXTURE = Glowcase.id("textures/gui/tablet.png");
	private static final int TXT_COLOR = 0x8af4b9;

	private static final int BG_WIDTH = 256;
	private static final int BG_HEIGHT = 160;

	// The cyan area where the pictures are being shown
	private static final int SCREEN_X1 = 5;
	private static final int SCREEN_Y1 = 20;
	private static final int SCREEN_X2 = 251;
	private static final int SCREEN_Y2 = 102;

	// maximum size a picture can take (ensures it does not go out of bounds while scaling)
	private static final int IMG_WIDTH = (int) ((SCREEN_X2 - SCREEN_X1) / 3f);
	private static final int IMG_HEIGHT = (int) ((SCREEN_Y2 - SCREEN_Y1) * .9f);

	// The data to be manipulated
	private int current;
	@Nullable
	private Pair<UUID, BlockPos> screen_pos;
	private final ArrayList<Pair<String, String>> slides;

	private boolean slide_dirty = false;

	// Currently shown pictures
	@Nullable
	private ScreenTexture previous_slide;
	@Nullable
	private ScreenTexture current_slide;
	@Nullable
	private ScreenTexture next_slide;

	// Widgets
	private StringWidget progressText;
	private EditBox urlEntryWidget;
	private EditBox altEntryWidget;
	private Button previousButton;
	private Button nextButton;

	public TabletEditScreen(ItemStack stack) {
		this.current = stack.getOrDefault(Glowcase.CURRENT_SLIDE_COMPONENT.get(), 0);

		if (stack.has(Glowcase.LINKED_SCREEN_COMPONENT.get()))
			this.screen_pos = stack.get(Glowcase.LINKED_SCREEN_COMPONENT.get());

		this.slides = new ArrayList<>();
		slides.addAll(stack.getOrDefault(Glowcase.SLIDESHOW_COMPONENT.get(), new ArrayList<>()));
	}

	@Override
	protected void init() {
		super.init();
		if (this.minecraft == null) return;

		this.progressText = new StringWidget(width / 2 - BG_WIDTH / 2 + 5, height / 2 - BG_HEIGHT / 2 + 5, (int) (BG_WIDTH * .2), this.minecraft.font.lineHeight,
			Component.empty(), this.minecraft.font)
//		FIXME these seem to no longer exist
//			.color(TXT_COLOR)
//			.alignLeft()
		;

		Component linkedText = (screen_pos == null) ? Component.translatable("gui.glowcase.tablet.not_linked")
			: Component.translatable("gui.glowcase.tablet.linked", screen_pos.getSecond().toShortString());

		StringWidget linkedTextWidget = new StringWidget(width / 2 - BG_WIDTH / 2 + 7 + (int) (BG_WIDTH * .2), height / 2 - BG_HEIGHT / 2 + 5, (int) (BG_WIDTH * .8) - 13, this.minecraft.font.lineHeight,
			linkedText, this.minecraft.font)

//		FIXME these seem to no longer exist
//			.setColor(TXT_COLOR)
//			.alignRight()
			;

		this.urlEntryWidget = new EditBox(this.minecraft.font, width / 2 - BG_WIDTH / 2 + 5, height / 2 + 30 - 1, BG_WIDTH - 10 - 55, 20, Component.empty());
		this.urlEntryWidget.setMaxLength(ScreenBlockEntity.URL_MAX_LENGTH);
		this.urlEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.url"));
		this.urlEntryWidget.setResponder((value) -> slide_dirty = true);

		this.altEntryWidget = new EditBox(this.minecraft.font, width / 2 - BG_WIDTH / 2 + 5, height / 2 + 55 - 1, BG_WIDTH - 10, 20, Component.empty());
		this.altEntryWidget.setMaxLength(ScreenBlockEntity.ALT_MAX_LENGTH);
		this.altEntryWidget.setHint(TextUtils.placeholder("gui.glowcase.alt"));
		this.altEntryWidget.setResponder((value) -> slide_dirty = true);

		Button updateButton = Button.builder(
			Component.translatable("gui.glowcase.refresh"),
			action -> {
				syncSlide();
				getSlides();
			}
		).bounds(width / 2 + BG_WIDTH / 2 - 55, height / 2 + 30 - 1, 50, 20).build();

		previousButton = Button.builder(
			Component.translatable("gui.glowcase.previous"),
			action -> {
				syncSlide();
				current--;
				getSlides();
			}
		).bounds(width / 2 - BG_WIDTH / 2 - 25 + SCREEN_X1, height / 2 - 25, 50, 20).build();

		nextButton = Button.builder(
			Component.translatable("gui.glowcase.next"),
			action -> {
				syncSlide();
				current++;
				getSlides();
			}
		).bounds(width / 2 + BG_WIDTH / 2 - 35 + SCREEN_X1, height / 2 - 25, 50, 20).build();

		getSlides();

		this.addRenderableWidget(this.progressText);
		this.addRenderableWidget(linkedTextWidget);
		this.addRenderableWidget(this.urlEntryWidget);
		this.addRenderableWidget(this.altEntryWidget);
		this.addRenderableWidget(updateButton);
		this.addRenderableWidget(previousButton);
		this.addRenderableWidget(nextButton);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
			width / 2 - BG_WIDTH / 2, height / 2 - BG_HEIGHT / 2,
			0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_WIDTH
		);

		// Render Slideshow

		graphics.enableScissor(
			width / 2 - BG_WIDTH / 2 + SCREEN_X1,
			height / 2 - BG_HEIGHT / 2 + SCREEN_Y1,
			width / 2 - BG_WIDTH / 2 + SCREEN_X2,
			height / 2 - BG_HEIGHT / 2 + SCREEN_Y2
		);
		//RenderSystem.enableBlend();

		// Previous and Next Slide
		renderPicture(graphics, previous_slide, width / 2 - BG_WIDTH / 2, height / 2 - 20, .8f);
		renderPicture(graphics, next_slide, width / 2 + BG_WIDTH / 2, height / 2 - 20, .8f);

		// Fade-out Gradient
		// We can't really use the build-in gradient because it only goes vertical

		// Left
		graphics.blit(
			RenderPipelines.GUI_TEXTURED, TEXTURE,
			width / 2 - BG_WIDTH / 2 + SCREEN_X1,
			height / 2 - BG_HEIGHT / 2 + SCREEN_Y1,
			0, 160,
			IMG_WIDTH - 1, (SCREEN_Y2 - SCREEN_Y1) + 1,
			BG_WIDTH, BG_WIDTH
		);

		// Right
		graphics.blit(
			RenderPipelines.GUI_TEXTURED, TEXTURE,
			width / 2 - BG_WIDTH / 2 + SCREEN_X2 - IMG_WIDTH + 1,
			height / 2 - BG_HEIGHT / 2 + SCREEN_Y1,
			BG_WIDTH - IMG_WIDTH + 1, 160,
			IMG_WIDTH - 1, (SCREEN_Y2 - SCREEN_Y1) + 1,
			BG_WIDTH, BG_WIDTH
		);

		// Current slide
		renderPicture(graphics, current_slide, width / 2, height / 2 - 20, 1f);

		//RenderSystem.disableBlend();
		graphics.disableScissor();
	}

	/**
	 * <p>Renders a given screen texture to the given coordinates (centered).</p>
	 *
	 * <p>The screen texture will be rescaled to fit within the IMG_WIDTH and IMG_HEIGHT constants.</p>
	 */
	public void renderPicture(GuiGraphicsExtractor graphics, @Nullable ScreenTexture slide, int x, int y, float scale) {
		if (slide == null || slide.getTexture().getSecond() == null)
			return;

		int cur_width = slide.getWidth();
		int cur_height = slide.getHeight();

		float width_scale = (float) IMG_WIDTH / cur_width;
		float height_scale = (float) IMG_HEIGHT / cur_height;
		float final_scale = Math.min(width_scale, height_scale) * scale;

		// Scale width and height
		int scaled_width = (int) (cur_width * final_scale);
		int scaled_height = (int) (cur_height * final_scale);

		graphics.blit(
			RenderPipelines.GUI_TEXTURED, slide.getTexture().getSecond(),
			x - scaled_width / 2, y - scaled_height / 2, 0, 0,
			scaled_width, scaled_height, scaled_width, scaled_height
		);
	}

	public void syncSlide() {
		if (slide_dirty) {
			slides.set(current, new Pair<>(this.urlEntryWidget.getValue(), this.altEntryWidget.getValue()));
			C2SEditTabletItem.of(current, this.urlEntryWidget.getValue(), this.altEntryWidget.getValue()).send();
		}
	}

	/**
	 * <p>Making sure no unexpected crashes appear.</p>
	 * <p>Also toggles previous/next buttons</p>
	 */
	public void keepBoundaries() {
		if (current < 0)
			current = 0;
		else if (current > slides.size())
			current = slides.size();

		if (current == slides.size())
			slides.add(new Pair<>("", ""));

		previousButton.active = current > 0;
		nextButton.active = current < slides.size();
	}

	/**
	 * <p>Gathers the current, next and previous image that is being shown in the gui.</p>
	 *
	 * <p>Also updates the shown variables and text fields and ensures the boundaries are met.</p>
	 */
	public void getSlides() {
		keepBoundaries();

		// Current slide
		Pair<String, String> slide = slides.get(current);
		current_slide = GlowcaseClient.screenImageCache.getImage(slide.getFirst(), null);


		// Previous slide
		if (current - 1 >= 0) {
			Pair<String, String> slide1 = slides.get(current - 1);
			previous_slide = GlowcaseClient.screenImageCache.getImage(slide1.getFirst(), null);
		} else
			previous_slide = null;

		// Next slide
		if (current + 1 < slides.size()) {
			Pair<String, String> slide1 = slides.get(current + 1);
			next_slide = GlowcaseClient.screenImageCache.getImage(slide1.getFirst(), null);
		} else
			next_slide = null;

		// Update shown values
		this.urlEntryWidget.setValue(slide.getFirst());
		this.altEntryWidget.setValue(slide.getSecond());
		this.progressText.setMessage(Component.translatable("gui.glowcase.progress", "" + (current + 1), slides.size()));
		slide_dirty = false;
	}

	@Override
	public void onClose() {
		syncSlide();
		super.onClose();
	}
}
