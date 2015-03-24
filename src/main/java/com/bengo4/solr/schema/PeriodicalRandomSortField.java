package com.bengo4.solr.schema;


import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

import org.apache.lucene.index.AtomicReaderContext;

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
public class PeriodicalRandomSortField extends SimpleRandomSortField
{
    protected static final String SYNTAX_DELIMIT        = "_";
    protected static final String SYNTAX_PERIOD_MINUTES = "m";
    protected static final String SYNTAX_PERIOD_HOURS   = "h";
    protected static final String SYNTAX_PERIOD_DELIMIT = ",";

    protected static final String PARAM_KEY_SEED    = "seed";
    protected static final String PARAM_KEY_PERIODS = "periods";
    protected static final String PARAM_KEY_EPOC    = "epoc";

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
    protected static int getSeed(String fieldName, AtomicReaderContext context) {
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
}
