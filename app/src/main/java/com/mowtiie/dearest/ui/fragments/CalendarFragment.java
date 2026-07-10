package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.ui.activities.EntryEditorActivity;
import com.mowtiie.dearest.ui.adapters.CalendarDayAdapter;
import com.mowtiie.dearest.ui.adapters.EntryAdapter;
import com.mowtiie.dearest.ui.viewmodel.CalendarViewModel;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private final Locale locale = Locale.getDefault();
    private final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", locale);
    private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale);

    private CalendarViewModel viewModel;
    private CalendarDayAdapter dayAdapter;
    private EntryAdapter entryAdapter;

    private TextView monthLabel;
    private LinearLayout weekdayHeader;
    private TextView selectedDateLabel;
    private TextView dayEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        monthLabel = view.findViewById(R.id.month_label);
        weekdayHeader = view.findViewById(R.id.weekday_header);
        selectedDateLabel = view.findViewById(R.id.selected_date_label);
        dayEmpty = view.findViewById(R.id.day_empty);

        buildWeekdayHeader();

        RecyclerView grid = view.findViewById(R.id.calendar_grid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        grid.setNestedScrollingEnabled(false);
        dayAdapter = new CalendarDayAdapter(viewModel::selectDate);
        grid.setAdapter(dayAdapter);

        RecyclerView entries = view.findViewById(R.id.day_entries);
        entries.setLayoutManager(new LinearLayoutManager(requireContext()));
        entryAdapter = new EntryAdapter(e ->
                EntryEditorActivity.open(requireContext(), e.getId(), null));
        entries.setAdapter(entryAdapter);

        view.findViewById(R.id.prev_month).setOnClickListener(v -> viewModel.prevMonth());
        view.findViewById(R.id.next_month).setOnClickListener(v -> viewModel.nextMonth());

        viewModel.month().observe(getViewLifecycleOwner(), m -> renderGrid());
        viewModel.daysWithEntries().observe(getViewLifecycleOwner(), s -> renderGrid());
        viewModel.selectedDate().observe(getViewLifecycleOwner(), d -> {
            renderGrid();
            if (d != null) selectedDateLabel.setText(dayFormat.format(d));
        });
        viewModel.entriesForSelectedDate().observe(getViewLifecycleOwner(), list -> {
            entryAdapter.submitList(list);
            dayEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    private DayOfWeek firstDayOfWeek() {
        return WeekFields.of(locale).getFirstDayOfWeek();
    }

    private void buildWeekdayHeader() {
        weekdayHeader.removeAllViews();
        DayOfWeek start = firstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = start.plus(i);
            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(dow.getDisplayName(TextStyle.SHORT, locale));
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall);
            weekdayHeader.addView(tv);
        }
    }

    private void renderGrid() {
        YearMonth month = viewModel.month().getValue();
        if (month == null) return;
        Set<LocalDate> days = viewModel.daysWithEntries().getValue();
        LocalDate selected = viewModel.selectedDate().getValue();

        List<CalendarDayAdapter.Day> cells = new ArrayList<>();
        LocalDate first = month.atDay(1);
        int offset = (first.getDayOfWeek().getValue() - firstDayOfWeek().getValue() + 7) % 7;
        for (int i = 0; i < offset; i++) {
            cells.add(new CalendarDayAdapter.Day(null, false));
        }
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            cells.add(new CalendarDayAdapter.Day(date, days != null && days.contains(date)));
        }

        dayAdapter.submit(cells, selected);
        monthLabel.setText(monthFormat.format(month));
    }
}