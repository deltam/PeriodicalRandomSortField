package com.bengo4.solr.schema;


import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.response.TextResponseWriter;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;

import org.apache.solr.common.SolrException;

/**
 * PeriodicalRandomSortField
 *
 * Provides RandomSortField periodically updated.
 * Unlike RandomSortField, this is does not depend on docBase and version.
 *
 * Usage: prandom_{SEED}_{PERIODS}_{EPOC_TIME} desc
 *   {SEED} : random seed
 *   {PERIODS} : update timing. minuts or hours. (ex 5m,4h)
 *   {EPOC_TIME} : period epoc unixtime (Defaults today 00:00:00)
 *
 * Example:
 *   No EPOC_TIME, No periods.
 *      ...&prandom_SEED
 *   EPOC_TIME is todays 00:00:00, updating 12 o'clock
 *      ...&prandom_SEED_12h
 *   EPOC_TIME is 2015-02-21 4:00:00, updating after 4 hour and 8 hour, 12 hour.
 *      ...&prandom_SEED_4h,8h,12h_1424458800
 */
public class PeriodicalRandomSortField extends FieldType
{
    protected static final String SYNTAX_DELIMIT        = "_";
    protected static final String SYNTAX_PERIOD_MINUTES = "m";
    protected static final String SYNTAX_PERIOD_HOURS   = "h";
    protected static final String SYNTAX_PERIOD_DELIMIT = ",";

    protected static final String PARAM_KEY_SEED    = "seed";
    protected static final String PARAM_KEY_PERIODS = "periods";
    protected static final String PARAM_KEY_EPOC    = "epoc";

    /**
     * This method is th same as RandomSortField
     */
    private static int hash(int key) {
        key = ~key + (key << 15); // key = (key << 15) - key - 1;
        key = key ^ (key >>> 12);
        key = key + (key << 2);
        key = key ^ (key >>> 4);
        key = key * 2057; // key = (key + (key << 3)) + (key << 11);
        key = key ^ (key >>> 16);
        return key >>> 1;
    }

    /**
     * parse of field name contains parameters
     * @return Map<String,String>
     */
    protected static Map<String,String> parseField(String fieldName) {
        String[] splitted = fieldName.split(SYNTAX_DELIMIT);
        Map<String,String> params = new HashMap<String,String>();

        if (splitted.length > 1) {
            params.put(PARAM_KEY_SEED, splitted[0] + SYNTAX_DELIMIT + splitted[1]);
        } else {
            params.put(PARAM_KEY_SEED, fieldName);
            return params;
        }

        // period randomized
        if (splitted.length > 2) {
            params.put(PARAM_KEY_PERIODS, splitted[2]);
        }
        // epoc
        if (splitted.length > 3) {
            params.put(PARAM_KEY_EPOC, splitted[3]);
        }

        return params;
    }

    /**
     * @return List<Integer> of seconds after epoc
     */
    protected static List<Integer> parsePeriodsString(String periodsStr)
    {
        String[] periods = periodsStr.split(SYNTAX_PERIOD_DELIMIT);
        List<Integer> periodTimes = new ArrayList<Integer>();
        for (String period: periods) {
            int time = 0;
            if (period.matches("^[0-9]+" + SYNTAX_PERIOD_HOURS + "$")) {
                time = Integer.parseInt(period.replaceAll(SYNTAX_PERIOD_HOURS,""));
                time *= 3600;
            } else if (period.matches("^[0-9]+" + SYNTAX_PERIOD_MINUTES + "$")) {
                time = Integer.parseInt(period.replaceAll(SYNTAX_PERIOD_MINUTES,""));
                time *= 60;
            }
            if (time > 0) {
                periodTimes.add(time);
            }
        }
        return periodTimes;
    }

    /**
     * @return boolean determinate period time
     */
    protected static boolean isPeriodNow(int epoc, int period, int now)
    {
        return (epoc <= now) && (now < epoc + period);
    }

