package com.example.smsexpensetracker.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.smsexpensetracker.db.AppDatabase;
import com.example.smsexpensetracker.db.TransactionDao;
import com.example.smsexpensetracker.models.Transaction;
import com.example.smsexpensetracker.utils.DateUtils;
import com.example.smsexpensetracker.utils.Prefs;
import com.example.smsexpensetracker.utils.SmsIngestor;
import com.example.smsexpensetracker.utils.SmsParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private final MutableLiveData<Set<String>> selectedBankNames = new MutableLiveData<>(new HashSet<>());
    
    public final LiveData<List<Transaction>> transactions;
    public final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    
    public final MutableLiveData<Summary> summary           = new MutableLiveData<>();
    public final MutableLiveData<CategoryBreakdown> breakdown = new MutableLiveData<>();
    public final MutableLiveData<Boolean> syncing           = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> syncResult        = new MutableLiveData<>();

    public MainViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.get(app).dao();
        
        // Mediator to combine all filter triggers
        MediatorLiveData<FilterParams> filterTrigger = new MediatorLiveData<>();
        filterTrigger.addSource(filterCategory, v -> notifyFilterChange(filterTrigger));
        filterTrigger.addSource(limit, v -> notifyFilterChange(filterTrigger));
        filterTrigger.addSource(selectedBankNames, v -> notifyFilterChange(filterTrigger));

        transactions = Transformations.switchMap(filterTrigger, p -> {
            List<String> allowedSenders = getAllowedSenders(p.selectedBanks);
            checkHasMore(allowedSenders, p.category, p.limit);
            return dao.getFilteredLive(allowedSenders, p.category, p.limit);
        });
    }

    private void notifyFilterChange(MediatorLiveData<FilterParams> mediator) {
        mediator.setValue(new FilterParams(filterCategory.getValue(), limit.getValue(), selectedBankNames.getValue()));
        refreshSummary();
    }

    private List<String> getAllowedSenders(Set<String> selectedBanks) {
        Set<String> allSenders = Prefs.getSenders(getApplication());
        if (selectedBanks == null || selectedBanks.isEmpty()) {
            return new ArrayList<>(allSenders);
        }
        
        List<String> filtered = new ArrayList<>();
        for (String s : allSenders) {
            if (selectedBanks.contains(SmsParser.getBankName(s))) {
                filtered.add(s);
            }
        }
        // If bank filter returns nothing (unlikely), return empty list to show nothing
        return filtered;
    }

    private void checkHasMore(List<String> senders, String cat, int currentLimit) {
        exec.execute(() -> {
            int total = dao.getFilteredCount(senders, cat);
            hasMore.postValue(total > currentLimit);
        });
    }

    public void toggleBankFilter(String bankName) {
        Set<String> current = selectedBankNames.getValue();
        if (current == null) current = new HashSet<>();
        if (current.contains(bankName)) current.remove(bankName);
        else current.add(bankName);
        selectedBankNames.setValue(current);
    }

    public void loadMore() {
        Integer current = limit.getValue();
        if (current != null) limit.setValue(current + 10);
    }

    public void setFilterCategory(String cat) {
        limit.setValue(10); 
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
        List<String> senders = getAllowedSenders(selectedBankNames.getValue());
        if (senders.isEmpty()) {
            summary.postValue(new Summary());
            breakdown.postValue(new CategoryBreakdown());
            return;
        }

        long[] td = DateUtils.today();
        long[] wk = DateUtils.thisWeek();
        long[] mn = DateUtils.thisMonth();
        long[] yr = DateUtils.thisYear();

        Summary s = new Summary();
        s.today = dao.sumDebitFilteredBetween(senders, td[0], td[1]);
        s.week  = dao.sumDebitFilteredBetween(senders, wk[0], wk[1]);
        s.month = dao.sumDebitFilteredBetween(senders, mn[0], mn[1]);
        s.year  = dao.sumDebitFilteredBetween(senders, yr[0], yr[1]);
        summary.postValue(s);

        CategoryBreakdown cb = new CategoryBreakdown();
        for (String cat : ALL_CATS) {
            double amt = dao.sumDebitByCategoryFilteredBetween(cat, senders, mn[0], mn[1]);
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

    private static class FilterParams {
        final String category;
        final int limit;
        final Set<String> selectedBanks;
        FilterParams(String c, int l, Set<String> b) { 
            category = c; limit = l; selectedBanks = b; 
        }
    }
}
