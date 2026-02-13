package com.example.smsexpensetracker;

import java.util.Calendar;
import java.util.List;

/**
 * A class dedicated to performing calculations on SMS transaction data.
 */
public class ExpenseCalculator {

    /**
     * A simple data class to hold the results of the calculation.
     */
    public static class Totals {
        public final double spentToday;
        public final double spentWeek;
        public final double spentMonth;
        public final double spentYear;

        public Totals(double spentToday, double spentWeek, double spentMonth, double spentYear) {
            this.spentToday = spentToday;
            this.spentWeek = spentWeek;
            this.spentMonth = spentMonth;
            this.spentYear = spentYear;
        }
    }

    /**
     * Calculates the total debits for today, this week, this month, and this year.
     *
     * @param smsList The list of processed SMS components.
     * @return A Totals object containing all the calculated amounts.
     */
    public Totals calculate(List<SmsComponent> smsList) {
        double spentToday = 0, spentWeek = 0, spentMonth = 0, spentYear = 0;
        Calendar now = Calendar.getInstance();

        for (SmsComponent sms : smsList) {
            // Only consider debit transactions for expense calculations
            if (sms.getAmountType().equalsIgnoreCase("DEBIT")) {
                Calendar smsCal = Calendar.getInstance();
                smsCal.setTime(sms.getSMSDate());

                // Check if the transaction happened in the current year
                if (smsCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    spentYear += sms.getSMSAmount();

                    // Check if it happened in the current month
                    if (smsCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                        spentMonth += sms.getSMSAmount();

                        // Check if it happened in the current week
                        if (smsCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                            spentWeek += sms.getSMSAmount();
                        }

                        // Check if it happened today
                        if (smsCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                            spentToday += sms.getSMSAmount();
                        }
                    }
                }
            }
        }

        return new Totals(spentToday, spentWeek, spentMonth, spentYear);
    }
}
