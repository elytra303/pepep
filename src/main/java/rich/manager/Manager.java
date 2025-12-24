package rich.manager;

import lombok.Getter;
import rich.events.api.EventManager;
import rich.modules.module.*;
import rich.screens.clickgui.ClickGui;
import rich.util.render.RenderCore;
import rich.screens.hud.drags.DraggableRepository;

@Getter
public class Manager {

    private EventManager eventManager;
    private RenderCore renderCore;
    private DraggableRepository draggableRepository;
    private ModuleProvider moduleProvider;
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;

    public void init() {
        clickgui = new ClickGui();
        eventManager = new EventManager();
        renderCore = new RenderCore();
        draggableRepository = new DraggableRepository();
        draggableRepository.setup();
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
    }
}