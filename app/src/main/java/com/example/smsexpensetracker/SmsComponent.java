package com.example.smsexpensetracker;

public class SmsComponent {
    private String SMSBody;
    private String SMSSender;
    private Date SMSDate;
    private Double SMSAmount;

    public SmsComponent(String SMSBody, String SMSSender, Date SMSDate, Double SMSAmount) {
        this.SMSBody = SMSBody;
        this.SMSSender = SMSSender;
        this.SMSDate = SMSDate;
        this.SMSAmount = SMSAmount;
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

    public void setSMSAmount(Double SMSAmount) {
        this.SMSAmount = SMSAmount;
    }
}
