package com.example.proiectfinal.views;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proiectfinal.R;
import com.example.proiectfinal.adapters.DayPlanAdapter;
import com.example.proiectfinal.models.AiPlan;

public class AiResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_result);

        AiPlan plan = (AiPlan) getIntent().getSerializableExtra("ai_plan");
        String vacationType = getIntent().getStringExtra("vacation_type");
        int daysCount = getIntent().getIntExtra("days_count", 0);

        ImageButton btnBack = findViewById(R.id.btn_back_result);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_result_title);
        tvTitle.setText("Plan " + vacationType + " — " + daysCount + " zile");

        // Sfaturile generale
        TextView tvGeneralTips = findViewById(R.id.tv_general_tips);
        if (plan != null && plan.getGeneralTips() != null) {
            tvGeneralTips.setText("✨ " + plan.getGeneralTips());
        }

        // RecyclerView cu zilele
        RecyclerView recycler = findViewById(R.id.recycler_days);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        if (plan != null && plan.getDays() != null) {
            DayPlanAdapter adapter = new DayPlanAdapter(this, plan.getDays());
            recycler.setAdapter(adapter);
        }
    }
}