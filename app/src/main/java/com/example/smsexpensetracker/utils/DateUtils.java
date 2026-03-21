package com.example.smsexpensetracker.utils;

import java.util.Calendar;

public class DateUtils {

    public static long[] today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59);
        return new long[]{start, c.getTimeInMillis()};
    }

    public static long[] thisWeek() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        return new long[]{c.getTimeInMillis(), System.currentTimeMillis()};
    }

    public static long[] thisMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        return new long[]{c.getTimeInMillis(), System.currentTimeMillis()};
    }

    public static long[] thisYear() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        return new long[]{c.getTimeInMillis(), System.currentTimeMillis()};
    }
}
