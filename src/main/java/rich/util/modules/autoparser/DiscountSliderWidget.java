package rich.util.modules.autoparser;

import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class DiscountSliderWidget extends SliderWidget {
    private final AutoParserConfig config;

    public DiscountSliderWidget(int x, int y, int width, int height, int initialValue) {
        super(x, y, width, height, Text.literal("Занижать цены на: " + initialValue + "%"), (initialValue - 10) / 80.0);
        this.config = AutoParserConfig.getInstance();
    }

    @Override
    protected void updateMessage() {
        int percent = (int) (this.value * 80) + 10;
        this.setMessage(Text.literal("Занижать цены на: " + percent + "%"));
    }

    @Override
    protected void applyValue() {
        int percent = (int) (this.value * 80) + 10;
        config.setDiscountPercent(percent);
    }
}