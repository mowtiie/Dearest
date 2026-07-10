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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarViewModel extends DearestViewModel {

    private final LiveData<List<Entry>> allEntries;
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>();
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();
    private final LiveData<Set<LocalDate>> daysWithEntries;
    private final LiveData<List<Entry>> entriesForSelectedDate;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        allEntries = repository().observeAllEntries();
        daysWithEntries = Transformations.map(allEntries, this::datesOf);
        entriesForSelectedDate = Transformations.switchMap(selectedDate, date ->
                Transformations.map(allEntries, list -> entriesOn(list, date)));

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

    private Set<LocalDate> datesOf(List<Entry> entries) {
        Set<LocalDate> set = new HashSet<>();
        if (entries != null) {
            for (Entry e : entries) set.add(toDate(e.getCreatedAt()));
        }
        return set;
    }

    private List<Entry> entriesOn(List<Entry> entries, LocalDate date) {
        List<Entry> out = new ArrayList<>();
        if (entries != null && date != null) {
            for (Entry e : entries) {
                if (toDate(e.getCreatedAt()).equals(date)) out.add(e);
            }
        }
        return out;
    }

    private static LocalDate toDate(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
