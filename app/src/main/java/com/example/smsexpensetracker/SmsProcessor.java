package com.example.smsexpensetracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsProcessor {
    private double totalDebitAmount = 0.0;

    public double getTotalDebitAmount() {
        return totalDebitAmount;
    }

    public void setTotalDebitAmount(double totalDebitAmount) {
        this.totalDebitAmount = totalDebitAmount;
    }

    public double getTotalCreditAmount() {
        return totalCreditAmount;
    }

    public void setTotalCreditAmount(double totalCreditAmount) {
        this.totalCreditAmount = totalCreditAmount;
    }

    private double totalCreditAmount = 0.0;
    private List<Double> allAmount = new ArrayList<>();

    private String amountType;

    public void resetTotals() {
        setTotalCreditAmount(0.0);
        setTotalDebitAmount(0.0);
    }

    public enum TransactionType {
        DEBIT, CREDIT, UNKNOWN
    }


    public static class ProcessResult {
        public double amount;
        public TransactionType type;
        public ProcessResult (double amount, TransactionType type) {
            this.amount = amount;
            this.type = type;
        }
    }
    public ProcessResult processSms(String smsBody) {

            Pattern amountPattern =
                    Pattern.compile("(Rs\\.?|INR)\\s*([\\d,]+\\.?\\d*)");

            Matcher matcher = amountPattern.matcher(smsBody);

            if (!matcher.find()) {
                return new ProcessResult(0.0, TransactionType.UNKNOWN);
            }

            double amount = Double.parseDouble(matcher.group(2).replace(",", ""));

            if (smsBody.toLowerCase().contains("debit")) {
                totalDebitAmount += amount;
                return new ProcessResult(amount, TransactionType.DEBIT);
            }

            if (smsBody.toLowerCase().contains("credit")) {
                totalCreditAmount += amount;
                return new ProcessResult(amount, TransactionType.CREDIT);
            }

            return new ProcessResult(amount, TransactionType.UNKNOWN);
        }

    }
