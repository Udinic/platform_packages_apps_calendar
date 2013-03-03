/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendarcommon2;

import com.android.calendarcommon2.ICalendar;
import com.android.calendarcommon2.RecurrenceSet;

import android.content.ContentValues;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.provider.CalendarContract;
import junit.framework.TestCase;

/**
 * Test some pim.RecurrenceSet functionality.
 */
public class RecurrenceSetTest extends TestCase {

    // Test a recurrence
    @SmallTest
    public void testRecurrenceSet0() throws Exception {
        String recurrence = "DTSTART;TZID=America/New_York:20080221T070000\n"
                + "DTEND;TZID=America/New_York:20080221T190000\n"
                + "RRULE:FREQ=DAILY;UNTIL=20080222T000000Z\n"
                + "EXDATE:20080222T120000Z";
        verifyPopulateContentValues(recurrence, "FREQ=DAILY;UNTIL=20080222T000000Z", null,
                null, "20080222T120000Z", 1203595200000L, "America/New_York", "P43200S", 0, false);
    }

    // Test 1 day all-day event
    @SmallTest
    public void testRecurrenceSet1() throws Exception {
        String recurrence = "DTSTART;VALUE=DATE:20090821\nDTEND;VALUE=DATE:20090822\n"
                + "RRULE:FREQ=YEARLY;WKST=SU";
        verifyPopulateContentValues(recurrence, "FREQ=YEARLY;WKST=SU", null,
                null, null, 1250812800000L, "UTC", "P1D", 1, false);
    }

    // Test 2 day all-day event
    @SmallTest
    public void testRecurrenceSet2() throws Exception {
        String recurrence = "DTSTART;VALUE=DATE:20090821\nDTEND;VALUE=DATE:20090823\n"
                + "RRULE:FREQ=YEARLY;WKST=SU";
        verifyPopulateContentValues(recurrence, "FREQ=YEARLY;WKST=SU", null,
                null, null, 1250812800000L, "UTC",  "P2D", 1, false);
    }

    // Test multi-rule RRULE.
    @SmallTest
    public void testRecurrenceSet3() throws Exception {
        String recurrence = "DTSTART;VALUE=DATE:20090821\n"
                + "RRULE:FREQ=YEARLY;WKST=SU\n"
                + "RRULE:FREQ=MONTHLY;COUNT=3\n"
                + "DURATION:P2H";
        verifyPopulateContentValues(recurrence, "FREQ=YEARLY;WKST=SU\nFREQ=MONTHLY;COUNT=3", null,
                null, null, 1250812800000L, "UTC", "P2H", 1 /*allDay*/, false);
        // allDay=1 just means the start time is 00:00:00 UTC.
    }

    // Test RDATE with VALUE=DATE.
    @SmallTest
    public void testRecurrenceSet4() throws Exception {
        String recurrence = "DTSTART;TZID=America/Los_Angeles:20090821T010203\n"
                + "RDATE;TZID=America/Los_Angeles;VALUE=DATE:20110601,20110602,20110603\n"
                + "DURATION:P2H";
        verifyPopulateContentValues(recurrence, null,
                //"TZID=America/Los_Angeles;VALUE=DATE:20110601,20110602,20110603",
                "America/Los_Angeles;20110601,20110602,20110603", // incorrect
                null, null, 1250841723000L, "America/Los_Angeles", "P2H", 0 /*allDay*/, false);
        // allDay=1 just means the start time is 00:00:00 UTC.
    }

    // Check generation of duration from events in different time zones.
    @SmallTest
    public void testRecurrenceSet5() throws Exception {
        String recurrence = "DTSTART;TZID=America/Los_Angeles:20090821T070000\n"
                + "DTEND;TZID=America/New_York:20090821T110000\n"
                + "RRULE:FREQ=YEARLY\n";
        verifyPopulateContentValues(recurrence, "FREQ=YEARLY", null,
                null, null, 1250863200000L, "America/Los_Angeles", "P3600S" /*P1H*/, 0 /*allDay*/,
                false);
        // TODO: would like to use P1H for duration

        String recurrence2 = "DTSTART;TZID=America/New_York:20090821T100000\n"
            + "DTEND;TZID=America/Los_Angeles:20090821T080000\n"
            + "RRULE:FREQ=YEARLY\n";
        verifyPopulateContentValues(recurrence, "FREQ=YEARLY", null,
                null, null, 1250863200000L, "America/Los_Angeles", "P3600S" /*P1H*/, 0 /*allDay*/,
                false);
        // TODO: should we rigorously define which tzid becomes the "event timezone"?
    }

    // Test a failure to parse the recurrence data
    @SmallTest
    public void testRecurrenceSetBadDstart() throws Exception {
        String recurrence = "DTSTART;TZID=GMT+05:30:20080221T070000\n"
                + "DTEND;TZID=GMT+05:30:20080221T190000\n"
                + "RRULE:FREQ=DAILY;UNTIL=20080222T000000Z\n"
                + "EXDATE:20080222T120000Z";
        verifyPopulateContentValues(recurrence, "FREQ=DAILY;UNTIL=20080222T000000Z", null,
                null, "20080222T120000Z", 1203595200000L, "America/New_York", "P43200S", 0, true);
    }

    @SmallTest
    public void testRecurrenceSetBadRrule() throws Exception {
        String recurrence = "DTSTART;TZID=America/New_York:20080221T070000\n"
                + "DTEND;TZID=GMT+05:30:20080221T190000\n"
                + "RRULE:FREQ=NEVER;UNTIL=20080222T000000Z\n"
                + "EXDATE:20080222T120000Z";
        verifyPopulateContentValues(recurrence, "FREQ=DAILY;UNTIL=20080222T000000Z", null,
                null, "20080222T120000Z", 1203595200000L, "America/New_York", "P43200S", 0, true);
    }

    // run populateContentValues and verify the results
    private void verifyPopulateContentValues(String recurrence, String rrule, String rdate,
            String exrule, String exdate, long dtstart, String tzid, String duration, int allDay,
            boolean badFormat)
            throws ICalendar.FormatException {
        ICalendar.Component recurrenceComponent =
                new ICalendar.Component("DUMMY", null /* parent */);
        ICalendar.parseComponent(recurrenceComponent, recurrence);
        ContentValues values = new ContentValues();
        boolean result = RecurrenceSet.populateContentValues(recurrenceComponent, values);
        Log.d("KS", "values " + values);

        if (badFormat) {
            assertEquals(result, !badFormat);
            return;
        }
        assertEquals(rrule, values.get(CalendarContract.Events.RRULE));
        assertEquals(rdate, values.get(CalendarContract.Events.RDATE));
        assertEquals(exrule, values.get(CalendarContract.Events.EXRULE));
        assertEquals(exdate, values.get(CalendarContract.Events.EXDATE));
        assertEquals(dtstart, (long) values.getAsLong(CalendarContract.Events.DTSTART));
        assertEquals(tzid, values.get(CalendarContract.Events.EVENT_TIMEZONE));
        assertEquals(duration, values.get(CalendarContract.Events.DURATION));
        assertEquals(allDay,
                (int) values.getAsInteger(CalendarContract.Events.ALL_DAY));
    }

}
