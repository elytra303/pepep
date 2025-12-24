package rich.util.inventory;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.events.impl.InputEvent;
import rich.screens.clickgui.ClickGui;

import java.util.List;

@UtilityClass
public class InventoryFlowManager implements IMinecraft {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public static final Script script = new Script(), postScript = new Script();
    public boolean canMove = true;

    public void tick() {
        script.update();
    }

    public void postMotion() {
        postScript.update();
    }

    public void input(InputEvent e) {
        if (!canMove) e.inputNone();
    }

    public void addTask(Runnable task) {
        if (script.isFinished()) {
            switch (Network.server) {
                case "FunTime" -> {
                    script.cleanup().addTickStep(0, () -> {
                        InventoryFlowManager.disableMoveKeys();

                    }).addTickStep(1, () -> {
                        task.run();
                        enableMoveKeys();
                    });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player.isOnGround()) {
                        return;
                    }
                }
                case "SpookyTime", "CopyTime" -> {
                    script.cleanup().addTickStep(0, ()-> {

                            }).addTickStep(1, task::run)
                            .addTickStep(2, InventoryFlowManager::enableMoveKeys);
                    return;
                }
            }
        }
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            InventoryTask.closeScreen(true);
        });
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        InventoryTask.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public boolean isChat(Screen screen) {return screen instanceof ChatScreen;}

    public boolean shouldSkipExecution() {
        return mc.currentScreen != null && !isChat(mc.currentScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof ClickGui);
    }

    public void updateMoveKeys() {
        long windowHandle = mc.getWindow().getHandle();
        moveKeys.forEach(keyBinding -> {
            int keyCode = keyBinding.getDefaultKey().getCode();
            boolean pressed = GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
            keyBinding.setPressed(pressed);
        });
    }

}