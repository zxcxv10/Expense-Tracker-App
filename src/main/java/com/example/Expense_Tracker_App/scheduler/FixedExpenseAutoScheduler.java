package com.example.Expense_Tracker_App.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.Expense_Tracker_App.entity.FixedExpenseAutoSetting;
import com.example.Expense_Tracker_App.repository.FixedExpenseAutoSettingRepository;
import com.example.Expense_Tracker_App.service.FixedExpenseAutoService;

@Component
public class FixedExpenseAutoScheduler {

    private final FixedExpenseAutoSettingRepository settingRepository;
    private final FixedExpenseAutoService autoService;

    public FixedExpenseAutoScheduler(
            FixedExpenseAutoSettingRepository settingRepository,
            FixedExpenseAutoService autoService
    ) {
        this.settingRepository = settingRepository;
        this.autoService = autoService;
    }

    // 매달 1일 00:05 실행
    @Scheduled(cron = "0 5 0 1 * *")
    public void runMonthly() {
        LocalDate now = LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();

        List<FixedExpenseAutoSetting> enabledUsers = settingRepository.findByEnabled(Boolean.TRUE);
        for (FixedExpenseAutoSetting s : enabledUsers) {
            if (s == null) {
                continue;
            }
            String username = s.getUsername();
            if (username == null || username.trim().isEmpty()) {
                continue;
            }
            try {
                autoService.generateForUser(username, y, m, true);
            } catch (Exception ignore) {
                // 스케줄러는 개별 사용자 실패로 전체 실패하면 안됨
            }
        }
    }
}
