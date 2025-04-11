package com.example.smsexpensetracker;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ArrayList<String> smsList = new ArrayList<>();

    private ArrayList<SmsComponent> smsComponents = new ArrayList<>();
    private ArrayList<Double> allAmount = new ArrayList<>();

    private ArrayList<Date> AmountDate = new ArrayList<>();
    private ListView listViewOfSMS;
    private ListView listViewOfAmount;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    private static double totalDebitAmount = 0.0;
    private  static double totalCreditAmount = 0.0;

    private TextView totalSpentTodayTextView;
    private TextView totalSpentYesterdayTextView;
    private TextView totalSpentThisMonthTextView;
    private TextView totalIncomeTextView;
    private TextView netIncomeExpenseTextView;

    private TextView topPayee1TextView;
    private TextView topPayee2TextView;
    private TextView topPayee3TextView;

    private ListView listViewOfTopPayee;


    private Map<String, Double> payeeAmounts;

    SmsProcessor processor = new SmsProcessor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize UI elements
        totalSpentTodayTextView = findViewById(R.id.totalSpentTodayTextView);
        totalSpentYesterdayTextView = findViewById(R.id.totalSpentYesterdayTextView);
        totalSpentThisMonthTextView = findViewById(R.id.totalSpentThisMonthTextView);
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView);
        netIncomeExpenseTextView = findViewById(R.id.netIncomeExpenseTextView);
        listViewOfTopPayee = findViewById(R.id.listViewOfTopPayee);

        topPayee1TextView = findViewById(R.id.topPayee1TextView);
        topPayee2TextView = findViewById(R.id.topPayee2TextView);
        topPayee3TextView = findViewById(R.id.topPayee3TextView);

        // Initialize transaction data
        payeeAmounts = new HashMap<>();

        // For demo purpose we are creating some static data.
        List<SmsComponent> simulatedTransactions = getSimulatedTransactions();
        processTransactions(simulatedTransactions);
        updateUI();





        // This will display all the SMS

        // listViewOfSMS.setAdapter(adapter);


    //        listViewOfSMS = findViewById(R.id.listViewOfSMS);
    //        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
    //        listViewOfSMS.setAdapter(adapter);

//        listViewOfAmount = findViewById(R.id.listViewOfAmount);
//        ArrayAdapter<Double> adapterAmount = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allAmount);
//        listViewOfAmount.setAdapter(adapterAmount);

//        listViewOfAmount = findViewById(R.id.listViewOfAmount);
//        ArrayAdapter<Date> adapterDate = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AmountDate);
//        listViewOfAmount.setAdapter(adapterDate);

