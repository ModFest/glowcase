package dev.hephaestus.glowcase.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadManagement {
	private static final ThreadMXBean JVM_MANAGER = ManagementFactory.getThreadMXBean();

	public static Waiting isLongWaiting(Thread thread, long millis) {
		if (thread.getState() != Thread.State.WAITING) return Waiting.SHORT;
		ThreadInfo threadInfo = JVM_MANAGER.getThreadInfo(thread.threadId());
		long waitedTime = threadInfo.getWaitedTime();
		if (waitedTime == -1) return Waiting.DISABLED;
		return waitedTime >= millis ? Waiting.LONG : Waiting.SHORT;
	}


	public enum Waiting {
		DISABLED(true),
		LONG(true),
		SHORT(false);

		public final boolean isLong;
		Waiting(boolean isLong) {
		    this.isLong = isLong;
		}
	}
}
