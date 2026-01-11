package rich.manager;

import lombok.Getter;
import rich.command.CommandManager;
import rich.events.api.EventManager;
import rich.modules.impl.combat.aura.attack.StrikerConstructor;
import rich.modules.module.*;
import rich.screens.clickgui.ClickGui;
import rich.util.config.ConfigSystem;
import rich.util.config.impl.bind.BindConfig;
import rich.util.config.impl.blockesp.BlockESPConfig;
import rich.util.config.impl.friend.FriendConfig;
import rich.util.config.impl.prefix.PrefixConfig;
import rich.util.config.impl.proxy.ProxyConfig;
import rich.util.config.impl.staff.StaffConfig;
import rich.util.modules.ModuleProvider;
import rich.util.modules.ModuleSwitcher;
import rich.util.render.RenderCore;
import rich.util.render.Scissor;
import rich.client.draggables.DraggableRepository;
import rich.util.repository.macro.MacroRepository;
import rich.util.repository.way.WayRepository;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@Getter
public class Manager {
    public StrikerConstructor attackPerpetrator = new StrikerConstructor();
    private EventManager eventManager;
    private RenderCore renderCore;
    private Scissor scissor;
    private DraggableRepository draggableRepository;
    private ModuleProvider moduleProvider;
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;
    private ConfigSystem configSystem;
    private CommandManager commandManager;

    public void init() {
        MacroRepository.getInstance().init();
        WayRepository.getInstance().init();
        BlockESPConfig.getInstance().load();
        FriendConfig.getInstance().load();
        PrefixConfig.getInstance().load();
        StaffConfig.getInstance().load();
        ProxyConfig.getInstance().load();
        BindConfig.getInstance();

        clickgui = new ClickGui();
        eventManager = new EventManager();
        renderCore = new RenderCore();
        scissor = new Scissor();
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
        configSystem = new ConfigSystem();
        configSystem.init();
        commandManager = new CommandManager();
        commandManager.init();
    }
}