package com.mowtiie.dearest.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.dearest.data.model.Entry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarViewModel extends DearestViewModel {

    private final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final LiveData<Set<LocalDate>> daysWithEntries;
    private final LiveData<List<Entry>> entriesForSelectedDate;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        // Only the visible month and the selected day are queried, not the whole journal.
        daysWithEntries = Transformations.switchMap(month, m -> {
            LiveData<List<Long>> created = repository().observeCreatedAtBetween(
                    startOfDay(m.atDay(1)), startOfDay(m.plusMonths(1).atDay(1)));
            return Transformations.map(created, CalendarViewModel::datesOf);
        });
        entriesForSelectedDate = Transformations.switchMap(selectedDate, date ->
                repository().observeEntriesCreatedBetween(startOfDay(date), startOfDay(date.plusDays(1))));

        LocalDate today = LocalDate.now();
        month.setValue(YearMonth.from(today));
        selectedDate.setValue(today);
    }

    public LiveData<YearMonth> month() { return month; }
    public LiveData<LocalDate> selectedDate() { return selectedDate; }
    public LiveData<Set<LocalDate>> daysWithEntries() { return daysWithEntries; }
    public LiveData<List<Entry>> entriesForSelectedDate() { return entriesForSelectedDate; }

    public void nextMonth() {
        YearMonth m = month.getValue();
        if (m != null) month.setValue(m.plusMonths(1));
    }

    public void prevMonth() {
        YearMonth m = month.getValue();
        if (m != null) month.setValue(m.minusMonths(1));
    }

    public void selectDate(LocalDate date) {
        selectedDate.setValue(date);
    }

    private static Set<LocalDate> datesOf(List<Long> createdAt) {
        Set<LocalDate> set = new HashSet<>();
        if (createdAt != null) {
            for (long t : createdAt) set.add(toDate(t));
        }
        return set;
    }

    private static long startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDate toDate(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
