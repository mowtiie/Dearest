package com.mowtiie.dearest.ui.adapters;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mowtiie.dearest.R;
import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    public interface OnDayClick {
        void onDayClick(LocalDate date);
    }

    public static final class Day {
        public final LocalDate date;
        public final boolean hasEntries;

        public Day(@Nullable LocalDate date, boolean hasEntries) {
            this.date = date;
            this.hasEntries = hasEntries;
        }
    }

    private final List<Day> days = new ArrayList<>();
    private final LocalDate today = LocalDate.now();
    private final OnDayClick listener;
    private LocalDate selected;

    public CalendarDayAdapter(OnDayClick listener) {
        this.listener = listener;
    }

    public void submit(List<Day> newDays, LocalDate selected) {
        this.days.clear();
        this.days.addAll(newDays);
        this.selected = selected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(days.get(position), selected, today, listener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final View cell;
        private final TextView number;
        private final View dot;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cell = itemView.findViewById(R.id.day_cell);
            number = itemView.findViewById(R.id.day_number);
            dot = itemView.findViewById(R.id.day_dot);
        }

        void bind(Day day, LocalDate selected, LocalDate today, OnDayClick listener) {
            if (day.date == null) {
                number.setText("");
                dot.setVisibility(View.INVISIBLE);
                cell.setBackground(null);
                itemView.setClickable(false);
                itemView.setOnClickListener(null);
                return;
            }

            number.setText(String.valueOf(day.date.getDayOfMonth()));
            dot.setVisibility(day.hasEntries ? View.VISIBLE : View.INVISIBLE);

            boolean isSelected = day.date.equals(selected);
            boolean isToday = day.date.equals(today);

            if (isSelected) {
                cell.setBackgroundResource(R.drawable.bg_calendar_selected);
                number.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_onPrimaryContainer));
            } else {
                cell.setBackground(null);
                number.setTextColor(ContextCompat.getColor(itemView.getContext(), isToday ? R.color.md_theme_primary : R.color.md_theme_onSurface));
            }
            number.setTypeface(null, isToday ? Typeface.BOLD : Typeface.NORMAL);

            itemView.setClickable(true);
            itemView.setOnClickListener(v -> listener.onDayClick(day.date));
        }
    }
}
