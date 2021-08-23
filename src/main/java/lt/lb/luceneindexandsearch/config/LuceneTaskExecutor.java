package lt.lb.luceneindexandsearch.config;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import lt.lb.commons.threads.service.ScheduledTaskExecutor;
import lt.lb.commons.threads.sync.AtomicMap;
import lt.lb.uncheckedutils.func.UncheckedRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Laimonas-Beniusis
 */
public interface LuceneTaskExecutor extends ScheduledTaskExecutor<String> {

    public static String getName(String kind, String folder) {
        return kind + "-" + folder;
    }

    public static class LuceneTaskExecutorImpl implements LuceneTaskExecutor {

        public static Logger logger = LogManager.getLogger(LuceneTaskExecutorImpl.class);
        public ExecutorService executor;
        public ScheduledExecutorService scheduler;

        public AtomicMap<String, Occupy> atomicMap = new AtomicMap<>();

        public LuceneTaskExecutorImpl(ExecutorService executor, ScheduledExecutorService scheduler) {
            this.executor = Objects.requireNonNull(executor);
            this.scheduler = Objects.requireNonNull(scheduler);
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
            if (error.isPresent()) {
                logger.error(name + " End with error", error.get());
            } else {
                logger.debug("End " + name);
            }
        }

        @Override
        public void failedToSubmit(FailedSubmitCase failedCase, String name, UncheckedRunnable run) {
            logger.debug(name + " Allready running, failed to submit");
        }

    }
}
