package dev.hephaestus.glowcase.client.gui.screen.ingame;

import dev.hephaestus.glowcase.block.entity.DisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.client.gui.widget.ingame.number.Vec3FieldsWidget;
import dev.hephaestus.glowcase.client.util.ModelPartUtil;
import dev.hephaestus.glowcase.client.util.Tooltips;
import dev.hephaestus.glowcase.mixinsupport.PartPosedRenderState;
import dev.hephaestus.glowcase.packet.C2SEditEntityDisplayBlock;
import dev.hephaestus.glowcase.util.PartPoseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityDisplayEditScreen extends DisplayBlockEditScreen {

	private final EntityDisplayBlockEntity entityDisplay;
	private final Set<String> expandedParts = new HashSet<>();
	private boolean expanded = false;
	private final int STARTING_X = 10;
	private float displayYaw = 0F;
	protected Button rotateLeft;
	protected Button rotateRight;

	public EntityDisplayEditScreen(DisplayBlockEntity displayBlock) {
		super(displayBlock);
		this.entityDisplay = (EntityDisplayBlockEntity) displayBlock;
	}

	@Override
	public void init() {
		super.init();

		int bottom = this.rowOffset;
		Entity displayEntity = this.entityDisplay.getDisplayEntity();
		EntityDisplayBlockEntity entityDisplayBlockEntity = ((EntityDisplayBlockEntity)(this.blockEntity));
		if(displayEntity != null) {
			if(displayEntity instanceof Mob) {
				this.addRenderableWidget(Checkbox.builder(Component.translatable("gui.glowcase.baby"), this.minecraft.font)
					.selected(entityDisplayBlockEntity.isBaby())
					.onValueChange((_, checked) -> {
							entityDisplayBlockEntity.setBaby(checked);
							editDisplayBlock();
							this.expanded = false;
							this.rebuildWidgets();
						}
					)
					.pos(STARTING_X, this.rowOffset += ROW)
					.build());
			}

			this.addRenderableWidget(Button.builder(
				Component.literal(this.expanded ? "▼ " : "▶ ").append(Component.translatable("gui.glowcase.model_parts")),
				_ -> {
					this.expanded = !this.expanded;
					this.rebuildWidgets();
				}
			).bounds(STARTING_X, this.rowOffset += ROW, 200, 20).build());

			ModelPart root = ModelPartUtil.getModelRoot(displayEntity);
			if (this.expanded && root != null) {
				bottom = this.addPartRows("root", root, this.rowOffset += ROW, 0);
			}

			this.rotateLeft = Button.builder(Component.literal("←"), _ -> {
				displayYaw -= 0.174533F;
			}).size( 20, 20).build();
			this.addRenderableWidget(this.rotateLeft);

			this.rotateRight = Button.builder(Component.literal("→"), _ -> {
				displayYaw += 0.174533F;
			}).size( 20, 20).build();
			this.addRenderableWidget(this.rotateRight);
		}

		this.setMaxScroll(0, bottom + 10 - this.height, false);
	}

	private int addPartRows(String path, ModelPart parent, int y, int depth) {
		int indent = Math.min(depth, 6) * 10;
		for (Map.Entry<String, ModelPart> entry : parent.children.entrySet()) {
			String name = entry.getKey();
			String partPath = path + "." + name;
			boolean partExpanded = this.expandedParts.contains(partPath);
			String displayName = name.replace("_", " ");
			displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);

			this.addRenderableWidget(Button.builder(Component.literal((partExpanded ? "▼ " : "▶ ") + displayName), _ -> {
				if (!this.expandedParts.remove(partPath)) this.expandedParts.add(partPath);
				this.rebuildWidgets();
			}).bounds(STARTING_X + 10 + indent, y, 190 - indent, 20).build());

			y += 22;
			if (!partExpanded) continue;

			for (int i = 0; i <= 1; i++) {
				boolean isRotation = i == 0;
				Component label = Component.translatable(isRotation ? "gui.glowcase.rotation" : "gui.glowcase.offset");
				this.addRenderableWidget(new StringWidget(STARTING_X + 20, y + 5, this.font.width(label), 9, label, this.font));

				PartPoseInfo partPoseInfo = this.entityDisplay.getPartPostInfo(partPath);
				this.addRenderableWidget(Vec3FieldsWidget.builder(this.font, new Vec3(isRotation ? partPoseInfo.rotation() : partPoseInfo.offset()))
					.setPos(STARTING_X + 70, y).
					setWidth(180).setHeight(18)
					.setRotation(isRotation)
					.setTooltips(isRotation ? Tooltips.ROTATIONS : Tooltips.OFFSET_AXES)
					.setEditBoxCharacterLimit(5)
					.setOnValueChange(vec3 -> {
					PartPoseInfo current = this.entityDisplay.getPartPostInfo(partPath);
						this.entityDisplay.setPartPoseInfo(
							partPath,
							isRotation ? current.withRotation(vec3.toVector3f()) : current.withOffset(vec3.toVector3f())
						);
					editDisplayBlock();
				}).build());
				y += 22;
			}
			y = this.addPartRows(partPath, entry.getValue(), y, depth + 1);
		}
		return y;

	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics,mouseX,mouseY,a);
		if(entityDisplay.getDisplayEntity() !=  null && entityDisplay.getDisplayEntity() instanceof Mob displayMob) {
			int centerX = graphics.guiWidth() * 3 / 4;
			int centerY = graphics.guiHeight() / 3;
			extractEntity(graphics, centerX - 100, centerY - 140, centerX + 100, centerY + 100, 40, 0, displayMob, this.entityDisplay.getPartPoses());
		}
	}

	public void extractEntity(final GuiGraphicsExtractor graphics, final int x0, final int y0, final int x1, final int y1, final int size, final float offsetY, final LivingEntity entity, final Map<String,PartPoseInfo> partPoseInfoMap) {
		Quaternionf rotation = (new Quaternionf()).rotateZ((float)Math.PI).rotateY(this.displayYaw);
		EntityRenderState renderState = extractRenderState(entity, partPoseInfoMap);
		if (renderState instanceof LivingEntityRenderState livingRenderState) {
			livingRenderState.bodyRot = 180.0F;
			livingRenderState.yRot = 0.0F;
			livingRenderState.xRot = 0.0F;

			livingRenderState.boundingBoxWidth /= livingRenderState.scale;
			livingRenderState.boundingBoxHeight /= livingRenderState.scale;
			livingRenderState.scale = 1.0F;
		}

		float pixelHeight = renderState.boundingBoxHeight * size;
		int rotateX = (graphics.guiWidth() * 3 / 4) - 25;
		int rotateY = ((graphics.guiHeight() / 3) + 5) + (int) (pixelHeight / 2);
		setRotateButtonPositions(rotateX, rotateY);

		Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + offsetY, 0.0F);
		graphics.entity(renderState, (float)size, translation, rotation, null, x0, y0, x1, y1);
	}

	private EntityRenderState extractRenderState(final LivingEntity entity, final Map<String, PartPoseInfo> partPoseInfoMap) {
		EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = entityRenderDispatcher.getRenderer(entity);
		EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
		((PartPosedRenderState) renderState).glowcase$setPartPoses(partPoseInfoMap.isEmpty() ? null : partPoseInfoMap);
		renderState.shadowPieces.clear();
		renderState.outlineColor = 0;
		return renderState;
	}

	private void setRotateButtonPositions(int x, int y){
		if(this.rotateLeft != null && this.rotateRight != null) {
			this.rotateLeft.setPosition(x, y);
			this.rotateRight.setPosition(x, y);
		}

	}

	@Override
	public CustomPacketPayload getUpdatePayload() {
		return C2SEditEntityDisplayBlock.of(blockEntity);
	}

}
