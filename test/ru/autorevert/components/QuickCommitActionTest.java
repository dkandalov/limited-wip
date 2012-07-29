package ru.autorevert.components;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.autorevert.components.QuickCommitAction.nextCommitMessage;

/**
 * User: dima
 * Date: 29/07/2012
 */
public class QuickCommitActionTest {
	@Test public void shouldComeUpWithNextCommitMessage() {
		assertThat(nextCommitMessage(null), equalTo(" 0"));
		assertThat(nextCommitMessage(""), equalTo(" 0"));

		assertThat(nextCommitMessage(" 0"), equalTo(" 1"));
		assertThat(nextCommitMessage(" 1"), equalTo(" 2"));

		assertThat(nextCommitMessage("some message"), equalTo("some message 0"));
		assertThat(nextCommitMessage("some message 0"), equalTo("some message 1"));
		assertThat(nextCommitMessage("some message 9"), equalTo("some message 10"));
		assertThat(nextCommitMessage("some message 10"), equalTo("some message 11"));
		assertThat(nextCommitMessage("some message 19"), equalTo("some message 20"));
	}
}
