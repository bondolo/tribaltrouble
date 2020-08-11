package com.oddlabs.tt.model.behaviour;

import java.util.Arrays;

public strictfp abstract class Controller {
	private final static int MAX_TRIES = 1;
	private final int[] give_up_counters;

	protected Controller(int num_states) {
		give_up_counters = new int[num_states];
	}

	public final void resetGiveUpCounters() {
        Arrays.fill(give_up_counters, 0);
	}

	public final void resetGiveUpCounter(int state_index) {
		give_up_counters[state_index] = 0;
	}

	protected final boolean shouldGiveUp(int state_index) {
		if (give_up_counters[state_index] != MAX_TRIES)  {
			give_up_counters[state_index]++;
			return false;
		} else {
			return true;
		}
	}

	public String getKey() {
		return Integer.toString(getClass().hashCode());
	}

	public abstract void decide();
}
