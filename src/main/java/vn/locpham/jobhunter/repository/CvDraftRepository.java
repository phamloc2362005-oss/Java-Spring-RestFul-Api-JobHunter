package vn.locpham.jobhunter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.CvDraft;

import java.util.Optional;

@Repository
public interface CvDraftRepository extends JpaRepository<CvDraft, Long> {

    Page<CvDraft> findByUserId(Long userId, Pageable pageable);

    Optional<CvDraft> findByIdAndUserId(Long id, Long userId);
}
