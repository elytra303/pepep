package rich.client.draggables;

import net.minecraft.client.gui.DrawContext;
import rich.events.impl.PacketEvent;
import rich.events.impl.SetScreenEvent;

public interface Draggable {
    boolean visible();

    void tick();

    void render(DrawContext context, int mouseX, int mouseY, float delta);

    void packet(PacketEvent e);

    void setScreen(SetScreenEvent screen);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseReleased(double mouseX, double mouseY, int button);
}
