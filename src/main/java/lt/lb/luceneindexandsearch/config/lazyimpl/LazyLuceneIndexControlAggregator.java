package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lt.lb.luceneindexandsearch.config.LuceneIndexControl;
import lt.lb.luceneindexandsearch.config.LuceneIndexControlAggregator;
import lt.lb.uncheckedutils.Checked;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author laim0nas100
 */
public abstract class LazyLuceneIndexControlAggregator implements LuceneIndexControlAggregator {

    public static Logger logger = LogManager.getLogger(LazyLuceneIndexControlAggregator.class);

    public ScheduledExecutorService scheduler;
    public ExecutorService executor;
    public ScheduledFuture scheduled;

    public LazyLuceneIndexControlAggregator(ScheduledExecutorService scheduler, ExecutorService executor, Map<String, LuceneIndexControl> map) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.executor = Objects.requireNonNull(executor);
        this.map = Objects.requireNonNull(map);
    }

    protected Map<String, LuceneIndexControl> map;
    protected ArrayList<ScheduledFuture> scheduledFutures = new ArrayList<>();

    @Override
    public void setupMaintenance() {
        for (Map.Entry<String, LuceneIndexControl> entry : getLuceneIndexControlMap().entrySet()) {
            LuceneIndexControl value = entry.getValue();
            String key = entry.getKey();
            scheduledFutures.add(scheduleRunnable(safeRunnableMaintenance(key, value), key, value));
        }
    }

    @Override
    public void cancelMaintenance() {
        for (ScheduledFuture future : scheduledFutures) {
            future.cancel(true);
        }
        scheduledFutures.clear();
    }

    protected abstract ScheduledFuture scheduleRunnable(Runnable run, String key, LuceneIndexControl control);

    protected Runnable safeRunnableMaintenance(String key, LuceneIndexControl control) {
        AtomicBoolean running = new AtomicBoolean(false);

        return () -> {
            logger.debug(key + " Start maintenance");
            if (running.compareAndSet(false, true)) {
                Optional<Throwable> checkedRun = Checked.checkedRun(control::periodicMaintenance);
                running.set(false);
                checkedRun.ifPresent(error -> {
                    logger.error("Error in periodic maintenance", error);
                });
                logger.debug(key + " End maintenance");
            } else {
                logger.debug(key + " Allready running, wait for next cycle");
            }

        };
    }

    @Override
    public Map<String, LuceneIndexControl> getLuceneIndexControlMap() {
        return this.map;
    }

}