//        listViewOfSMS = findViewById(R.id.listViewOfSMS);
//        ArrayAdapter<SmsComponent> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsComponents);
//        listViewOfSMS.setAdapter(adapter);
//
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            String[] possibleSenders = new String[]{"AD-ICICIT", "JD-ICICIT", "JM-ICICIT", "JX-ICICIT"};
            readSms(possibleSenders);
            // readSms(); // This will read all the messages.
        }
    }

    private void processTransactions(List<SmsComponent> transactions) {
        double totalSpentToday = 0;
        double totalSpentYesterday = 0;
        double totalSpentThisMonth = 0;
        double totalIncome = 0;
        double totalExpense = 0;

        Date today = new Date();
        long oneDay = 24 * 60 * 60 * 1000;
        Date yesterday = new Date(today.getTime() - oneDay);

        for (SmsComponent transaction : transactions) {
            Date transactionDate = transaction.getSmsDate();

            if(processor.isSameDay(transactionDate, today)){
                if(transaction.getType().equals("DEBIT")){
                    totalSpentToday += transaction.getAmount();
                }
            }
            if(processor.isSameDay(transactionDate, yesterday)){
                if(transaction.getType().equals("DEBIT")){
                    totalSpentYesterday += transaction.getAmount();
                }
            }

            if(processor.isSameMonth(transactionDate, today)){
                if(transaction.getType().equals("DEBIT")){
                    totalSpentThisMonth += transaction.getAmount();
                    totalExpense += transaction.getAmount();
                } else if (transaction.getType().equals("CREDIT")) {
                    totalIncome += transaction.getAmount();
                }
            }

            if(transaction.getType().equals("DEBIT")) {
                String payee = processor.extractPayee(transaction.getBody());
                payeeAmounts.put(payee, payeeAmounts.getOrDefault(payee, 0.0) + transaction.getAmount());
            }
        }

        totalDebitAmount = totalExpense;
        totalCreditAmount = totalIncome;
        updateUI(totalSpentToday, totalSpentYesterday, totalSpentThisMonth, totalIncome, totalExpense);
    }

    private void updateUI(double totalSpentToday, double totalSpentYesterday, double totalSpentThisMonth, double totalIncome, double totalExpense) {
        // Update summary section
        totalSpentTodayTextView.setText(String.format("₹ %.2f", totalSpentToday));
        totalSpentYesterdayTextView.setText(String.format("₹ %.2f", totalSpentYesterday));
        totalSpentThisMonthTextView.setText(String.format("₹ %.2f", totalSpentThisMonth));
        totalIncomeTextView.setText(String.format("₹ %.2f", totalIncome));
        netIncomeExpenseTextView.setText(String.format("₹ %.2f", totalIncome - totalExpense));

        // Update top payees section
        List<Map.Entry<String, Double>> sortedPayees = new ArrayList<>(payeeAmounts.entrySet());
        sortedPayees.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        int topPayeesToShow = Math.min(sortedPayees.size(), 3);
        for (int i = 0; i < topPayeesToShow; i++) {
            Map.Entry<String, Double> payeeEntry = sortedPayees.get(i);
            String payeeName = payeeEntry.getKey();
            double payeeAmount = payeeEntry.getValue();

            if (i == 0) {
                topPayee1TextView.setText(String.format("%s - ₹ %.2f", payeeName, payeeAmount));
            } else if (i == 1) {
                topPayee2TextView.setText(String.format("%s - ₹ %.2f", payeeName, payeeAmount));
            } else if (i == 2) {
                topPayee3TextView.setText(String.format("%s - ₹ %.2f", payeeName, payeeAmount));
            }
        }

        for (int i = topPayeesToShow; i < 3; i++) {
            if (i == 0) {
                topPayee1TextView.setText("");
            } else if (i == 1) {
                topPayee2TextView.setText("");
            } else if (i == 2) {
                topPayee3TextView.setText("");
            }
        }
    }


    // This function will get the messages only from a specific number.
    private void readSms(String[] possibleSenders) {
        ContentResolver contentResolver = getContentResolver();

//        String selection = Telephony.Sms.ADDRESS + " = ?"; // Selection criteria
//        String[] selectionArgs = {phoneNumber};

        StringBuilder selectionBuilder = new StringBuilder();
        String[] selectionArgs = new String[possibleSenders.length];

        selectionBuilder.append(Telephony.Sms.ADDRESS + " IN (");
        for(int i=0; i<possibleSenders.length; i++){
         selectionBuilder.append("?");
         selectionArgs[i] = possibleSenders[i];
         if(i < possibleSenders.length - 1) {
             selectionBuilder.append(", ");
         }
        }

        selectionBuilder.append(")");

        String selection = selectionBuilder.toString();

        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // This will get the sender.
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                // This will get what the sender has sent.
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                Date smsDate = new Date(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
//                AmountDate.add(smsDate);

                SmsProcessor.ProcessResult result = processor.processSms(body);
                String Type = result.type == SmsProcessor.TransactionType.DEBIT ? "DEBIT" : "CREDIT";

                SmsComponent smsComponent = new SmsComponent(address, body, smsDate, result.amount, Type);
                smsComponents.add(smsComponent);

//                smsList.add("Sender: " + address + "\nMessage: " + body + "\nAmount: " + processSms(body));

            } while (cursor.moveToNext());
        }

