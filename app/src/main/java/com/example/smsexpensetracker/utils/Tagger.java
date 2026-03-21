package com.example.smsexpensetracker.utils;

public class Tagger {

    public static final String FOOD          = "Food";
    public static final String TRANSPORT     = "Transport";
    public static final String SHOPPING      = "Shopping";
    public static final String ENTERTAINMENT = "Entertainment";
    public static final String HEALTH        = "Health";
    public static final String UTILITIES     = "Utilities";
    public static final String FUEL          = "Fuel";
    public static final String ATM           = "ATM";
    public static final String TRANSFER      = "Transfer";
    public static final String EMI           = "EMI";
    public static final String INVESTMENT    = "Investment";
    public static final String OTHER         = "Other";

    private static final Object[][] RULES = {
        {FOOD,          new String[]{"swiggy","zomato","domino","pizza","mcdonald","kfc","burger","subway","cafe","restaurant","dining","food","bakery","blinkit","zepto","bigbasket","grocer","grocery","dunzo","canteen","mess","biryani","dine"}},
        {TRANSPORT,     new String[]{"uber","ola","rapido","metro","irctc","railway","train","bus","flight","airline","indigo","spicejet","redbus","makemytrip","goibibo","cab","auto","rickshaw","yulu","travel","ticket"}},
        {FUEL,          new String[]{"fuel","petrol","diesel","pump","hp petro","bharat petroleum","indian oil","iocl","hpcl","bpcl","reliance petro","shell"}},
        {SHOPPING,      new String[]{"amazon","flipkart","myntra","ajio","meesho","nykaa","snapdeal","tatacliq","jiomart","ikea","decathlon","zara","croma","vijay sales","mall","shop","purchase"}},
        {ENTERTAINMENT, new String[]{"netflix","hotstar","prime video","spotify","gaana","wynk","disney","zee5","sonyliv","bookmyshow","pvr","inox","cinema","movie","gaming","steam"}},
        {HEALTH,        new String[]{"pharmacy","medical","hospital","clinic","doctor","apollo","1mg","pharmeasy","medplus","netmeds","diagnostic","medicine","chemist","health"}},
        {UTILITIES,     new String[]{"electricity","bescom","tata power","bses","msedcl","water bill","gas bill","broadband","airtel","jio","bsnl","vodafone"," vi ","recharge","mobile bill","utility","municipal"}},
        {ATM,           new String[]{"atm","cash withdrawal","cash advance"}},
        {EMI,           new String[]{"emi","loan","equated","repayment","installment","bajaj finance","credit card bill","card dues"}},
        {INVESTMENT,    new String[]{"mutual fund","sip","zerodha","groww","upstox","kuvera","nps","ppf","fixed deposit"," fd "," rd ","equity","demat","stock"}},
        {TRANSFER,      new String[]{"upi","neft","imps","rtgs","transfer","phonepe","gpay","google pay","paytm","bhim"}},
    };

    public static String tag(String body, String merchant) {
        String text = ((body != null ? body : "") + " " + (merchant != null ? merchant : "")).toLowerCase();
        for (Object[] rule : RULES) {
            for (String kw : (String[]) rule[1]) {
                if (text.contains(kw)) return (String) rule[0];
            }
        }
        return OTHER;
    }
}
