package vn.locpham.jobhunter.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.locpham.jobhunter.domain.Review;
import vn.locpham.jobhunter.domain.ReviewVote;
import vn.locpham.jobhunter.domain.User;

@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {

    Optional<ReviewVote> findByUserAndReview(User user, Review review);
}
