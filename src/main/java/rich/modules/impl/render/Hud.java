package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends ModuleStructure {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Элементы", "Настройка элементов интерфейса")
            .value("Watermark", "HotKeys", "Potions", "Staff", "TargetHud", "Binds", "CoolDowns", "Inventory", "Info", "Notifications")
            .selected("Watermark", "HotKeys", "Potions", "Staff", "TargetHud", "Binds", "CoolDowns", "Inventory", "Info", "Notifications");

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "Показывать блоки в секунду")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Info"));

    public BooleanSetting showTps = new BooleanSetting("Show TPS", "Показывать TPS в Watermark")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public MultiSelectSetting notificationSettings = new MultiSelectSetting("Уведомления", "Выберите, когда будут появляться уведомления")
            .value("Module Switch", "Staff Join", "Staff Leave", "Item Pick Up", "Auto Armor", "Break Shield")
            .selected("Module Switch", "Item Pick Up", "Auto Armor", "Break Shield")
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public SliderSettings soundVolumeSetting = new SliderSettings("Sound Volume", "Volume for module switch sounds")
            .range(0.0f, 1.0f)
            .setValue(1.0f)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        setup(interfaceSettings, showBps, showTps, notificationSettings, soundVolumeSetting);
    }
}