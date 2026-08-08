package com.codesa.core.scheduler;

import com.codesa.core.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class OverdueTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueTaskScheduler.class);

    private final TaskRepository taskRepository;

    public OverdueTaskScheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "${scheduler.overdue.cron}")
    @Transactional
    public void markOverdueTasks() {
        int updated = taskRepository.markOverdue(LocalDate.now());
        log.info("Tareas marcadas como OVERDUE: {}", updated);
    }
}
