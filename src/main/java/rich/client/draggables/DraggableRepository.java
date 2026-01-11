package rich.client.draggables;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DraggableRepository {
    List<AbstractDraggable> draggable = new ArrayList<>();

    public void setup() {
        register(
//                new Watermark(),
        );
    }

    public void register(AbstractDraggable... module) {
        draggable.addAll(Arrays.asList(module));
    }

    public List<AbstractDraggable> draggable() {
        return draggable;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractDraggable> T get(String name) {
        return (T) draggable.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public <T extends AbstractDraggable> T get(Class<T> clazz) {
        return draggable.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}