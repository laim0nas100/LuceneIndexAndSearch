package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import lt.lb.commons.threads.service.BasicTaskExecutorQueue.BasicRunInfo;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.luceneindexandsearch.config.LuceneIndexControl;
import lt.lb.luceneindexandsearch.config.LuceneIndexControlAggregator;
import lt.lb.luceneindexandsearch.config.LuceneTaskExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author laim0nas100
 */
public class LazyLuceneIndexControlAggregator implements LuceneIndexControlAggregator {

    public static Logger logger = LogManager.getLogger(LazyLuceneIndexControlAggregator.class);

    public LuceneTaskExecutor executor;
    public ScheduledFuture scheduled;
    public WaitTime period;

    public LazyLuceneIndexControlAggregator(WaitTime waitTime, LuceneTaskExecutor executor, Map<String, LuceneIndexControl> map) {
        this.period = Objects.requireNonNull(waitTime);
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
            // control unit should be the one responsible for making things unique
            BasicRunInfo info = BasicRunInfo.basic(false, "Periodic invocation " + key);
            ScheduledFuture schedule = executor.schedulePeriodically(info, 0, period.time, period.unit, value::periodicMaintenance);
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
    public Map<String, LuceneIndexControl> getLuceneIndexControlMap() {
        return this.map;
    }

    @Override
    public LuceneTaskExecutor getExecutor() {
        return this.executor;
    }

}
