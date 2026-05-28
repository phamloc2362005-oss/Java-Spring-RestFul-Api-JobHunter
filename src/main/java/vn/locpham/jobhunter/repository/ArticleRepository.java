package vn.locpham.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.locpham.jobhunter.domain.Article;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    List<Article> findTop6ByIsFeaturedTrueAndIsPublishedTrueOrderByCreatedAtDesc();
}