//        smsList.add("Total debit amount: " + totalDebitAmount);
//        smsList.add("Total Credit Amount: " + totalCreditAmount);
        if (cursor != null) {
            cursor.close();
        }
    }

    // This  function will get all the messages.
    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                smsList.add("Sender: " + address + "\nMessage: " + body);
//                amountList.add(processSms(body));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private List<SmsComponent> getSimulatedTransactions(){

        List<SmsComponent> simulatedTransactions = new ArrayList<>();
        // Simulate some transactions
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 1000.00 on 23-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis()), 1000.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "Dear Customer, Acct XX314 is credited with Rs 2000.00 on 23-Nov-2023 00:15:52", new Date(), 2000.00, "CREDIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 500.00 on 22-Nov-2023 00:15:52. Info: UPI/811465454545/zomato.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000), 500.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 750.00 on 20-Nov-2023 00:15:52. Info: UPI/811465454545/flipkart.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000), 750.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 200.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 200.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 300.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/flipkart.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 300.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 400.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/zomato.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 400.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 500.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 500.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 100.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/flipkart.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 100.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 200.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 200.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 1000.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 1000.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 200.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/zomato.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 200.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 100.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/flipkart.com/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 100.00, "DEBIT"));
        simulatedTransactions.add(new SmsComponent("AD-ICICIT", "ICICI Bank Acct XX314 debited for Rs 1000.00 on 10-Nov-2023 00:15:52. Info: UPI/811465454545/amazon.in/P2A/100100300400/NA/0000000000/Ref no 327085454545", new Date(System.currentTimeMillis() - 13 * 24 * 60 * 60 * 1000), 1000.00, "DEBIT"));


        return simulatedTransactions;
    }

    private void updateUI(){
        updateUI(0,0,0,0,0);
    }


//    public double processSms(String smsBody) {
//
//        String regexDebit = "ICICI Bank Acct XX314 debited for Rs ([\\d,.]+) on ";
//
//        Pattern pattern = Pattern.compile(regexDebit);
//        Matcher matcher = pattern.matcher(smsBody);
//
//        String amount = matcher.find() ? matcher.group(1) : null;
//
//        if (amount != null) {
//            String numericAmountString = amount.replace(",", "");
//            try {
//                double parsedAmount = Double.parseDouble(numericAmountString);
//                totalDebitAmount += parsedAmount;
//                allAmount.add(parsedAmount);
//                return parsedAmount;
//
//            } catch (NumberFormatException e) {
//                System.out.println("Error converting amount to number.");
//            }
//
//        } else {
//            System.out.println("Amount not found in SMS.");
//        }
//
//        String regexCredit = "Dear Customer, Acct XX314 is credited with Rs ([\\d,.]+) on ";
//        Pattern patternCredit = Pattern.compile(regexCredit);
//        Matcher matcherCredit = patternCredit.matcher(smsBody);
//
//        String amountCredit = matcherCredit.find() ? matcherCredit.group(1) : null;
//
//        if (amountCredit != null) {
//            System.out.println("Extracted amount: " + amountCredit);
//            String numericAmountString = amountCredit.replace(",", "");
//            try {
//                double parsedCreditAmount = Double.parseDouble(numericAmountString);
//                totalCreditAmount += parsedCreditAmount;
//                return parsedCreditAmount;
//            } catch (NumberFormatException e) {
//                System.out.println("Error converting amount to number.");
//            }
//        } else {
//            System.out.println("Amount not found in SMS.");
//        }
//
//        return 0.0;
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //                readSms("AD-ICICIT");
                //                readSms();
//                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listViewOfSMS.getAdapter();
//                adapter.notifyDataSetChanged();
//
//                ArrayAdapter<Double> adapterAmount = (ArrayAdapter<Double>) listViewOfAmount.getAdapter();
//                adapterAmount.notifyDataSetChanged();
            } else {
                System.out.println("Permission denied.");
            }
        }
    }
}
