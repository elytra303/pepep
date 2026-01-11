package rich.modules.impl.combat.macetarget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;
import rich.modules.impl.combat.macetarget.armor.ArmorSwapHandler;
import rich.modules.impl.combat.macetarget.armor.FireworkHandler;
import rich.util.inventory.InventoryUtils;
import rich.util.timer.StopWatch;

@Getter
@RequiredArgsConstructor
public class StageHandler {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private final ArmorSwapHandler armorSwapHandler;
    private final FireworkHandler fireworkHandler;
    private final StopWatch fireworkTimer;
    private final float heightValue;
    
    @Setter
    private Stage stage = Stage.PREPARE;
    
    @Setter
    private boolean pendingAttack = false;
    
    public void handlePrepare(LivingEntity target) {
        if (!InventoryUtils.hasElytra()) {
            int slot = InventoryUtils.findElytraSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilentMode());
            }
            return;
        }
        stage = Stage.FLYING_UP;
        fireworkTimer.reset();
    }
    
    public void handleFlyingUp(LivingEntity target, boolean isSilent) {
        if (!InventoryUtils.hasElytra()) {
            stage = Stage.PREPARE;
            return;
        }
        
        if (mc.player.isGliding() && fireworkTimer.finished(300)) {
            fireworkHandler.useFirework(isSilent);
            fireworkTimer.reset();
        }
        
        if (mc.player.getY() - target.getY() >= heightValue) {
            stage = Stage.TARGETTING;
        }
    }
    
    public void handleTargetting(LivingEntity target, boolean isSilent) {
        float swapDistance = 12.0f;
        
        if (InventoryUtils.hasElytra() && mc.player.distanceTo(target) < swapDistance 
                && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilent);
            }
        }
        
        if (mc.player.distanceTo(target) < 16.0f) {
            stage = Stage.ATTACKING;
        }
    }
    
    public void handleAttacking(LivingEntity target, boolean isSilent) {
        if (InventoryUtils.hasElytra() && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilent);
            }
            return;
        }
        
        if (!InventoryUtils.hasElytra() && !armorSwapHandler.isActive() 
                && mc.player.distanceTo(target) < 5) {
            pendingAttack = true;
            stage = Stage.FLYING_UP;
            fireworkTimer.reset();
        }
    }
    
    public void performAttack(LivingEntity target) {
        if (mc.player == null || target == null) return;
        
        int maceSlot = InventoryUtils.findHotbarItem(Items.MACE);
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
        }
        
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }
        
        stage = Stage.FLYING_UP;
        fireworkTimer.reset();
    }
    
    private boolean isSilentMode() {
        return true;
    }
    
    public void reset() {
        stage = Stage.PREPARE;
        pendingAttack = false;
    }
}