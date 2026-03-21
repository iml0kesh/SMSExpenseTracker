package com.example.smsexpensetracker.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.smsexpensetracker.db.AppDatabase;
import com.example.smsexpensetracker.db.TransactionDao;
import com.example.smsexpensetracker.models.Transaction;
import com.example.smsexpensetracker.utils.DateUtils;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.utils.SmsIngestor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainViewModel extends AndroidViewModel {

    public static class Summary {
        public double today, week, month, year;
    }

    public static class CategoryBreakdown {
        public Map<String, Double> totals = new HashMap<>();
    }

    private static final String[] ALL_CATS = {
        "Food","Transport","Shopping","Entertainment",
        "Health","Utilities","Fuel","ATM","Transfer","EMI","Investment","Other"
    };

    private final TransactionDao dao;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> filterCategory = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> limit = new MutableLiveData<>(10);
    
    public final LiveData<List<Transaction>> transactions;
    public final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    
    public final MutableLiveData<Summary> summary           = new MutableLiveData<>();
    public final MutableLiveData<CategoryBreakdown> breakdown = new MutableLiveData<>();
    public final MutableLiveData<Boolean> syncing           = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> syncResult        = new MutableLiveData<>();

    public MainViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.get(app).dao();
        
        // Combined LiveData for filtering and pagination
        LiveData<CombinedParams> params = Transformations.switchMap(filterCategory, cat -> 
            Transformations.map(limit, l -> new CombinedParams(cat, l))
        );

        transactions = Transformations.switchMap(params, p -> {
            checkHasMore(p.category, p.limit);
            if (p.category == null) return dao.getAllLive(p.limit);
            return dao.getByCategoryLive(p.category, p.limit);
        });
    }

    private void checkHasMore(String cat, int currentLimit) {
        exec.execute(() -> {
            int total = (cat == null) ? dao.getCount() : dao.getCountByCategory(cat);
            hasMore.postValue(total > currentLimit);
        });
    }

    public void loadMore() {
        Integer current = limit.getValue();
        if (current != null) limit.setValue(current + 10);
    }

    public void setFilterCategory(String cat) {
        limit.setValue(10); // Reset limit on filter change
        filterCategory.setValue(cat);
    }

    public String getFilterCategory() {
        return filterCategory.getValue();
    }

    public void sync() {
        if (Boolean.TRUE.equals(syncing.getValue())) return;
        syncing.postValue(true);
        exec.execute(() -> {
            Set<String> senders = Prefs.getSenders(getApplication());
            int count = SmsIngestor.ingest(getApplication().getContentResolver(), dao, senders);
            refreshSummaryInternal();
            syncing.postValue(false);
            if (count > 0) syncResult.postValue(count);
        });
    }

    public void refreshSummary() {
        exec.execute(this::refreshSummaryInternal);
    }

    private void refreshSummaryInternal() {
        long[] td = DateUtils.today();
        long[] wk = DateUtils.thisWeek();
        long[] mn = DateUtils.thisMonth();
        long[] yr = DateUtils.thisYear();

        Summary s = new Summary();
        s.today = dao.sumDebitBetween(td[0], td[1]);
        s.week  = dao.sumDebitBetween(wk[0], wk[1]);
        s.month = dao.sumDebitBetween(mn[0], mn[1]);
        s.year  = dao.sumDebitBetween(yr[0], yr[1]);
        summary.postValue(s);

        CategoryBreakdown cb = new CategoryBreakdown();
        for (String cat : ALL_CATS) {
            double amt = dao.sumDebitByCategoryBetween(cat, mn[0], mn[1]);
            if (amt > 0) cb.totals.put(cat, amt);
        }
        breakdown.postValue(cb);
    }

    public void updateCategory(Transaction t, String newCat) {
        exec.execute(() -> {
            t.category = newCat;
            dao.update(t);
            refreshSummaryInternal();
        });
    }

    public void getAllForExport(Consumer<List<Transaction>> callback) {
        exec.execute(() -> callback.accept(dao.getAll()));
    }

    private static class CombinedParams {
        final String category;
        final int limit;
        CombinedParams(String c, int l) { category = c; limit = l; }
    }
}
