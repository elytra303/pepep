package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.*;
import rich.modules.impl.movement.*;
import rich.modules.impl.render.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();

    public void setup() {
        register(
                new TestModuleStructure(),
                new InventoryMove(),
                new InventoryMove1(),
                new InventoryMove2(),
                new TestModule(),
                new TestModule2(),
                new TestModule3(),
                new TestModule4(),
                new TestModule5(),
                new TestModule6(),
                new TestModule7(),
                new TestModule8(),
                new TestModule9(),
                new TestModule10(),
                new TestModule11(),
                new TestModule12(),
                new Hud(),
                new ClickGuiModule()
        );
    }

    public void register(ModuleStructure... moduleStructure) {
        moduleStructures.addAll(List.of(moduleStructure));
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }
}