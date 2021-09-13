package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lt.lb.uncheckedutils.PassableException;
import lt.lb.uncheckedutils.SafeOpt;

/**
 *
 * @author laim0nas100
 * @param <ID>
 */
public abstract class LazyLuceneControlDate<ID> extends LazyLuceneIndexControl<String, ID, Date> {

    public LazyLuceneControlDate(String startingFolder, Supplier<Map<String, LuceneCachedMap<ID, Date>>> cachingStrategy) {
        super(cachingStrategy);
        this.startingFolder = startingFolder;

    }

    protected Map<String, Date> cachedFirst = new ConcurrentHashMap<>();
    protected Map<String, Date> cachedLast = new ConcurrentHashMap<>();

    protected String startingFolder;

    protected abstract SafeOpt<Date> parseDate(String toParse);

    protected abstract SafeOpt<String> formatDate(Date toFormat);

    protected abstract Date incrementDate(Date date);

    /**
     * Should be the same as folder name
     *
     * @param folderName
     * @return
     */
    protected SafeOpt<Date> getFirstDate(String folderName) {
        if (cachedFirst.containsKey(folderName)) {
            return SafeOpt.of(cachedFirst.get(folderName));
        }
        return parseDate(folderName)
                .ifPresent(date -> cachedFirst.put(folderName, date));
    }

    /**
     * Should be the folder name plus a month span
     *
     * @param folderName
     * @return
     */
    protected SafeOpt<Date> getLastDate(String folderName) {
        if (cachedLast.containsKey(folderName)) {
            return SafeOpt.of(cachedLast.get(folderName));
        }
        return parseDate(folderName)
                .map(date -> incrementDate(date))
                .ifPresent(date -> cachedLast.put(folderName, date));
    }

    protected <ID> boolean mapDateEq(ID k, Date date, Map<ID, Date> currentIDs) {
        Date mapDate = currentIDs.getOrDefault(k, null);
        if (Objects.equals(mapDate, date)) {
            return true;
        }
        if (mapDate != null && date != null) {
            return mapDate.getTime() == date.getTime();
        }
        return false;
    }

    protected LinkedList<Date> getDates() {
        return getNestedKeys().stream()
                .map(m -> SafeOpt.of(m).flatMap(this::parseDate))
                .filter(m -> m.isPresent())
                .map(m -> m.get()).sorted()
                .collect(Collectors.toCollection(LinkedList::new));
    }

    protected SafeOpt<String> folderByDate(Date date) {
        if (date == null) {
            return SafeOpt.empty();
        }
        LinkedList<Date> dates = getDates();
        if (dates.isEmpty()) {
            return SafeOpt.empty();
        }
        Date lastAfter = null;
        for (Date d : dates) {
            if (d.after(date)) {
                if (lastAfter == null) {
                    return SafeOpt.error(new PassableException("passed date was out of range:" + date + " min:" + d));
                }
                return SafeOpt.of(lastAfter).flatMap(this::formatDate);
            }
            lastAfter = d;
        }

        // got all dates, maybe at the last one?
        Date incremented = incrementDate(lastAfter);

        if (incremented.after(date)) { // in last range?
            return SafeOpt.of(lastAfter).flatMap(this::formatDate);
        }
        return SafeOpt.error(new PassableException("passed date was out of range:" + date + " max:" + incremented));

        
    }

    @Override
    public void initOrExpandNested() throws IOException {
//this is not neccessary because we initiate all based on date
//            String[] listDistinctFolders = Q.listDistinctFolders(this.get);
//            for (String folderName : listDistinctFolders) {
//                dir.apply(folderName); // init all present directories
//            }
        LinkedList<Date> dates = getDates();

        if (dates.isEmpty()) {
            resolveDirectory(startingFolder);
            dates.add(SafeOpt.of(startingFolder).flatMap(this::parseDate).throwIfErrorAsNested().get());
        }
        Date now = new Date();
        int times = 1000000;
        while (dates.getLast().before(now)) {
            times--;
            Date last = dates.getLast();
            resolveDirectory(formatDate(last).get());
            Date incremented = incrementDate(last);
            dates.addLast(incremented);
            if (times < 0) {
                throw new IllegalStateException("Date increment exceeded a limit, probably an oveflow");
            }
        }
    }

}
