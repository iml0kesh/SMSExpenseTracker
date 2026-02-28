package com.example.smsexpensetracker.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions", indices = {@Index(value = "sms_id", unique = true)})
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "sms_id")
    public String smsId;

    @ColumnInfo(name = "sender")
    public String sender;

    @ColumnInfo(name = "body")
    public String body;

    @ColumnInfo(name = "amount")
    public double amount;

    @ColumnInfo(name = "type")
    public String type; // DEBIT | CREDIT | UNKNOWN

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "merchant")
    public String merchant;

    @ColumnInfo(name = "date_millis")
    public long dateMillis;

    public Transaction() {}

    public Transaction(String smsId, String sender, String body,
                       double amount, String type, String category,
                       String merchant, long dateMillis) {
        this.smsId = smsId;
        this.sender = sender;
        this.body = body;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.merchant = merchant;
        this.dateMillis = dateMillis;
    }
}
