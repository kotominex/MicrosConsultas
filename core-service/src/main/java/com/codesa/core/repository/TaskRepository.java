package com.codesa.core.repository;

import com.codesa.core.model.Task;
import com.codesa.core.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProjectId(Long projectId);

    List<Task> findAllByProjectOwnerId(Long ownerId);

    @Modifying
    @Query("UPDATE Task t SET t.status = com.codesa.core.model.TaskStatus.OVERDUE, t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.status NOT IN (com.codesa.core.model.TaskStatus.DONE, com.codesa.core.model.TaskStatus.OVERDUE) " +
            "AND t.dueDate < :today")
    int markOverdue(@Param("today") LocalDate today);
}
