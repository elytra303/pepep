package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.InputEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.impl.combat.macetarget.*;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;
import rich.modules.impl.combat.macetarget.armor.ArmorSwapHandler;
import rich.modules.impl.combat.macetarget.armor.FireworkHandler;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.SwapSettings;
import rich.util.math.TaskPriority;
import rich.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceTarget extends ModuleStructure {

    public static MaceTarget getInstance() {
        return Instance.get(MaceTarget.class);
    }

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свапа")
            .value("Silent", "Legit")
            .selected("Silent");

    final SliderSettings height = new SliderSettings("Высота", "Высота полёта над целью")
            .range(20.0f, 60.0f)
            .setValue(30.0f);

    final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Типы целей")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    final ArmorSwapHandler armorSwapHandler;
    final FireworkHandler fireworkHandler;
    final TargetFinder targetFinder = new TargetFinder();
    final StopWatch fireworkTimer = new StopWatch();

    LivingEntity target;
    Stage stage = Stage.PREPARE;
    boolean pendingAttack = false;

    public MaceTarget() {
        super("MaceTarget", "Mace Target", ModuleCategory.COMBAT);
        setup(modeSetting, height, targetType);

        armorSwapHandler = new ArmorSwapHandler(this::buildSettings);
        fireworkHandler = new FireworkHandler(this::buildSettings);
    }

    private boolean isSilentMode() {
        return modeSetting.getSelected().equals("Silent");
    }

    private SwapSettings buildSettings() {
        return isSilentMode() ? SwapSettings.instant() : SwapSettings.legit();
    }

    @Override
    public void activate() {
        stage = Stage.PREPARE;
        target = null;
        pendingAttack = false;
        armorSwapHandler.reset();
        fireworkHandler.reset();
        fireworkTimer.reset();
    }

    @Override
    public void deactivate() {
        armorSwapHandler.forceRestore();
        fireworkHandler.forceRestore();
        target = null;
        targetFinder.releaseTarget();
        armorSwapHandler.reset();
        fireworkHandler.reset();
        AngleConnection.INSTANCE.startReturning();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.PRE) {
            if (target == null || !target.isAlive()) {
                findTarget();
            }

            if (target == null) return;

            switch (stage) {
                case FLYING_UP -> {
                    if (InventoryUtils.hasElytra() && mc.player.isGliding()) {
                        Angle targetAngle = MathAngle.fromVec3d(
                                target.getEntityPos().add(0, height.getValue(), 0).subtract(mc.player.getEyePos())
                        );
                        rotateTo(targetAngle);
                    }
                }
                case TARGETTING, ATTACKING -> {
                    Angle targetAngle = MathAngle.fromVec3d(target.getEntityPos().subtract(mc.player.getEyePos()));
                    rotateTo(targetAngle);
                }
            }
        }

        if (event.getType() == EventType.POST) {
            if (pendingAttack) {
                performAttack();
                pendingAttack = false;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            resetAllStates();
            return;
        }

        if (!isSilentMode()) {
            armorSwapHandler.processLoop();
            fireworkHandler.processLoop();
        }

        if (armorSwapHandler.isActive() || fireworkHandler.isActive()) {
            return;
        }

        if (target == null || !target.isAlive()) {
            return;
        }

        boolean hasElytra = InventoryUtils.hasElytra();

        switch (stage) {
            case PREPARE -> handlePrepare(hasElytra);
            case FLYING_UP -> handleFlyingUp(hasElytra);
            case TARGETTING -> handleTargetting();
            case ATTACKING -> handleAttacking(hasElytra);
        }
    }

    private void handlePrepare(boolean hasElytra) {
        if (!hasElytra) {
            int slot = InventoryUtils.findElytraSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilentMode());
            }
            return;
        }
        stage = Stage.FLYING_UP;
        fireworkTimer.reset();
    }

    private void handleFlyingUp(boolean hasElytra) {
        if (!hasElytra) {
            stage = Stage.PREPARE;
            return;
        }

        if (mc.player.isGliding() && fireworkTimer.finished(300)) {
            fireworkHandler.useFirework(isSilentMode());
            fireworkTimer.reset();
        }

        if (mc.player.getY() - target.getY() >= height.getValue()) {
            stage = Stage.TARGETTING;
        }
    }

    private void handleTargetting() {
        float swapDistance = 12.0f;

        if (InventoryUtils.hasElytra() && mc.player.distanceTo(target) < swapDistance
                && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilentMode());
            }
        }

        if (mc.player.distanceTo(target) < 16.0f) {
            stage = Stage.ATTACKING;
        }
    }

    private void handleAttacking(boolean hasElytra) {
        if (hasElytra && !armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                armorSwapHandler.startSwap(slot, isSilentMode());
            }
            return;
        }

        if (!hasElytra && !armorSwapHandler.isActive() && mc.player.distanceTo(target) < 5) {
            pendingAttack = true;
            stage = Stage.FLYING_UP;
            fireworkTimer.reset();
        }
    }

    private void performAttack() {
        if (mc.player == null || target == null) return;

        int maceSlot = InventoryUtils.findHotbarItem(net.minecraft.item.Items.MACE);
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(maceSlot)
            );
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        if (maceSlot != -1 && maceSlot != prevSlot) {
            mc.getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(prevSlot)
            );
        }

        stage = Stage.FLYING_UP;
        fireworkTimer.reset();
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null) return;

        if (armorSwapHandler.getMovement().isBlocked() || fireworkHandler.getMovement().isBlocked()) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
        }

        if (target != null && InventoryUtils.hasElytra() && stage == Stage.FLYING_UP) {
            if (mc.player.isOnGround()) {
                event.setJumping(true);
            } else if (!mc.player.isGliding() && !mc.player.getAbilities().flying) {
                event.setJumping(mc.player.age % 2 == 0);
            }
        }
    }

    private void findTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        targetFinder.searchTargets(mc.world.getEntities(), 128.0f, 360, true);
        targetFinder.validateTarget(filter::isValid);
        target = targetFinder.getCurrentTarget();
    }

    private void rotateTo(Angle angle) {
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(angle, angle.toVector());
        AngleConnection.INSTANCE.rotateTo(rotation, target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }

    private void resetAllStates() {
        armorSwapHandler.reset();
        fireworkHandler.reset();
        target = null;
        stage = Stage.PREPARE;
        pendingAttack = false;
    }
}