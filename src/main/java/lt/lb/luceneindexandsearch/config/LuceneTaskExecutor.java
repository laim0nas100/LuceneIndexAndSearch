package lt.lb.luceneindexandsearch.config;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import lt.lb.commons.threads.service.BasicTaskExecutorQueue;
import lt.lb.commons.threads.service.TaskExecutorQueue;
import lt.lb.uncheckedutils.func.UncheckedRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author laim0nas100
 */
public interface LuceneTaskExecutor extends TaskExecutorQueue<String, BasicTaskExecutorQueue.BasicRunInfo> {

    public static String getName(String kind, String folder) {
        return kind + "-" + folder;
    }

    public static class LuceneTaskExecutorImpl extends BasicTaskExecutorQueue implements LuceneTaskExecutor {

        public static Logger logger = LogManager.getLogger(LuceneTaskExecutorImpl.class);
        public ExecutorService executor;
        public ScheduledExecutorService scheduler;

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

        
        private static String format(BasicRunInfo info, String msg){
            return new StringBuilder()
                    .append("[").append(info.getKey()).append("] ")
                    .append(msg).append(":").append(info.getName())
                    .toString();
        }
        @Override
        public Optional<Throwable> onFailedEnqueue(BasicRunInfo info) {
            logger.debug(format(info, "Allready running, failed to submit"));
            return Optional.empty();
        }

        @Override
        public UncheckedRunnable beforeRun(BasicRunInfo info, UncheckedRunnable run) {
            logger.debug(format(info, "Start"));
            return run;
        }

        @Override
        public void afterRun(BasicRunInfo info, Optional<Throwable> error) {
            if (error.isPresent()) {
                logger.error(format(info, "Errored"), error.get());
            } else {
                logger.debug(format(info, "End"));
            }
        }
    }
}
