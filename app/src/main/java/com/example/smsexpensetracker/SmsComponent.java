package com.example.smsexpensetracker;

import androidx.annotation.NonNull;

import java.util.Date;

public class SmsComponent {
    private String SMSSender;
    private String SMSBody;
    private Date SMSDate;
    private Double SMSAmount;
    private String amountType;
    private Double totalDebitAmount;
    private Double totalCreditAmount;

    public SmsComponent(String SMSSender, String SMSBody, Date SMSDate, Double SMSAmount, String amountType) {
        this.SMSSender = SMSSender;
        this.SMSBody = SMSBody;
        this.SMSDate = SMSDate;
        this.SMSAmount = SMSAmount;
        this.amountType = amountType;
    }

    public String getSMSBody() {
        return SMSBody;
    }

    public void setSMSBody(String SMSBody) {
        this.SMSBody = SMSBody;
    }

    public String getSMSSender() {
        return SMSSender;
    }

    public void setSMSSender(String SMSSender) {
        this.SMSSender = SMSSender;
    }

    public Date getSMSDate() {
        return SMSDate;
    }

    public void setSMSDate(Date SMSDate) {
        this.SMSDate = SMSDate;
    }

    public Double getSMSAmount() {
        return SMSAmount;
    }

    public String getAmountType() {
        return amountType;
    }

    public void setAmountType(String amountType) {
        this.amountType = amountType;
    }

    public Double getTotalDebitAmount() {
        return totalDebitAmount;
    }

    public void setTotalDebitAmount(Double totalDebitAmount) {
        this.totalDebitAmount = totalDebitAmount;
    }

    public Double getTotalCreditAmount() {
        return totalCreditAmount;
    }

    public void setTotalCreditAmount(Double totalCreditAmount) {
        this.totalCreditAmount = totalCreditAmount;
    }

    public void setSMSAmount(Double SMSAmount) {
        this.SMSAmount = SMSAmount;
    }

    @NonNull
    @Override
    public String toString() {
        return "SmsComponent:\n" +
                "Sender = " + SMSSender + '\n' +
                "Body = " + SMSBody + '\n' +
                "SMSDate = " + SMSDate + '\n' +
                "SMSAmount = " + SMSAmount + '\n' +
                "Type = " + amountType;
    }

}
