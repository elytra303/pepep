package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.*;
import rich.modules.impl.combat.aura.NoInteract;
import rich.modules.impl.misc.*;
import rich.modules.impl.misc.autoparser.AutoParser;
import rich.modules.impl.movement.*;
import rich.modules.impl.player.*;
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
                new ClientSounds(),
                new HitSound(),
                new TargetStrafe(),
                new AutoLeave(),
                new Strafe(),
                new AutoDuel(),
                new NoWeb(),
                new AutoTpAccept(),
                new ClickFriend(),
                new FreeLook(),
                new FullBright(),
                new NoDelay(),
                new SeeInvisible(),
                new NoFallDamage(),
                new ShiftTap(),
                new HitBoxModule(),
                new NoFriendDamage(),
                new ProjectileHelper(),
                new NoInteract(),
                new AntiBot(),
                new ViewModel(),
                new SuperFireWork(),
                new LongJump(),
                new ElytraTarget(),
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