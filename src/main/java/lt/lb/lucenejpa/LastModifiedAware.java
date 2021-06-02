package lt.lb.lucenejpa;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.misc.compare.Compare;
import lt.lb.commons.misc.compare.Compare.SimpleCompare;

/**
 *
 * @author laim0nas100
 */
public interface LastModifiedAware {

    public Date getLastChange();

    public default void registerChange(String name){
        registerChange(name, new Date());
    }
    
    public void registerChange(String name, Date date);
    
    public void removeChange(String name);

    public Date getLastChange(String name);
    
    public static LastModifiedAware ofNOP(){
        return new LastModifiedAware() {
            @Override
            public Date getLastChange() {
                return null;
            }

            @Override
            public void registerChange(String name, Date date) {
            }

            @Override
            public void removeChange(String name) {
            }

            @Override
            public Date getLastChange(String name) {
                return null;
            }
        };
    }
    
    public static LastModifiedAware ofMap(){
        SimpleCompare<Date> cmp = Compare.of(Compare.CompareNull.NULL_LOWER);
        ConcurrentHashMap<String,Date> map = new ConcurrentHashMap<>();
        Value<Date> lastChange = new Value<>();
        return new LastModifiedAware() {
            @Override
            public Date getLastChange() {
                return lastChange.get();
            }

            @Override
            public void registerChange(String name, Date date) {
                Date max = cmp.max(lastChange.get(), date);
                lastChange.set(max);
                map.put(name, date);
            }

            @Override
            public Date getLastChange(String name) {
                return map.get(name);
            }

            @Override
            public void removeChange(String name) {
                map.remove(name);
            }
        };
    }
}
