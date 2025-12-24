package rich.screens.clickgui.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rich.modules.module.setting.Setting;
import rich.util.interfaces.AbstractComponent;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
}
