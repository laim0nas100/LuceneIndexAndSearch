package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import lt.lb.commons.threads.sync.AtomicMap;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.luceneindexandsearch.config.LuceneIndexControl;
import lt.lb.luceneindexandsearch.config.LuceneIndexControlAggregator;
import lt.lb.uncheckedutils.func.UncheckedRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author laim0nas100
 */
public class LazyLuceneIndexControlAggregator implements LuceneIndexControlAggregator {

    public static Logger logger = LogManager.getLogger(LazyLuceneIndexControlAggregator.class);

    public ScheduledExecutorService scheduler;
    public ExecutorService executor;
    public ScheduledFuture scheduled;
    public AtomicMap<String, Occupy> atomicMap;
    public WaitTime period;

    public LazyLuceneIndexControlAggregator(WaitTime waitTime, ScheduledExecutorService scheduler, ExecutorService executor, Map<String, LuceneIndexControl> map) {
        this.period = Objects.requireNonNull(waitTime);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.executor = Objects.requireNonNull(executor);
        this.map = Objects.requireNonNull(map);
        this.atomicMap = new AtomicMap<>();
    }

    protected Map<String, LuceneIndexControl> map;
    protected ArrayList<ScheduledFuture> scheduledFutures = new ArrayList<>();

    @Override
    public void setupMaintenance() {
        for (Map.Entry<String, LuceneIndexControl> entry : getLuceneIndexControlMap().entrySet()) {
            LuceneIndexControl value = entry.getValue();
            String key = entry.getKey();
            ScheduledFuture schedule = this.schedulePeriodically(true, 0, period.time, period.unit, key, value::periodicMaintenance);
            scheduledFutures.add(schedule);
        }
    }

    @Override
    public void cancelMaintenance() {
        for (ScheduledFuture future : scheduledFutures) {
            future.cancel(true);
        }
        scheduledFutures.clear();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public AtomicMap<String, Occupy> getAtomicMap() {
        return atomicMap;
    }

    @Override
    public UncheckedRunnable beforeRun(String name, UncheckedRunnable run) {
        logger.debug(name + " Start");
        return run;
    }

    @Override
    public void afterRun(String name, Optional<Throwable> error) {
        logger.debug(name + " End");
    }

    @Override
    public void failedToSubmit(FailedSubmitCase failedCase, String name, UncheckedRunnable run) {
        logger.debug(name + " Allready running, failed to submit");
    }

    @Override
    public Map<String, LuceneIndexControl> getLuceneIndexControlMap() {
        return this.map;
    }

}
