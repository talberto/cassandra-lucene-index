package com.stratio.cassandra.lucene.util;

import com.stratio.cassandra.lucene.IndexException;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Created by eduardoalonso on 11/08/15.
 */
public class DateParserTest {

    @Test
    public void testParseNullDate() {
        DateParser dateParser = new DateParser("yyyy/MM/dd");
        Date date = null;
        Date dateOut = dateParser.parse(date);
        assertEquals("Date null parsed by DateParser(\"yyyy/MM/dd\") must return null", null, dateOut);
    }

    @Test
    public void testParseNullObject() {
        DateParser dateParser = new DateParser("yyyy/MM/dd");
        Object date = null;
        Date dateOut = dateParser.parse(date);
        assertEquals("Object null parsed by DateParser(\"yyyy/MM/dd\") must return null", null, dateOut);
    }

    @Test
    public void testParseNullLong() {
        DateParser dateParser = new DateParser("yyyy/MM/dd");
        Long date = null;
        Date dateOut = dateParser.parse(date);
        assertEquals("Long null parsed by DateParser(\"yyyy/MM/dd\") must return null", null, dateOut);
    }

    @Test
    public void testParseValidDate() throws ParseException {
        String pattern = "yyyy/MM/dd";
        DateParser dateParser = new DateParser(pattern);

        String dateString = "2015/11/03";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        Date date = simpleDateFormat.parse(dateString);

        Date dateOut = dateParser.parse(date);
        assertEquals("Date parsed by DateParser(\"yyyy/MM/dd\") wrong parsed",
                     dateString,
                     simpleDateFormat.format(dateOut));

    }

    @Test
    public void testParseValidObject() {

        String pattern = "yyyy/MM/dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        DateParser dateParser = new DateParser(pattern);

        String dateString = "2015/11/03";
        Date dateOut = dateParser.parse(dateString);

        assertEquals("String parsed by DateParser(\"yyyy/MM/dd\") wrong parsed",
                     dateString,
                     simpleDateFormat.format(dateOut));
    }

    @Test
    public void testParseValidLong() {
        String pattern = "yyyyMMdd";
        DateParser dateParser = new DateParser(pattern);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Long dateLong = 20151103l;
        Date dateOut = dateParser.parse(dateLong);

        assertEquals("Long parsed by DateParser(\"yyyy/MM/dd\") wrong parsed",
                     dateLong.toString(),
                     simpleDateFormat.format(dateOut));
    }

    @Test(expected = IndexException.class)
    public void testParseInvalidObject() throws ParseException {
        String pattern = "yyyyMMdd";
        DateParser dateParser = new DateParser(pattern);

        String dateString = "2015/11/03";

        dateParser.parse(dateString);
        fail("DateParser(" + pattern + ").parse(" + dateString + ") Must generate IndexException and does not do it");

    }

    @Test(expected = IndexException.class)
    public void testParseInvalidNegativeLong() {
        String pattern = "yyyyMMdd";
        DateParser dateParser = new DateParser(pattern);

        Long dateLong = -20152345l;//invalid long not parseable
        Date dateOut = dateParser.parse(dateLong);
        fail("DateParser(" +
             pattern +
             ").parse(" +
             dateLong.toString() +
             ") Must generate IndexException and does not do it");

    }

    @Test(expected = IndexException.class)
    public void testParseInvalidBiggerLong() {
        String pattern = "yyyyMMdd";
        DateParser dateParser = new DateParser(pattern);

        Long dateLong = 201523455859l;//invalid long not parseable
        Date dateOut = dateParser.parse(dateLong);
        fail("DateParser(" +
             pattern +
             ").parse(" +
             dateLong.toString() +
             ") Must generate IndexException and does not do it");

    }

    @Test
    public void testValidTimestampString() {
        String pattern = "timestamp";
        DateParser dateParser = new DateParser(pattern);
        String dateString = "2635421542648178234";
        Date date = dateParser.parse(dateString);

        Date dateToCompare = new Date(Long.parseLong(dateString));
        assertEquals("DateParser(" + pattern + ").parse(" + dateString.toString() + ") wrong parsed.",
                     dateToCompare,
                     date);
    }

    @Test
    public void testValidTimestampLong() {
        String pattern = "timestamp";
        DateParser dateParser = new DateParser(pattern);
        Long dateLong = 2635421542648178234l;
        Date date = dateParser.parse(dateLong);

        Date dateToCompare = new Date(dateLong);
        assertEquals("DateParser(" + pattern + ").parse(" + dateLong.toString() + ") wrong parsed.",
                     dateToCompare,
                     date);
    }

    @Test(expected = IndexException.class)
    public void testInvalidTimestampString() {
        String pattern = "timestamp";
        DateParser dateParser = new DateParser(pattern);

        String dateString = "2015/03/02";
        Date date = dateParser.parse(dateString);
        fail("DateParser(" +
             pattern +
             ").parse(" +
             dateString.toString() +
             ") Must generate IndexException and does not do it");
    }

}

