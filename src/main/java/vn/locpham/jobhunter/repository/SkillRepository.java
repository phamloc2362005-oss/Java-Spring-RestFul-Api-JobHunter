package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.locpham.jobhunter.domain.Skill;

public interface SkillRepository extends JpaRepository<Skill, Long>, JpaSpecificationExecutor<Skill> {
    Page<Skill> findAll(Specification<Skill> spec, Pageable pageable);

    boolean existsByName(String name);

    List<Skill> findByIdIn(List<Long> id);

    @Query(value = "SELECT s.name, COUNT(js.job_id) as cnt FROM skills s " +
            "JOIN job_skill js ON s.id = js.skill_id " +
            "GROUP BY s.id, s.name ORDER BY cnt DESC LIMIT 6", nativeQuery = true)
    List<Object[]> findTopSkills();
}
