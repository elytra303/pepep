package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import rich.modules.module.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClickGuiModule extends ModuleStructure {
    public static ClickGuiModule getInstance() {
        return Instance.get(ClickGuiModule.class);
    }

    public SelectSetting animationType = new SelectSetting("Animation Type", "Выберите тип анимации")
            .value("InOutBack", "OutBack", "InOutCirc", "Decelerate", "Linear")
            .selected("InOutBack");

    public SliderSettings animationDuration = new SliderSettings("Animation Duration", "Длительность анимации в миллисекундах")
            .range(100, 2000)
            .setValue(600);

    public SelectSetting animationDirection = new SelectSetting("Animation Direction", "Направление появления панелей")
            .value("Left", "Right", "Top", "Bottom", "Center")
            .selected("Left");

    public SliderSettings animationOffset = new SliderSettings("Animation Offset", "Смещение анимации")
            .range(0, 500)
            .setValue(50);

    public BooleanSetting smoothAnimation = new BooleanSetting("Smooth Animation", "Плавная анимация")
            .setValue(true);

    public BooleanSetting staggeredAnimation = new BooleanSetting("Staggered Animation", "Последовательная анимация панелей")
            .setValue(false);

    public SliderSettings staggerDelay = new SliderSettings("Stagger Delay", "Задержка между панелями")
            .range(0, 200)
            .setValue(50)
            .visible(() -> staggeredAnimation.isValue());

    public BooleanSetting fadeAnimation = new BooleanSetting("Fade Animation", "Плавное появление/исчезновение")
            .setValue(true);

    public SliderSettings fadeSpeed = new SliderSettings("Fade Speed", "Скорость затухания")
            .range(0.1f, 5.0f)
            .setValue(1.0f)
            .visible(() -> fadeAnimation.isValue());

    public BooleanSetting scaleAnimation = new BooleanSetting("Scale Animation", "Анимация масштабирования")
            .setValue(false);

    public SliderSettings scaleFrom = new SliderSettings("Scale From", "Начальный масштаб")
            .range(0.1f, 2.0f)
            .setValue(0.8f)
            .visible(() -> scaleAnimation.isValue());

    public BooleanSetting rotationAnimation = new BooleanSetting("Rotation Animation", "Анимация вращения")
            .setValue(false);

    public SliderSettings rotationDegrees = new SliderSettings("Rotation Degrees", "Градусы вращения")
            .range(0, 360)
            .setValue(45)
            .visible(() -> rotationAnimation.isValue());

    public BooleanSetting bounceEffect = new BooleanSetting("Bounce Effect", "Эффект отскока в конце анимации")
            .setValue(false);

    public SliderSettings bouncePower = new SliderSettings("Bounce Power", "Сила отскока")
            .range(0.1f, 3.0f)
            .setValue(1.5f)
            .visible(() -> bounceEffect.isValue());

    public BooleanSetting blurBackground = new BooleanSetting("Blur Background", "Размытие фона во время анимации")
            .setValue(false);

    public SliderSettings blurAmount = new SliderSettings("Blur Amount", "Степень размытия")
            .range(0, 20)
            .setValue(10)
            .visible(() -> blurBackground.isValue());

    public SelectSetting easingFunction = new SelectSetting("Easing Function", "Функция сглаживания")
            .value("EaseInOut", "EaseIn", "EaseOut", "Linear", "Elastic")
            .selected("EaseInOut");

    public BooleanSetting reverseOnClose = new BooleanSetting("Reverse On Close", "Обратная анимация при закрытии")
            .setValue(true);

    public int getAnimationDuration() {
        return animationDuration.getInt();
    }

    public String getAnimationType() {
        return animationType.getSelected();
    }

    public String getAnimationDirection() {
        return animationDirection.getSelected();
    }

    public float getAnimationOffset() {
        return animationOffset.getValue();
    }

    public boolean isSmoothAnimation() {
        return smoothAnimation.isValue();
    }

    public boolean isStaggeredAnimation() {
        return staggeredAnimation.isValue();
    }

    public int getStaggerDelay() {
        return staggerDelay.getInt();
    }

    public boolean isFadeAnimation() {
        return fadeAnimation.isValue();
    }

    public float getFadeSpeed() {
        return fadeSpeed.getValue();
    }

    public boolean isScaleAnimation() {
        return scaleAnimation.isValue();
    }

    public float getScaleFrom() {
        return scaleFrom.getValue();
    }

    public boolean isRotationAnimation() {
        return rotationAnimation.isValue();
    }

    public int getRotationDegrees() {
        return rotationDegrees.getInt();
    }

    public boolean isBounceEffect() {
        return bounceEffect.isValue();
    }

    public float getBouncePower() {
        return bouncePower.getValue();
    }

    public boolean isBlurBackground() {
        return blurBackground.isValue();
    }

    public int getBlurAmount() {
        return blurAmount.getInt();
    }

    public String getEasingFunction() {
        return easingFunction.getSelected();
    }

    public boolean isReverseOnClose() {
        return reverseOnClose.isValue();
    }

    public ClickGuiModule() {
        super("ClickGui Settings", ModuleCategory.RENDER);
        setup(
                animationType,
                animationDuration,
                animationDirection,
                animationOffset,
                smoothAnimation,
                staggeredAnimation,
                staggerDelay,
                fadeAnimation,
                fadeSpeed,
                scaleAnimation,
                scaleFrom,
                rotationAnimation,
                rotationDegrees,
                bounceEffect,
                bouncePower,
                blurBackground,
                blurAmount,
                easingFunction,
                reverseOnClose
        );
    }
}