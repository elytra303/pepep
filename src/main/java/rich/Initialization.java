package rich;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import rich.manager.Manager;

import javax.swing.*;

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
    }

    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
    }
}