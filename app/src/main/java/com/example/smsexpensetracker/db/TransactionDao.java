package com.example.smsexpensetracker.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.smsexpensetracker.models.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Transaction t);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Transaction> list);

    @Update
    void update(Transaction t);

    @Query("SELECT * FROM transactions WHERE sender IN (:senders) AND (:cat IS NULL OR category = :cat) ORDER BY date_millis DESC LIMIT :limit")
    LiveData<List<Transaction>> getFilteredLive(List<String> senders, String cat, int limit);

    @Query("SELECT COUNT(*) FROM transactions WHERE sender IN (:senders) AND (:cat IS NULL OR category = :cat)")
    int getFilteredCount(List<String> senders, String cat);

    @Query("SELECT * FROM transactions ORDER BY date_millis DESC")
    List<Transaction> getAll();

    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='DEBIT' AND sender IN (:senders) AND date_millis >= :from AND date_millis <= :to")
    double sumDebitFilteredBetween(List<String> senders, long from, long to);

    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type='DEBIT' AND category=:cat AND sender IN (:senders) AND date_millis >= :from AND date_millis <= :to")
    double sumDebitByCategoryFilteredBetween(String cat, List<String> senders, long from, long to);

    @Query("SELECT COUNT(*) FROM transactions WHERE sms_id = :smsId")
    int existsBySmsId(String smsId);

    @Query("DELETE FROM transactions")
    void deleteAll();
}
