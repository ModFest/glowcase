package dev.hephaestus.glowcase.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hephaestus.glowcase.Glowcase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}

	@ModifyReturnValue(
		method = "isSecondaryUseActive",
		at = @At(value = "RETURN")
	)
	private boolean directLockInteraction(boolean original) {
		return getItemBySlot(EquipmentSlot.MAINHAND).getItem().equals(Glowcase.LOCK_ITEM.get()) || original;
	}
}
