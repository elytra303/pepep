package rich.modules.impl.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.events.api.EventHandler;
import rich.events.impl.DeathScreenEvent;
import rich.events.impl.PacketEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;

@SuppressWarnings("all")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoRespawn extends ModuleStructure {

    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите, что будет использоваться").value("Default");

    public AutoRespawn() {
        super("AutoRespawn", "Auto Respawn", ModuleCategory.PLAYER);
        setup(modeSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
//        switch (e.getPacket()) {
//            case DeathMessageS2CPacket message when Network.getWorldType().equals("lobby") && modeSetting.isSelected("FunTime Back") -> {
//                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(1448, 1337, 228, false, false));
//                mc.player.requestRespawn();
//                mc.player.closeScreen();
//            }
//            default -> {
//            }
//        }
    }

    
    @EventHandler
    public void onDeathScreen(DeathScreenEvent e) {
        if (modeSetting.isSelected("Default")) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
