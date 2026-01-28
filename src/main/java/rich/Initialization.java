package rich;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import rich.manager.Manager;
import rich.util.mods.config.wave.ResourceManager;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class Initialization implements ClientModInitializer {

    @Getter
    private static Initialization instance;

    @Getter
    private Manager manager;

    @Override
    public void onInitializeClient() {
        ResourceManager.onClientInit();
    }

    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
        ResourceManager.onClientInit();
    }
}