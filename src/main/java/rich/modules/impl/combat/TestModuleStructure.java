package rich.modules.impl.combat;


import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.implement.*;

public class TestModuleStructure extends ModuleStructure {
    
    private final BooleanSetting booleanTest = new BooleanSetting("Boolean Test", "Тест чекбокса")
            .setValue(true);
    
    private final BooleanSetting booleanTest2 = new BooleanSetting("Another Boolean", "Еще один чекбокс")
            .setValue(false);
    
    private final SliderSettings sliderInt = new SliderSettings("Integer Slider", "Тест целочисленного слайдера")
            .range(0, 100)
            .setValue(50);
    
    private final SliderSettings sliderFloat = new SliderSettings("Float Slider", "Тест дробного слайдера")
            .range(0.0f, 10.0f)
            .setValue(5.0f);
    
    private final SelectSetting selectMode = new SelectSetting("Select Mode", "Выберите режим")
            .value("Mode 1", "Mode 2", "Mode 3", "Mode 4")
            .selected("Mode 1");
    
    private final MultiSelectSetting multiSelect = new MultiSelectSetting("Multi Select", "Выберите несколько опций")
            .value("Option A", "Option B", "Option C", "Option D", "Option E");
    
    private final ColorSetting colorPicker = new ColorSetting("Color Picker", "Выберите цвет")
            .value(0xFF00FF00);
    
    private final TextSetting textInput = new TextSetting("Text Input", "Введите текст")
            .setMin(0)
            .setMax(50)
            .setText("Hello World");
    
    private final TextSetting textShort = new TextSetting("Short Text", "Короткий текст")
            .setMin(1)
            .setMax(10)
            .setText("Test");
    
    private final BindSetting bindKey = new BindSetting("Bind Key", "Назначьте клавишу");
    
    private final ButtonSetting buttonAction = new ButtonSetting("Button Action", "Нажмите для действия")
            .setRunnable(() -> {})
            .setButtonName("Click Me");
    
    private final ButtonSetting buttonReset = new ButtonSetting("Reset Settings", "Сбросить все настройки")
            .setRunnable(this::resetAllSettings)
            .setButtonName("Reset");
    
    private final BooleanSetting advancedMode = new BooleanSetting("Advanced Mode", "Показать дополнительные настройки")
            .setValue(false);;
    
    private final SliderSettings advancedSlider = new SliderSettings("Advanced Slider", "Продвинутый слайдер")
            .range(0, 1000)
            .setValue(500)
            .visible(() -> advancedMode.isValue());
    
    private final ColorSetting advancedColor = new ColorSetting("Advanced Color", "Продвинутый цвет")
            .value(0xFFFF0000)
            .visible(() -> advancedMode.isValue());

    public TestModuleStructure() {
        super("TestModule", "Test Module", ModuleCategory.COMBAT);
        setup(
            booleanTest,
            booleanTest2,
            sliderInt,
            sliderFloat,
            selectMode,
            multiSelect,
            colorPicker,
            textInput,
            textShort,
            bindKey,
            buttonAction,
            buttonReset,
            advancedMode,
            advancedSlider,
            advancedColor
        );
    }
    
    private void resetAllSettings() {
        booleanTest.setValue(true);
        booleanTest2.setValue(false);
        sliderInt.setValue(50);
        sliderFloat.setValue(5.0f);
        selectMode.setSelected("Mode 1");
        multiSelect.getSelected().clear();
        colorPicker.setColor(0xFF00FF00);
        textInput.setText("Hello World");
        textShort.setText("Test");
        bindKey.setKey(-1);
        advancedMode.setValue(false);
        advancedSlider.setValue(500);
        advancedColor.setColor(0xFFFF0000);
    }
}