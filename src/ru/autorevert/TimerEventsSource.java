package ru.autorevert;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class TimerEventsSource implements ApplicationComponent {

	private final Timer timer = new Timer();
	private final List<Listener> listeners = new ArrayList<Listener>();

	@Override public void initComponent() {
		timer.schedule(new TimerTask() {
			@Override public void run() {
				for (Listener listener : listeners) {
					listener.onTimerEvent();
				}
				System.out.println("TimerEventsSource.run");
			}
		}, 0, 1000);
	}

	@Override public void disposeComponent() {
		timer.cancel();
		timer.purge();
	}

	@NotNull @Override public String getComponentName() {
		return "TimerEventsSource";
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public interface Listener {
		void onTimerEvent();
	}
}
