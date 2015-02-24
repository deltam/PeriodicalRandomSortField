package com.bengo4.solr.schema;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Unit test for simple PeriodicalRandomSortField.
 */
public class PeriodicalRandomSortFieldTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PeriodicalRandomSortFieldTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PeriodicalRandomSortFieldTest.class );
    }


    public void testParseFieldAll()
    {
        // parseParameters
        String fieldName = "prand_12344_2h,12h_123456";
        Map<String,String> params = PeriodicalRandomSortField.parseField(fieldName);
        assertEquals("prand_12344", params.get(PeriodicalRandomSortField.PARAM_KEY_SEED));
        assertEquals("2h,12h", params.get(PeriodicalRandomSortField.PARAM_KEY_PERIODS));
        assertEquals("123456", params.get(PeriodicalRandomSortField.PARAM_KEY_EPOC));
    }

    public void testParseFieldPeriod()
    {
        String fieldName = "prand_12344_2h,12h";
        Map<String,String> params = PeriodicalRandomSortField.parseField(fieldName);
        assertEquals("prand_12344", params.get(PeriodicalRandomSortField.PARAM_KEY_SEED));
        assertEquals("2h,12h", params.get(PeriodicalRandomSortField.PARAM_KEY_PERIODS));
        assertEquals(null, params.get(PeriodicalRandomSortField.PARAM_KEY_EPOC));

    }

    public void testParseFieldSeed()
    {
        String fieldName = "prand_12344";
        Map<String,String> params = PeriodicalRandomSortField.parseField(fieldName);
        assertEquals("prand_12344", params.get(PeriodicalRandomSortField.PARAM_KEY_SEED));
        assertEquals(null, params.get(PeriodicalRandomSortField.PARAM_KEY_PERIODS));
        assertEquals(null, params.get(PeriodicalRandomSortField.PARAM_KEY_EPOC));
    }

    public void testParseFieldNoUnderscore()
    {
        String fieldName = "prand12344";
        Map<String,String> params = PeriodicalRandomSortField.parseField(fieldName);
        assertEquals("prand12344", params.get(PeriodicalRandomSortField.PARAM_KEY_SEED));
        assertEquals(null, params.get(PeriodicalRandomSortField.PARAM_KEY_PERIODS));
        assertEquals(null, params.get(PeriodicalRandomSortField.PARAM_KEY_EPOC));
    }

    public void testParsePeriodsString()
    {
        String periodStr = "5m,1h,12h";
        List<Integer> periods = PeriodicalRandomSortField.parsePeriodsString(periodStr);
        assertEquals(3, periods.size());
        assertEquals(5*60, (int)periods.get(0));
        assertEquals(1*60*60, (int)periods.get(1));
        assertEquals(12*60*60, (int)periods.get(2));
    }


    public void testParsePeriodsStringIllegalSyntax()
    {
        String periodStr = "5s,10m,15m"; // no 5s syntax
        List<Integer> periods = PeriodicalRandomSortField.parsePeriodsString(periodStr);
        assertEquals(2, periods.size());
        assertEquals(10*60, (int)periods.get(0));
        assertEquals(15*60, (int)periods.get(1));
    }


    public void testIsPeriodNow()
    {
        int epoc = 1424098800; // 2015/02/17 00:00:00
        int period = 60*60;    // 1時間後に切り替わる
        int firstPeriod  = 1424098860; // 2015/02/17 00:01:00
        int firstPeriod2 = 1424102399; // 2015/02/17 00:59:59
        int secondPeriod = firstPeriod2 + 1; // 2015/02/17 01:00:00

        assertTrue(PeriodicalRandomSortField.isPeriodNow(epoc, period, firstPeriod));
        assertTrue(PeriodicalRandomSortField.isPeriodNow(epoc, period, firstPeriod2));
        assertFalse(PeriodicalRandomSortField.isPeriodNow(epoc, period, secondPeriod));
    }

    public void testGetDefaultEpoc() throws Exception
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date yesterday = sdf.parse("2015-02-16 23:59:59");
        Date today1    = sdf.parse("2015-02-17 00:00:00");
        Date today2    = sdf.parse("2015-02-17 08:09:20");
        Date today3    = sdf.parse("2015-02-17 23:59:59");
        Date tommorow  = sdf.parse("2015-02-18 00:00:00");
        int epoc = 1424098800; // 2015-02-17 00:00:00

        assertFalse(epoc == PeriodicalRandomSortField.getDefaultEpoc(yesterday));
        assertEquals(epoc, PeriodicalRandomSortField.getDefaultEpoc(today1));
        assertEquals(epoc, PeriodicalRandomSortField.getDefaultEpoc(today2));
        assertEquals(epoc, PeriodicalRandomSortField.getDefaultEpoc(today3));
        assertFalse(epoc == PeriodicalRandomSortField.getDefaultEpoc(tommorow));
    }

    public void testGetPeriodSeed()
    {
        int epoc = 1424098800;                          // 2015/02/17 00:00:00

        List<Integer> periods = new ArrayList<Integer>();
        int period1hour = 1*60*60; // 1時間後
        int period6hour = 6*60*60; // 6時間後
        periods.add(period1hour);
        periods.add(period6hour);

        int firstPeriod       = 1424098860;           // 2015/02/17 00:01:00
        int firstPeriodEnd    = 1424102399;           // 2015/02/17 00:59:59
        int secondPeriodStart = firstPeriodEnd + 1;   // 2015/02/17 01:00:00
        int secondPeriodEnd   = 1424120399;           // 2015/02/17 05:59:59
        int thirdPeriodStart  = secondPeriodEnd + 1;  // 2015/02/17 05:59:59

        assertEquals(period1hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, firstPeriod));
        assertEquals(period1hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, firstPeriodEnd));
        assertEquals(period6hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, secondPeriodStart));
        assertEquals(period6hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, secondPeriodEnd));
        assertEquals(0,           PeriodicalRandomSortField.getPeriodSeed(epoc, periods, thirdPeriodStart));
    }

    public void testGetPeriodSeedWhenModified()
    {
        int epoc = 1424098800;                          // 2015/02/17 00:00:00

        List<Integer> periods = new ArrayList<Integer>();
        int period1hour = 1*60*60; // 1時間後
        periods.add(period1hour);

        int firstPeriodEnd      = 1424102399;           // 2015/02/17 00:59:59
        int secondPeriodStart   = firstPeriodEnd + 1;   // 2015/02/17 01:00:00

        assertEquals(period1hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, firstPeriodEnd));
        assertEquals(0,           PeriodicalRandomSortField.getPeriodSeed(epoc, periods, secondPeriodStart));



        // あとでPeriodを追加した場合でも既存のPeriodの返り値には影響しない
        int period30min = 30*60;   // 30分後
        periods.add(0, period30min);
        int newPeriodEnd = 1424100600 - 1; // 2015/02/17 00:29:59
        int firstPeriodStart = newPeriodEnd + 1;
        assertEquals(period30min, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, newPeriodEnd));
        assertEquals(period1hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, firstPeriodStart));
        assertEquals(period1hour, PeriodicalRandomSortField.getPeriodSeed(epoc, periods, firstPeriodEnd));
        assertEquals(0,           PeriodicalRandomSortField.getPeriodSeed(epoc, periods, secondPeriodStart));
    }
}
