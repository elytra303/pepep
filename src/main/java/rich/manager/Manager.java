package rich.manager;

import lombok.Getter;
import rich.command.CommandManager;
import rich.events.api.EventManager;
import rich.modules.module.*;
import rich.screens.clickgui.ClickGui;
import rich.util.config.ConfigSystem;
import rich.util.modules.ModuleProvider;
import rich.util.modules.ModuleSwitcher;
import rich.util.render.RenderCore;
import rich.util.render.Scissor;
import rich.screens.hud.drags.DraggableRepository;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

@Getter
public class Manager {

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