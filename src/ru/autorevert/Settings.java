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
	public static final Integer MIN_MINUTES_TO_REVERT = 1;
	public static final Integer MAX_MINUTES_TO_REVERT = 99;
	private static final int DEFAULT_MINUTES_TILL_REVERT = 2;

	public int minutesTillRevert = DEFAULT_MINUTES_TILL_REVERT;

	public int secondsTillRevert() {
		return minutesTillRevert * 60;
	}

	@Override public Settings getState() {
		return this;
	}

	@Override public void loadState(Settings state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	@SuppressWarnings("RedundantIfStatement")
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Settings settings = (Settings) o;

		if (minutesTillRevert != settings.minutesTillRevert) return false;

		return true;
	}

	@Override public int hashCode() {
		return minutesTillRevert;
	}

	@Override public String toString() {
		return "Settings{minutesTillRevert=" + minutesTillRevert + '}';
	}
}
