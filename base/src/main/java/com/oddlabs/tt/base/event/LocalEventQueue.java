package com.oddlabs.tt.base.event;

import com.oddlabs.event.Deterministic;
import com.oddlabs.event.LoadDeterministic;
import com.oddlabs.event.NotDeterministic;
import com.oddlabs.event.SaveDeterministic;
import com.oddlabs.tt.base.animation.AnimationManager;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class LocalEventQueue implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(LocalEventQueue.class.getName());

    private final StateChecksum checksum = new StateChecksum();
    private final AnimationManager manager = new AnimationManager();
    private final AnimationManager high_precision_manager = new AnimationManager();
    private Deterministic deterministic = new NotDeterministic();
    private float time = 0;

    public float getTime() {
        return time;
    }

    public long getMillis() {
        return high_precision_manager.getTick() * AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK;
    }

    public void setEventsLogged(Path log_file) {
        assert deterministic instanceof NotDeterministic;
        this.deterministic = new SaveDeterministic(log_file);
    }

    @Override
    public void close() {
        logger.info("LocalEventQueue disposing...");
        logger.info("Ending deterministic log...");
        deterministic.endLog();
        logger.info("LocalEventQueue disposed.");
    }

    //public static Deterministic stack_deterministic;
    public void loadEvents(Path log_file, boolean zipped) {
        this.deterministic = new LoadDeterministic(log_file, zipped);
        /*		File stack_file = new File("stack.log");
        		if (stack_file.exists())
        			stack_deterministic = new LoadDeterministic(stack_file, false);
        		else
        			stack_deterministic = new SaveDeterministic(stack_file);
        		this.deterministic = new StackTraceDeterministic(deterministic, stack_deterministic);*/
    }

    public AnimationManager getHighPrecisionManager() {
        return high_precision_manager;
    }

    public AnimationManager getManager() {
        return manager;
    }

    public int computeChecksum() {
        checksum.update(getManager().getTick());
        checksum.update(getHighPrecisionManager().getTick());
        manager.updateChecksum(checksum);
        high_precision_manager.updateChecksum(checksum);
        return checksum.getValue();
    }

    public Deterministic getDeterministic() {
        return deterministic;
    }

    public void tickHighPrecision(float t) {
        time += t;
        getHighPrecisionManager().runAnimations(t);
    }

    public void tickLowPrecision(float t) {
        getManager().runAnimations(t);
    }

    public void debugPrintAnimations() {
        getManager().debugPrintAnimations();
    }
}
