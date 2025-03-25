package com.example.smsexpensetracker;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> smsList = new ArrayList<>();

    private ArrayList<SmsComponent> smsComponents = new ArrayList<>();
    private ArrayList<Double> allAmount = new ArrayList<>();

    private ArrayList<Date> AmountDate = new ArrayList<>();
    private ListView listViewOfSMS;
    private ListView listViewOfAmount;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    private static double totalDebitAmount = 0.0;
    private  static double totalCreditAmount = 0.0;

    SmsProcessor processor = new SmsProcessor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // This will displat all the SMS
    //        listViewOfSMS = findViewById(R.id.listViewOfSMS);
    //        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
    //        listViewOfSMS.setAdapter(adapter);

//        listViewOfAmount = findViewById(R.id.listViewOfAmount);
//        ArrayAdapter<Double> adapterAmount = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allAmount);
//        listViewOfAmount.setAdapter(adapterAmount);

//        listViewOfAmount = findViewById(R.id.listViewOfAmount);
//        ArrayAdapter<Date> adapterDate = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AmountDate);
//        listViewOfAmount.setAdapter(adapterDate);

        listViewOfSMS = findViewById(R.id.listViewOfSMS);
        ArrayAdapter<SmsComponent> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsComponents);
        listViewOfSMS.setAdapter(adapter);

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
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listViewOfSMS.getAdapter();
                adapter.notifyDataSetChanged();

                ArrayAdapter<Double> adapterAmount = (ArrayAdapter<Double>) listViewOfAmount.getAdapter();
                adapterAmount.notifyDataSetChanged();
            } else {
                System.out.println("Permission denied.");
            }
        }
    }
}
