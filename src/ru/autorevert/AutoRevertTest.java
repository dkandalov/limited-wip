package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * 	TODO what if change list is switched? ==> should notice and warn
 *
 * 	com.intellij.openapi.vcs.changes.actions.RollbackAction
 * 	com.intellij.openapi.vcs.checkin.CheckinHandler
 *
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {

	@Test
	public void whenStartedShouldScheduleRevertTask() {
		Actions actions = mock(Actions.class);
		MyTimer timer = mock(MyTimer.class);
		Config config = new Config();
		AutoRevert autoRevert = new AutoRevert(config, actions, timer);

		autoRevert.start();
		verify(timer).schedule(any(MyTimer.Callback.class), eq(config.revertPeriodSeconds));
		verifyNoMoreInteractions(actions, timer);
	}

	@Test
	public void whenReceivesTimeUpdateShouldInvokeRevertAction_And_ScheduleNewRevertTask() {
		Actions actions = mock(Actions.class);
		MyTimer timer = mock(MyTimer.class);
		Config config = new Config();
		AutoRevert autoRevert = new AutoRevert(config, actions, timer);

		autoRevert.onTime();
		verify(actions).revertActiveChangeList();
		verify(timer).schedule(any(MyTimer.Callback.class), eq(config.revertPeriodSeconds));
		verifyNoMoreInteractions(actions, timer);
	}

	@Test
	public void whenActiveChangeListIsCommitted_shouldScheduleNewRevertTask() {
		Actions actions = mock(Actions.class);
		MyTimer timer = mock(MyTimer.class);
		Config config = new Config();
		AutoRevert autoRevert = new AutoRevert(config, actions, timer);

		autoRevert.onCommit();
		verify(timer).schedule(any(MyTimer.Callback.class), eq(config.revertPeriodSeconds));
		verifyNoMoreInteractions(actions, timer);
	}

	public static class Actions {
		public void revertActiveChangeList() {
			// TODO implement
		}
	}

	public static class MyTimer {
		public void schedule(Callback callback, long timeout) {
			// TODO implement
		}

		public interface Callback {
			void call();
		}
	}

	public static class Config {
		public long revertPeriodSeconds;
	}

	public static class AutoRevert {
		private final Config config;
		private final Actions actions;
		private final MyTimer timer;

		public AutoRevert(Config config, Actions actions, MyTimer timer) {
			this.config = config;
			this.actions = actions;
			this.timer = timer;
		}

		public void start() {
			scheduleRevertTask();
		}

		public void onTime() {
			actions.revertActiveChangeList();
			scheduleRevertTask();
		}

		public void onCommit() {
			scheduleRevertTask();
		}

		private void scheduleRevertTask() {
			timer.schedule(new MyTimer.Callback() {
				@Override public void call() {
					onTime();
				}
			}, config.revertPeriodSeconds);
		}
	}
}
