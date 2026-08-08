package com.codesa.core.repository;

import com.codesa.core.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOwnerId(Long ownerId);
}
