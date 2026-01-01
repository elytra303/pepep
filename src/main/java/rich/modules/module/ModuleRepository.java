package rich.modules.module;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.impl.combat.*;
import rich.modules.impl.misc.AutoBuy;
import rich.modules.impl.misc.ItemParser;
import rich.modules.impl.render.*;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleRepository {
    List<ModuleStructure> moduleStructures = new ArrayList<>();

    public void setup() {
        register(
                new Hud(),
                new ItemParser(),
                new AutoBuy()
        );
    }

    public void register(ModuleStructure... moduleStructure) {
        moduleStructures.addAll(List.of(moduleStructure));
    }

    public List<ModuleStructure> modules() {
        return moduleStructures;
    }
}