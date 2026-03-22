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

    // ── Amount patterns (most specific first) ────────────────────────────
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

    // ── Merchant patterns ─────────────────────────────────────────────────

    private static final Pattern ICICI_FORMAT = Pattern.compile("(?i);\\s*([^;]+?)\\s+credited");

    private static final Pattern[] MERCHANT_PATTERNS = {
            Pattern.compile("(?i)(?:at |towards |at merchant |info:|trf to )([A-Z][A-Za-z0-9@._\\- ]{2,30}?)(?:\\s+on|\\s+ref|\\s+upi|\\.|,|$|\\s+received)"),
            Pattern.compile("(?i)(?:paid to |transfer to |trf to )([A-Z][A-Za-z0-9\\s\\.]{2,30}?)")
    };

    public static Result parse(String body) {
        if (body == null || body.trim().isEmpty())
            return new Result(0, "UNKNOWN", null);
        String lower = body.toLowerCase();
        for (String n : NOISE) {
            if (lower.contains(n)) return new Result(0, "UNKNOWN", null);
        }
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
        Matcher iciciMatcher = ICICI_FORMAT.matcher(body);
        if (iciciMatcher.find()) {
            String val = iciciMatcher.group(1).trim();
            if (isValidMerchant(val)) return formatName(val);
        }

        for (Pattern p : MERCHANT_PATTERNS) {
            Matcher m = p.matcher(body);
            while (m.find()) {
                String val = m.group(1).trim();
                if (isValidMerchant(val)) return formatName(val);
            }
        }
        return null;
    }

    private static boolean isValidMerchant(String val) {
        if (val == null || val.length() < 3) return false;
        if (val.matches(".*\\d{5,}.*")) return false;
        String l = val.toLowerCase().trim();
        if (l.contains("sms block") || l.contains("to block") || l.contains("dispute") || l.contains("call")) return false;
        
        String[] skip = {"your","the","a/c","ac","account","bank","upi","ref","txn","neft","imps","rtgs","you", "sms", "block"};
        for (String g : skip) if (l.equals(g)) return false;

        return true;
    }

    private static String formatName(String val) {
        if (val == null || val.isEmpty()) return val;
        return Character.toUpperCase(val.charAt(0)) + val.substring(1);
    }

    public static String getBankName(String sender) {
        if (sender == null || sender.isEmpty()) return "Other";
        String entity = sender.toUpperCase();
        if (entity.length() >= 4 && entity.charAt(2) == '-') {
            entity = entity.substring(3);
        }

        if (entity.contains("HDFCBK")) return "HDFC";
        if (entity.contains("ICICIB") || entity.contains("ICICI")) return "ICICI";
        if (entity.contains("SBI")) return "SBI";
        if (entity.contains("AXISBK") || entity.contains("AXIS")) return "Axis";
        if (entity.contains("KOTAK")) return "Kotak";
        if (entity.contains("PNB")) return "PNB";
        if (entity.contains("BOB")) return "BOB";
        if (entity.contains("CANARA")) return "Canara";
        if (entity.contains("PAYTM")) return "Paytm";
        if (entity.contains("PHONEPE")) return "PhonePe";
        
        return "Bank";
    }
}
