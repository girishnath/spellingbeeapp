package me.girish.spellingbeeapp.repository;

import me.girish.spellingbeeapp.model.Mistake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MistakeRepository extends JpaRepository<Mistake, Long> {
    List<Mistake> findByStudentId(String studentId);
}
