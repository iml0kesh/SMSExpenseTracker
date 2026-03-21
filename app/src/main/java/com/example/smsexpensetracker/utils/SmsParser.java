package com.example.smsexpensetracker.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    public static class Result {
        public final double amount;
        public final String type;
        public final String merchant;
        public Result(double amount, String type, String merchant) {
            this.amount = amount; this.type = type; this.merchant = merchant;
        }
    }

    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("[₹]\\s*([1-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)Rs\\.?\\s*([1-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)INR\\s*([1-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:debited?|credited?|withdrawn|spent|paid)(?:\\s+(?:with|by|for|of))?\\s+(?:Rs\\.?|INR|[₹])?\\s*([1-9][0-9,]*(?:\\.[0-9]{1,2})?)"),
    };

    private static final String[] DEBIT_WORDS = {
        "debited", "debit", "withdrawn", "withdrawal", "spent", "paid",
        "payment", "sent", "deducted", "charged", "purchase", "used",
        "transfer to", "transferred to", "trf to", " dr "
    };

    private static final String[] CREDIT_WORDS = {
        "credited", "credit", "received", "deposited", "deposit",
        "added", "refunded", "refund", "reversed", "reversal",
        "cashback", "transfer from", "transferred from", "trf from", " cr "
    };

    private static final String[] NOISE = {
        "otp", "one time password", "verification code",
        "click here", "unsubscribe", "reply stop", "winner", "prize"
    };

    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
        "(?i)(?:at |to |towards |at merchant |info:|trf to )([A-Za-z0-9][A-Za-z0-9@._\\- ]{2,28}?)(?:\\s+on|\\s+ref|\\s+upi|\\.|,|$)"
    );

    public static Result parse(String body) {
        if (body == null || body.trim().isEmpty())
            return new Result(0, "UNKNOWN", null);
        String lower = body.toLowerCase();
        for (String n : NOISE) if (lower.contains(n)) return new Result(0, "UNKNOWN", null);
        double amount   = extractAmount(body);
        String type     = extractType(lower);
        String merchant = extractMerchant(body);
        return new Result(amount, type, merchant);
    }

    private static double extractAmount(String body) {
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(body);
            if (m.find()) {
                try {
                    double val = Double.parseDouble(m.group(1).replace(",", ""));
                    if (val > 0 && val < 10_000_000) return val;
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static String extractType(String lower) {
        for (String w : DEBIT_WORDS)  if (lower.contains(w)) return "DEBIT";
        for (String w : CREDIT_WORDS) if (lower.contains(w)) return "CREDIT";
        return "UNKNOWN";
    }

    private static String extractMerchant(String body) {
        Matcher m = MERCHANT_PATTERN.matcher(body);
        if (m.find()) {
            String val = m.group(1).trim();
            if (val.length() >= 3 && !isGeneric(val)) {
                return Character.toUpperCase(val.charAt(0)) + val.substring(1);
            }
        }
        return null;
    }

    private static boolean isGeneric(String s) {
        String[] skip = {"your","the","a/c","ac","account","bank","upi","ref","txn","neft","imps","rtgs","you"};
        String l = s.toLowerCase().trim();
        for (String g : skip) if (l.equals(g)) return true;
        return false;
    }
}
