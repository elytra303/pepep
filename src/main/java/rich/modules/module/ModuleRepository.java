package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.TriggerBot;
import rich.modules.impl.misc.AutoBuy;
import rich.modules.impl.misc.ElytraHelper;
import rich.modules.impl.misc.autoparser.AutoParser;
import rich.modules.impl.misc.autoparser.dev.ItemParser;
import rich.modules.impl.movement.AutoSprint;
import rich.modules.impl.movement.Jesus;
import rich.modules.impl.movement.NoWeb;
import rich.modules.impl.movement.Speed;
import rich.modules.impl.player.AutoRespawn;
import rich.modules.impl.player.NoDelay;
import rich.modules.impl.player.NoEntityTrace;
import rich.modules.impl.player.NoPush;
import rich.modules.impl.render.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();
    List<ModuleStructure> hiddenModules = new ArrayList<>();

    public void setup() {

        //                new ItemParser(),

        register(
                new Hud(),
                new Aura(),
                new TriggerBot(),
                new ElytraHelper(),
                new Jesus(),
                new NoWeb(),
                new FullBright(),
                new NoDelay(),
                new SeeInvisible(),
                new ViewModel(),
                new Speed(),
//                new SwingAnimation(),
                new NoEntityTrace(),
                new AutoRespawn(),
                new NoPush(),
                new AutoSprint(),
                new AutoBuy()
        );

        registerHidden(
                new AutoParser()
        );
    }

    public void register(ModuleStructure... moduleStructure) {
        moduleStructures.addAll(List.of(moduleStructure));
    }

    public void registerHidden(ModuleStructure... moduleStructure) {
        for (ModuleStructure module : moduleStructure) {
            hiddenModules.add(module);
            module.setState(true);
        }
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }

    public List<ModuleStructure> hiddenModules() {
        return hiddenModules;
    }

    public List<ModuleStructure> allModules() {
        List<ModuleStructure> all = new ArrayList<>(moduleStructures);
        all.addAll(hiddenModules);
        return all;
    }
}