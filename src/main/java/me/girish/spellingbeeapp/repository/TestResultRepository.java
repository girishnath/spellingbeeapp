package me.girish.spellingbeeapp.repository;

import me.girish.spellingbeeapp.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByStudentId(String studentId);
}
