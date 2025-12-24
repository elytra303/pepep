package rich.util.interfaces;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rich.modules.module.setting.Setting;

@Getter
@RequiredArgsConstructor
public abstract class AbstractSettingComponent extends AbstractComponent {
    private final Setting setting;
}
