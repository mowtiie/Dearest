package com.mowtiie.dearest.ui.fragments;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.FragmentCalendarBinding;
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

    private FragmentCalendarBinding binding;
    private CalendarViewModel viewModel;
    private CalendarDayAdapter dayAdapter;
    private EntryAdapter entryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        buildWeekdayHeader();

        binding.calendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.calendarGrid.setNestedScrollingEnabled(false);
        dayAdapter = new CalendarDayAdapter(viewModel::selectDate);
        binding.calendarGrid.setAdapter(dayAdapter);

        binding.dayEntries.setLayoutManager(new LinearLayoutManager(requireContext()));
        entryAdapter = new EntryAdapter(e ->
                EntryEditorActivity.open(requireContext(), e.getId(), null));
        binding.dayEntries.setAdapter(entryAdapter);

        binding.prevMonth.setOnClickListener(v -> viewModel.prevMonth());
        binding.nextMonth.setOnClickListener(v -> viewModel.nextMonth());

        viewModel.month().observe(getViewLifecycleOwner(), m -> renderGrid());
        viewModel.daysWithEntries().observe(getViewLifecycleOwner(), s -> renderGrid());
        viewModel.selectedDate().observe(getViewLifecycleOwner(), d -> {
            renderGrid();
            if (d != null) binding.selectedDateLabel.setText(dayFormat.format(d));
        });
        viewModel.entriesForSelectedDate().observe(getViewLifecycleOwner(), list -> {
            entryAdapter.submitList(list);
            binding.dayEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private DayOfWeek firstDayOfWeek() {
        return WeekFields.of(locale).getFirstDayOfWeek();
    }

    private void buildWeekdayHeader() {
        binding.weekdayHeader.removeAllViews();
        DayOfWeek start = firstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = start.plus(i);
            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(dow.getDisplayName(TextStyle.SHORT, locale));
            tv.setTextAppearance(android.R.style.TextAppearance_Material_Small);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant));
            binding.weekdayHeader.addView(tv);
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
        binding.monthLabel.setText(monthFormat.format(month));
    }
}