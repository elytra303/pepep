package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class TextComponent extends AbstractSettingComponent {
    public static boolean typing = false;
    private final TextSetting textSetting;
    private boolean focused = false;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private String text = "";
    private float focusAnimation = 0f;

    public TextComponent(TextSetting setting) {
        super(setting);
        this.textSetting = setting;
        this.text = textSetting.getText() != null ? textSetting.getText() : "";
        this.cursorPosition = text.length();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        focusAnimation += (focused ? 1f : 0f - focusAnimation) * 0.2f;
        focusAnimation = Math.max(0f, Math.min(1f, focusAnimation));

        int glassAlpha = focused ? 35 : (hovered ? 25 : 20);
        Render2D.rect(x, y, width, height, new Color(255, 255, 255, glassAlpha).getRGB(), 6f);

        if (focused) {
            Render2D.outline(x, y, width, height, 1.0f, new Color(100, 180, 255, 150).getRGB(), 6f);
        } else if (hovered) {
            Render2D.outline(x, y, width, height, 1.0f, new Color(255, 255, 255, 30).getRGB(), 6f);
        }

        String label = textSetting.getName() + ": ";
        float labelWidth = Fonts.BOLD.getWidth(label, 7);

        Fonts.BOLD.draw(label, x + 6, y + height / 2 - 3.5f, 7, new Color(200, 205, 210, 190).getRGB());

        if (focused && hasSelection()) {
            int start = getStartOfSelection();
            int end = getEndOfSelection();
            String beforeSelection = text.substring(0, start);
            String selection = text.substring(start, end);

            float selectionX = x + 6 + labelWidth + Fonts.BOLD.getWidth(beforeSelection, 7);
            float selectionWidth = Fonts.BOLD.getWidth(selection, 7);

            Render2D.rect(selectionX, y + 3, selectionWidth, height - 6, new Color(100, 180, 255, 100).getRGB(), 3f);
        }

        String displayText = text.isEmpty() && !focused ? "" : text;
        Color textColor = focused ? new Color(230, 230, 235, 220) : new Color(210, 210, 220, 200);
        Fonts.BOLD.draw(displayText, x + 6 + labelWidth, y + height / 2 - 3.5f, 7, textColor.getRGB());

        if (focused && !hasSelection()) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime % 1000) < 500) {
                String beforeCursor = text.substring(0, cursorPosition);
                float cursorX = x + 6 + labelWidth + Fonts.BOLD.getWidth(beforeCursor, 7);
                int cursorAlpha = Math.max(0, Math.min(255, (int)(255 * focusAnimation)));
                Render2D.rect(cursorX, y + 3, 1, height - 6, new Color(120, 180, 255, cursorAlpha).getRGB(), 0f);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasInside = isHover(mouseX, mouseY);

        if (wasInside && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250 && focused) {
                selectAllText();
            } else {
                focused = true;
                typing = true;
                cursorPosition = getCursorIndexAt(mouseX);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            lastClickTime = currentTime;
            return true;
        } else if (!wasInside && focused) {
            applyText();
            focused = false;
            typing = false;
            clearSelection();
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (focused && button == 0) {
            cursorPosition = getCursorIndexAt(mouseX);
            selectionEnd = cursorPosition;
            return true;
        }
        return false;
    }

    private boolean isControlDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean isShiftDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (isControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    selectAllText();
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    pasteFromClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    copyToClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSelection()) {
                        copyToClipboard();
                        deleteSelectedText();
                    }
                    return true;
                }
            }
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    handleBackspace();
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    handleDelete();
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    moveCursor(-1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveCursor(1);
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    cursorPosition = 0;
                    updateSelectionAfterCursorMove();
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    cursorPosition = text.length();
                    updateSelectionAfterCursorMove();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    applyText();
                    focused = false;
                    typing = false;
                    return true;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    text = textSetting.getText() != null ? textSetting.getText() : "";
                    cursorPosition = text.length();
                    focused = false;
                    typing = false;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        if (Character.isISOControl(chr)) {
            return false;
        }

        int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;

        if (text.length() < maxLength || hasSelection()) {
            deleteSelectedText();
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            clearSelection();
            return true;
        }

        return false;
    }

    @Override
    public void tick() {
        focusAnimation += (focused ? 1f : 0f - focusAnimation) * 0.2f;
        focusAnimation = Math.max(0f, Math.min(1f, focusAnimation));
    }

    private void applyText() {
        int minLength = textSetting.getMin() > 0 ? textSetting.getMin() : 0;
        int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;

        if (text.length() >= minLength && text.length() <= maxLength) {
            textSetting.setText(text);
        } else {
            text = textSetting.getText() != null ? textSetting.getText() : "";
            cursorPosition = text.length();
        }
    }

    private void handleBackspace() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        } else if (cursorPosition > 0) {
            replaceText(cursorPosition - 1, cursorPosition, "");
        }
    }

    private void handleDelete() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        } else if (cursorPosition < text.length()) {
            text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
        }
    }

    private void moveCursor(int direction) {
        if (hasSelection() && !isShiftDown()) {
            if (direction < 0) {
                cursorPosition = getStartOfSelection();
            } else {
                cursorPosition = getEndOfSelection();
            }
            clearSelection();
        } else {
            if (direction < 0 && cursorPosition > 0) {
                cursorPosition--;
            } else if (direction > 0 && cursorPosition < text.length()) {
                cursorPosition++;
            }
            updateSelectionAfterCursorMove();
        }
    }

    private void updateSelectionAfterCursorMove() {
        if (isShiftDown()) {
            if (selectionStart == -1) {
                selectionStart = selectionEnd != -1 ? selectionEnd : cursorPosition;
            }
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
    }

    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\n\r\t]", "");

            if (hasSelection()) {
                deleteSelectedText();
            }

            int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;
            int remainingSpace = maxLength - text.length();
            if (clipboardText.length() > remainingSpace) {
                clipboardText = clipboardText.substring(0, remainingSpace);
            }

            if (!clipboardText.isEmpty()) {
                text = text.substring(0, cursorPosition) + clipboardText + text.substring(cursorPosition);
                cursorPosition += clipboardText.length();
            }
        }
    }

    private void copyToClipboard() {
        if (hasSelection()) {
            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), getSelectedText());
        }
    }

    private void selectAllText() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
    }

    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        return text.substring(getStartOfSelection(), getEndOfSelection());
    }

    private int getStartOfSelection() {
        return Math.min(selectionStart, selectionEnd);
    }

    private int getEndOfSelection() {
        return Math.max(selectionStart, selectionEnd);
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private int getCursorIndexAt(double mouseX) {
        String label = textSetting.getName() + ": ";
        float labelWidth = Fonts.BOLD.getWidth(label, 7);
        float relativeX = (float)(mouseX - x - 6 - labelWidth);

        if (relativeX <= 0) return 0;

        int position = 0;
        float lastWidth = 0;

        while (position < text.length()) {
            float currentWidth = Fonts.BOLD.getWidth(text.substring(0, position + 1), 7);
            float midPoint = (lastWidth + currentWidth) / 2;

            if (relativeX < midPoint) {
                return position;
            }

            lastWidth = currentWidth;
            position++;
        }

        return text.length();
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}