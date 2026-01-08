package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.SwapExecutor;
import rich.util.inventory.SwapSettings;
import rich.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTotem extends ModuleStructure {

    final SelectSetting swapMode = new SelectSetting("Режим свапа", "Способ свапа тотема")
            .value("Instant", "Legit")
            .selected("Legit");

    final SliderSettings healthThreshold = new SliderSettings("Порог здоровья", "Минимальное здоровье для взятия тотема")
            .range(1, 20).setValue(6);

    final SliderSettings anticipation = new SliderSettings("Упреждение", "Дополнительное здоровье для легит режима")
            .range(0, 10).setValue(2)
            .visible(() -> swapMode.isSelected("Legit"));

    final BooleanSetting elytraCheck = new BooleanSetting("Здоровье на элитре", "Отдельный порог здоровья при полёте на элитре")
            .setValue(false);

    final SliderSettings elytraHealth = new SliderSettings("Здоровье элитры", "Порог здоровья при полёте на элитре")
            .range(1, 20).setValue(10)
            .visible(() -> elytraCheck.isValue());

    final BooleanSetting crystalCheck = new BooleanSetting("Кристалл", "Брать тотем при наличии кристалла рядом")
            .setValue(true);

    final SliderSettings crystalDistance = new SliderSettings("Дистанция кристалла", "Максимальное расстояние до кристалла")
            .range(1, 12).setValue(6)
            .visible(() -> crystalCheck.isValue());

    final BooleanSetting noTakeIfHead = new BooleanSetting("Не брать если шар", "Не брать тотем на кристалл если в offhand голова")
            .setValue(false)
            .visible(() -> crystalCheck.isValue());

    final BooleanSetting takeIfLowHp = new BooleanSetting("Брать если мало ХП", "Брать тотем игнорируя голову при низком здоровье")
            .setValue(false)
            .visible(() -> crystalCheck.isValue() && noTakeIfHead.isValue());

    final SliderSettings takeIfLowHpValue = new SliderSettings("ХП для взятия", "Порог здоровья для игнорирования головы")
            .range(1, 20).setValue(4)
            .visible(() -> crystalCheck.isValue() && noTakeIfHead.isValue() && takeIfLowHp.isValue());

    final BooleanSetting fallCheck = new BooleanSetting("Падение", "Брать тотем при падении с высоты")
            .setValue(true);

    final SliderSettings fallHeight = new SliderSettings("Высота падения", "Минимальная высота падения")
            .range(5, 50).setValue(15)
            .visible(() -> fallCheck.isValue());

    final BooleanSetting armorCheck = new BooleanSetting("Не полная броня", "Добавлять здоровье к порогу при неполной броне")
            .setValue(false);

    final SliderSettings armorHealthAdd = new SliderSettings("Доп. здоровье брони", "Дополнительное здоровье к порогу")
            .range(1, 10).setValue(4)
            .visible(() -> armorCheck.isValue());

    final BooleanSetting returnItem = new BooleanSetting("Возвращение предмета", "Возвращать предмет после угрозы")
            .setValue(true);

    final SliderSettings returnDelay = new SliderSettings("Задержка возврата", "Задержка перед возвратом предмета (мс)")
            .range(100, 2000).setValue(500)
            .visible(() -> returnItem.isValue());

    final BooleanSetting saveTalismans = new BooleanSetting("Сохранение талисманов", "Не использовать зачарованные тотемы")
            .setValue(true);

    final BooleanSetting goldenHearts = new BooleanSetting("Золотые сердца", "Учитывать absorption при расчёте здоровья")
            .setValue(true);

    final BooleanSetting maceCheck = new BooleanSetting("Булава", "Брать тотем при наличии игрока с булавой рядом")
            .setValue(false);

    final SliderSettings maceDistance = new SliderSettings("Дистанция булавы", "Максимальное расстояние до игрока с булавой")
            .range(3, 20).setValue(10)
            .visible(() -> maceCheck.isValue());

    final BooleanSetting tntCheck = new BooleanSetting("Динамит", "Брать тотем при наличии динамита рядом")
            .setValue(false);

    final SliderSettings tntDistance = new SliderSettings("Дистанция динамита", "Максимальное расстояние до динамита")
            .range(1, 12).setValue(6)
            .visible(() -> tntCheck.isValue());

    final BooleanSetting tntMinecartCheck = new BooleanSetting("Вагонетка с динамитом", "Брать тотем при наличии вагонетки с динамитом")
            .setValue(false);

    final SliderSettings tntMinecartDistance = new SliderSettings("Дистанция вагонетки", "Максимальное расстояние до вагонетки")
            .range(1, 12).setValue(6)
            .visible(() -> tntMinecartCheck.isValue());

    final SwapExecutor executor = new SwapExecutor();
    final StopWatch safeTimer = new StopWatch();

    int savedSlotId = -1;
    float fallStartY = 0;
    boolean wasFalling = false;

    public AutoTotem() {
        super("AutoTotem", "Auto Totem", ModuleCategory.COMBAT);
        setup(
                swapMode, healthThreshold, anticipation,
                elytraCheck, elytraHealth,
                crystalCheck, crystalDistance, noTakeIfHead, takeIfLowHp, takeIfLowHpValue,
                fallCheck, fallHeight,
                armorCheck, armorHealthAdd,
                returnItem, returnDelay, saveTalismans, goldenHearts,
                maceCheck, maceDistance,
                tntCheck, tntDistance,
                tntMinecartCheck, tntMinecartDistance
        );
    }

    @Override
    public void activate() {
        savedSlotId = -1;
        fallStartY = 0;
        wasFalling = false;
        safeTimer.reset();
    }

    @Override
    public void deactivate() {
        executor.cancel();
        savedSlotId = -1;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        executor.tick();

        if (executor.isRunning()) return;

        updateFallTracking();

        boolean needTotem = shouldEquipTotem();
        boolean hasTotemInOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;

        if (needTotem) {
            safeTimer.reset();

            if (!hasTotemInOffhand) {
                equipTotem();
            }
        } else {
            if (savedSlotId != -1 && hasTotemInOffhand && returnItem.isValue()) {
                if (safeTimer.finished(returnDelay.getValue())) {
                    returnSavedItem();
                }
            } else if (!hasTotemInOffhand) {
                savedSlotId = -1;
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;
        if (executor.isBlocking()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }
    }

    private void updateFallTracking() {
        boolean isFalling = mc.player.getVelocity().y < -0.1
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.isGliding();

        if (isFalling && !wasFalling) {
            fallStartY = (float) mc.player.getY();
        }

        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isClimbing()) {
            fallStartY = (float) mc.player.getY();
        }

        wasFalling = isFalling;
    }

    private boolean shouldEquipTotem() {
        float health = getEffectiveHealth();
        float threshold = getEffectiveThreshold();

        if (health <= threshold) {
            return true;
        }

        if (elytraCheck.isValue() && mc.player.isGliding()) {
            float elytraThreshold = elytraHealth.getValue();
            if (swapMode.isSelected("Legit")) {
                elytraThreshold += anticipation.getValue();
            }
            if (health <= elytraThreshold) {
                return true;
            }
        }

        if (fallCheck.isValue()) {
            float fallDistance = fallStartY - (float) mc.player.getY();
            if (fallDistance >= fallHeight.getValue() && mc.player.getVelocity().y < -0.1) {
                return true;
            }
        }

        if (crystalCheck.isValue() && checkCrystalDanger()) {
            return true;
        }

        if (maceCheck.isValue() && checkMaceDanger()) {
            return true;
        }

        if (tntCheck.isValue() && checkTntDanger()) {
            return true;
        }

        if (tntMinecartCheck.isValue() && checkTntMinecartDanger()) {
            return true;
        }

        return false;
    }

    private float getEffectiveHealth() {
        float health = mc.player.getHealth();
        if (goldenHearts.isValue()) {
            health += mc.player.getAbsorptionAmount();
        }
        return health;
    }

    private float getEffectiveThreshold() {
        float threshold = healthThreshold.getValue();

        if (swapMode.isSelected("Legit")) {
            threshold += anticipation.getValue();
        }

        if (armorCheck.isValue()) {
            int armor = mc.player.getArmor();
            if (armor < 20) {
                threshold += armorHealthAdd.getValue();
            }
        }

        return threshold;
    }

    private boolean isBall() {
        if (fallCheck.isValue() && (fallStartY - (float) mc.player.getY()) > 5.0F) {
            return false;
        }
        return noTakeIfHead.isValue() && mc.player.getOffHandStack().getItem() == Items.PLAYER_HEAD;
    }

    private boolean checkCrystalDanger() {
        if (isBall()) {
            if (takeIfLowHp.isValue() && getEffectiveHealth() <= takeIfLowHpValue.getValue()) {
                return true;
            }
            return false;
        }

        double distance = crystalDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMaceDanger() {
        double distance = maceDistance.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > distance) continue;

            boolean hasMace = player.getMainHandStack().getItem() == Items.MACE
                    || player.getOffHandStack().getItem() == Items.MACE;
            if (!hasMace) continue;

            if (player.getVelocity().y < -0.3 && !player.isOnGround()) {
                double dx = mc.player.getX() - player.getX();
                double dz = mc.player.getZ() - player.getZ();
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                double predictedY = player.getY() + player.getVelocity().y * 10;
                if (predictedY <= mc.player.getY() + 2 && horizontalDist < 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkTntDanger() {
        double distance = tntDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkTntMinecartDanger() {
        double distance = tntMinecartDistance.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TntMinecartEntity) {
                if (mc.player.distanceTo(entity) <= distance) {
                    return true;
                }
            }
        }
        return false;
    }

    private void swapSlots(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        int syncId = mc.player.playerScreenHandler.syncId;

        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
    }

    private boolean isScreenOpen() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen);
    }

    private void equipTotem() {
        Slot totemSlot = findTotemSlot();
        if (totemSlot == null) return;

        boolean hasItemInOffhand = !mc.player.getOffHandStack().isEmpty()
                && mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING;

        if (hasItemInOffhand && savedSlotId == -1) {
            savedSlotId = totemSlot.id;
        }

        final int slotId = totemSlot.id;

        if (swapMode.isSelected("Instant") || isScreenOpen()) {
            swapSlots(slotId);
        } else {
            executor.execute(() -> swapSlots(slotId), SwapSettings.legit());
        }
    }

    private void returnSavedItem() {
        if (savedSlotId == -1) return;

        final int slotId = savedSlotId;

        if (swapMode.isSelected("Instant") || isScreenOpen()) {
            swapSlots(slotId);
            savedSlotId = -1;
        } else {
            executor.execute(() -> {
                swapSlots(slotId);
                savedSlotId = -1;
            }, SwapSettings.legit());
        }
    }

    private Slot findTotemSlot() {
        if (mc.player == null) return null;

        for (int i = 36; i <= 44; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isValidTotem(slot.getStack())) {
                return slot;
            }
        }

        for (int i = 9; i <= 35; i++) {
            Slot slot = mc.player.playerScreenHandler.getSlot(i);
            if (slot != null && isValidTotem(slot.getStack())) {
                return slot;
            }
        }

        return null;
    }

    private boolean isValidTotem(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.TOTEM_OF_UNDYING) return false;

        if (saveTalismans.isValue() && stack.hasEnchantments()) {
            return false;
        }

        return true;
    }
}