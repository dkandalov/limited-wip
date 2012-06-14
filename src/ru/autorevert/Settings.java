package ru.autorevert;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * User: dima
 * Date: 14/06/2012
 */
@State(name = "AutoRevertSettings", storages = {@Storage(id = "other", file = "$APP_CONFIG$/autorevert.settings.xml")})
public class Settings implements PersistentStateComponent<Settings>  {

	@Override public Settings getState() {
		return this;
	}

	@Override public void loadState(Settings state) {
		XmlSerializerUtil.copyBean(state, this);
	}
}