    /**
     * @return int today 00:00:00 (unixtime)
     */
    protected static int getDefaultEpoc(Date date)
    {
        if (date == null) {
            date = new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);
        cal.set(y, m, d, 0, 0, 0);
        return (int)(cal.getTimeInMillis() / 1000L);
    }

    /**
     * Use calc random seed
     * @return int now period time
     */
    protected static int getPeriodSeed(int epoc, List<Integer> periods, int nowTime)
    {
        for (int p: periods) {
            if (isPeriodNow(epoc, p, nowTime)) {
                return p;
            }
        }
        return 0;
    }


    /**
     * field
     */
    private static int getSeed(String fieldName, AtomicReaderContext context) {
        int periodSeed = 0;

        Map<String,String> params = parseField(fieldName);
        String fieldSeed = params.get(PARAM_KEY_SEED);

        // get period param
        if (params.containsKey(PARAM_KEY_PERIODS)) {
            List<Integer> parsedPeriods = parsePeriodsString(params.get(PARAM_KEY_PERIODS));
            int nowTime = (int)(System.currentTimeMillis() / 1000L);
            int epoc;
            if (params.containsKey(PARAM_KEY_EPOC)) {
                epoc = Integer.parseInt(params.get(PARAM_KEY_EPOC));
            } else {
                epoc = getDefaultEpoc(null); // today 00:00:00
            }

            periodSeed = getPeriodSeed(epoc, parsedPeriods, nowTime);
        }

        return fieldSeed.hashCode() + periodSeed;
    }



    ///////////////////////////////////////////////////////////////////////////////////////////
    /* under code is the same as RandomSortField */

    @Override
    public SortField getSortField(SchemaField field, boolean reverse) {
        return new SortField(field.getName(), randomComparatorSource, reverse);
    }

    @Override
    public ValueSource getValueSource(SchemaField field, QParser qparser) {
        return new RandomValueSource(field.getName());
    }

    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException { }



    private static FieldComparatorSource randomComparatorSource = new FieldComparatorSource() {
            @Override
            public FieldComparator<Integer> newComparator(final String fieldname, final int numHits, int sortPos, boolean reversed) {
                return new FieldComparator<Integer>() {
                    int seed;
                    private final int[] values = new int[numHits];
                    int bottomVal;

                    @Override
                    public int compare(int slot1, int slot2) {
                        return values[slot1] - values[slot2];  // values will be positive... no overflow possible.
                    }

                    @Override
                    public void setBottom(int slot) {
                        bottomVal = values[slot];
                    }

                    @Override
                    public int compareBottom(int doc) {
                        return bottomVal - hash(doc+seed);
                    }

                    @Override
                    public void copy(int slot, int doc) {
                        values[slot] = hash(doc+seed);
                    }

                    @Override
                    public FieldComparator<Integer> setNextReader(AtomicReaderContext context) { // unchecked警告が出るため返り値に型指定を追加
                        seed = getSeed(fieldname, context);
                        return this;
                    }

                    @Override
                    public Integer value(int slot) {
                        return values[slot];
                    }

                    @Override
                    public int compareDocToValue(int doc, Integer valueObj) {
                        // values will be positive... no overflow possible.
                        return hash(doc+seed) - valueObj.intValue();
                    }
                };
            }
        };


    public class RandomValueSource extends ValueSource {
        private final String field;

        public RandomValueSource(String field) {
            this.field = field;
        }

        @Override
        public String description() {
            return field;
        }

        @Override
        public FunctionValues getValues(Map context, final AtomicReaderContext readerContext) throws IOException {
            return new IntDocValues(this) {
                private final int seed = getSeed(field, readerContext);
                @Override
                public int intVal(int doc) {
                    return hash(doc+seed);
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RandomValueSource)) return false;
            RandomValueSource other = (RandomValueSource)o;
            return this.field.equals(other.field);
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        };
    }
}
