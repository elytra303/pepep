package rich.util.render.font;

import java.util.LinkedHashMap;
import java.util.Map;

public class Fonts {

    private static final Map<String, String> FONT_REGISTRY = new LinkedHashMap<>();

    public static final Font BOLD = register("bold", "bold");
    public static final Font ICONS = register("icons", "icons");
    public static final Font GUI_ICONS = register("guiicons", "guiicons");
    public static final Font CATEGORY_ICONS = register("categoryicons", "categoryicons");

    private static Font register(String name, String path) {
        FONT_REGISTRY.put(name, path);
        return new Font(name);
    }

    public static Map<String, String> getRegistry() {
        return FONT_REGISTRY;
    }

    private Fonts() {}
}