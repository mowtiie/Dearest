package com.mowtiie.dearest.ui.activities;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.mowtiie.dearest.R;
import com.mowtiie.dearest.databinding.ActivitySectionJournalBinding;
import com.mowtiie.dearest.ui.InsetsUtil;
import com.mowtiie.dearest.ui.fragments.JournalFragment;

/** Time-ordered history page scoped to one Notebook Section. */
public final class SectionJournalActivity extends DearestActivity {
    static final String EXTRA_NOTEBOOK_ID = "com.dearest.extra.SECTION_JOURNAL_NOTEBOOK";
    static final String EXTRA_SECTION_ID = "com.dearest.extra.SECTION_JOURNAL_SECTION";
    static final String EXTRA_SECTION_NAME = "com.dearest.extra.SECTION_JOURNAL_NAME";

    public static void open(Context context, String notebookId, String sectionId, String sectionName) {
        context.startActivity(new Intent(context, SectionJournalActivity.class)
                .putExtra(EXTRA_NOTEBOOK_ID, notebookId)
                .putExtra(EXTRA_SECTION_ID, sectionId)
                .putExtra(EXTRA_SECTION_NAME, sectionName));
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySectionJournalBinding binding = ActivitySectionJournalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.sectionJournalToolbar.setTitle(getIntent().getStringExtra(EXTRA_SECTION_NAME));
        setSupportActionBar(binding.sectionJournalToolbar);
        binding.sectionJournalToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        binding.sectionJournalToolbar.setNavigationContentDescription(R.string.cd_navigate_up);
        binding.sectionJournalToolbar.setNavigationOnClickListener(v -> finish());
        InsetsUtil.applyToolbarAndBottom(binding.sectionJournalRoot, binding.sectionJournalToolbar);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.section_journal_container,
                    JournalFragment.newSectionInstance(getIntent().getStringExtra(EXTRA_NOTEBOOK_ID),
                            getIntent().getStringExtra(EXTRA_SECTION_ID))).commit();
        }
    }
}
