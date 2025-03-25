package com.example.smsexpensetracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsProcessor {
    private double totalDebitAmount = 0.0;
    private double totalCreditAmount = 0.0;
    private List<Double> allAmount = new ArrayList<>();

    private String amountType;
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
        ProcessResult amount = extractAndProcessAmount(smsBody, "ICICI Bank Acct XX314 debited for Rs ([\\d,.]+)", TransactionType.DEBIT);
        if( amount.type != TransactionType.UNKNOWN) return amount;

        return extractAndProcessAmount(smsBody, "Dear Customer, Acct XX123 is credited with Rs ([\\d,.]+)", TransactionType.CREDIT);
    }

    private ProcessResult extractAndProcessAmount(String smsBody, String regex, TransactionType type) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(smsBody);

        String amountString = matcher.find() ? matcher.group(1) : null;

        if(amountString != null) {
            try {
                double parsedAmount = Double.parseDouble(amountString.replace(",", ""));
                allAmount.add(parsedAmount);

                if(type == TransactionType.DEBIT) {
                    totalDebitAmount += parsedAmount;
                    return new ProcessResult(parsedAmount, TransactionType.DEBIT);
                } else if(type == TransactionType.CREDIT) {
                    totalCreditAmount += parsedAmount;
                    return new ProcessResult(parsedAmount, TransactionType.CREDIT);
                }
            } catch (NumberFormatException e) {
                System.out.println("Error converting amount to number");
            }
        }
        return new ProcessResult(0.0, TransactionType.UNKNOWN);
    }

    public double getTotalDebitAmount() {
        return totalDebitAmount;
    }

    public double getTotalCreditAmount() {
        return totalCreditAmount;
    }

    public List<Double> getAllAmount() {
        return allAmount;
    }

    public String getAmountType() {
        return amountType;
    }
}
