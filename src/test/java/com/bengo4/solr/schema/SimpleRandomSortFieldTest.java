package com.bengo4.solr.schema;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple PeriodicalRandomSortField.
 */
public class SimpleRandomSortFieldTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SimpleRandomSortFieldTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SimpleRandomSortFieldTest.class );
    }

    public void testSome()
    {
        assertEquals(1, 1);
    }
}
