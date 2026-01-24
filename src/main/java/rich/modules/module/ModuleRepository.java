package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.*;
import rich.modules.impl.combat.NoInteract;
import rich.modules.impl.combat.AutoTotem;
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
                new HitEffect(),
                new Esp(),
                new BlockESP(),
                new AutoTool(),
                new RegionExploit(),
                new AuctionHelper(),
                new GlassHands(),
                new ChunkAnimator(),
                new MaceTarget(),
                new TriggerBot(),
                new BowSpammer(),
                new AutoTotem(),
                new TapeMouse(),
                new ElytraHelper(),
                new ChinaHat(),
                new AutoPotion(),
                new Jesus(),
                new ClientSounds(),
                new AutoGApple(),
                new ServerHelper(),
                new WindJump(),
                new TargetESP(),
                new BlockOverlay(),
                new HitSound(),
                new ClickPearl(),
                new JumpCircle(),
                new ItemScroller(),
                new TargetStrafe(),
                new AutoLeave(),
                new Strafe(),
                new AutoDuel(),
                new NoWeb(),
//                new AutoCrystal(),
                new AutoTpAccept(),
                new Spider(),
                new ClickFriend(),
                new FreeLook(),
                new Fly(),
                new ElytraMotion(),
                new FullBright(),
                new CameraSettings(),
                new ItemPhysic(),
                new NoDelay(),
                new ServerRPSpoofer(),
                new SeeInvisible(),
                new AutoPilot(),
                new NoFallDamage(),
                new NoRender(),
                new ShiftTap(),
                new HitBoxModule(),
                new WaterSpeed(),
                new NameProtect(),
                new NoFriendDamage(),
                new ProjectileHelper(),
                new InventoryMove(),
                new ChestStealer(),
                new NoInteract(),
                new AntiBot(),
                new ViewModel(),
                new SuperFireWork(),
                new LongJump(),
                new ElytraTarget(),
                new FreeCam(),
                new Speed(),
                new NoEntityTrace(),
                new AutoRespawn(),
                new AutoSwap(),
                new NoPush(),
                new NoSlow(),
                new Velocity(),
                new SwingAnimation(),
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