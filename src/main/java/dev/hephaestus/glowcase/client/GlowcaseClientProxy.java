package dev.hephaestus.glowcase.client;

import dev.hephaestus.glowcase.GlowcaseCommonProxy;
import dev.hephaestus.glowcase.block.entity.ConfigLinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.EntityDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.HyperlinkBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemAcceptorBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.ItemProviderBlockEntity;
import dev.hephaestus.glowcase.block.entity.OutlineBlockEntity;
import dev.hephaestus.glowcase.block.entity.ParticleDisplayBlockEntity;
import dev.hephaestus.glowcase.block.entity.PopupBlockEntity;
import dev.hephaestus.glowcase.block.entity.RecipeBlockEntity;
import dev.hephaestus.glowcase.block.entity.ScreenBlockEntity;
import dev.hephaestus.glowcase.block.entity.SoundPlayerBlockEntity;
import dev.hephaestus.glowcase.block.entity.SpriteBlockEntity;
import dev.hephaestus.glowcase.block.entity.TextBlockEntity;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ConfigLinkBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.EntityDisplayEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.HyperlinkBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ItemAcceptorBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ItemDisplayEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ItemProviderBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.NoteEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.OutlineBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ParticleDisplayEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.PopupBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.PopupBlockViewScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.RecipeBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.ScreenBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.SoundPlayerBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.SpriteBlockEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.TabletEditScreen;
import dev.hephaestus.glowcase.client.gui.screen.ingame.TextBlockEditScreen;
import dev.hephaestus.glowcase.client.util.ConfigLinkClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class GlowcaseClientProxy extends GlowcaseCommonProxy {

	@Override
	public void openConfigLinkBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ConfigLinkBlockEntity be) {
			Minecraft.getInstance().setScreen(new ConfigLinkBlockEditScreen(be));
		}
	}

	@Override
	public void openConfigScreen(String link) {
		Minecraft client = Minecraft.getInstance();
		client.setScreen(ConfigLinkClientUtil.getConfigScreen(client, link));
	}

	@Override
	public void openHyperlinkBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof HyperlinkBlockEntity be) {
			Minecraft.getInstance().setScreen(new HyperlinkBlockEditScreen(be));
		}
	}

	@Override
	public void openUrlWithConfirmation(String url) {
		ConfirmLinkScreen.confirmLinkNow(Minecraft.getInstance().screen, url);
	}

	@Override
	public void openItemDisplayBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ItemDisplayBlockEntity be) {
			Minecraft.getInstance().setScreen(new ItemDisplayEditScreen(be));
		}
	}

	@Override
	public void openItemProviderBlockEditScreen(BlockPos pos){
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ItemProviderBlockEntity be) {
			Minecraft.getInstance().setScreen(new ItemProviderBlockEditScreen(be));
		}
	}

	@Override
	public void openTextBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof TextBlockEntity be) {
			Minecraft.getInstance().setScreen(new TextBlockEditScreen(be));
		}
	}

	@Override
	public void openPopupBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof PopupBlockEntity be) {
			Minecraft.getInstance().setScreen(new PopupBlockEditScreen(be));
		}
	}

	@Override
	public void openPopupBlockViewScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof PopupBlockEntity be) {
			Minecraft.getInstance().setScreen(new PopupBlockViewScreen(be));
		}
	}

	@Override
	public void openScreenBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ScreenBlockEntity be) {
			Minecraft.getInstance().setScreen(new ScreenBlockEditScreen(be));
		}
	}

	@Override
	public void openRecipeBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof RecipeBlockEntity be) {
			Minecraft.getInstance().setScreen(new RecipeBlockEditScreen(be));
		}
	}

	@Override
	public void openSpriteBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof SpriteBlockEntity be) {
			Minecraft.getInstance().setScreen(new SpriteBlockEditScreen(be));
		}
	}

	@Override
	public void openOutlineBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof OutlineBlockEntity be) {
			Minecraft.getInstance().setScreen(new OutlineBlockEditScreen(be));
		}
	}

	@Override
	public void openParticleDisplayBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ParticleDisplayBlockEntity be) {
			Minecraft.getInstance().setScreen(new ParticleDisplayEditScreen(be));
		}
	}

	@Override
	public void openSoundBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof SoundPlayerBlockEntity be) {
			Minecraft.getInstance().setScreen(new SoundPlayerBlockEditScreen(be));
		}
	}

	@Override
	public void openItemAcceptorBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof ItemAcceptorBlockEntity be) {
			Minecraft.getInstance().setScreen(new ItemAcceptorBlockEditScreen(be));
		}
	}

	@Override
	public void openTabletEditScreen(ItemStack stack) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			Minecraft.getInstance().setScreen(new TabletEditScreen(stack));
		}
	}

	@Override
	public void openNoteEditScreen(ItemStack stack) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			Minecraft.getInstance().setScreen(new NoteEditScreen(stack));
		}
	}

	@Override
	public void openEntityDisplayBlockEditScreen(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		if (client.level != null && client.level.getBlockEntity(pos) instanceof EntityDisplayBlockEntity be) {
			Minecraft.getInstance().setScreen(new EntityDisplayEditScreen(be));
		}
	}
}
