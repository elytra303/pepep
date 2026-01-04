package rich.modules.impl.misc;

import rich.events.api.EventHandler;
import rich.events.impl.ModuleToggleEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.sounds.SoundManager;

public class ClientSounds extends ModuleStructure {

    public static ClientSounds getInstance() {
        return Instance.get(ClientSounds.class);
    }

    private final SelectSetting soundType = new SelectSetting("Тип звука", "Select sound type")
            .value("New", "Old")
            .selected("New");

    private final SliderSettings volume = new SliderSettings("Громкость", "Set volume")
            .range(0.1f, 2.0f)
            .setValue(1.0f);


    public ClientSounds() {
        super("ClientSounds", ModuleCategory.MISC);
        setup(soundType, volume);
    }

    @EventHandler
    public void onModuleToggle(ModuleToggleEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getModule() == this) return;

        float vol = volume.getValue();

        if (event.isEnabled()) {
            if (soundType.isSelected("New")) {
                SoundManager.playSound(SoundManager.MODULE_ENABLE, vol, 1);
            } else {
                SoundManager.playSound(SoundManager.ON, vol, 1);
            }
        } else {
            if (soundType.isSelected("New")) {
                SoundManager.playSound(SoundManager.MODULE_DISABLE, vol, 1);
            } else {
                SoundManager.playSound(SoundManager.OFF, vol, 1);
            }
        }
    }
}