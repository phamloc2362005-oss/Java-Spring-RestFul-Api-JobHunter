package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Article;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.repository.ArticleRepository;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Article handleCreateArticle(Article article) {
        return this.articleRepository.save(article);
    }

    /**
     * Lấy article theo id, KHÔNG tăng viewCount.
     * Dùng nội bộ (update, delete, check tồn tại).
     */
    public Article findById(long id) {
        return this.articleRepository.findById(id).orElse(null);
    }

    /**
     * Lấy article theo id VÀ tăng viewCount thêm 1.
     * Chỉ dùng khi người dùng public xem chi tiết bài viết.
     */
    public Article fetchArticleById(long id) {
        Article article = findById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            return this.articleRepository.save(article);
        }
        return null;
    }

    public Article handleUpdateArticle(Article reqArticle, Article articleInDB) {
        articleInDB.setTitle(reqArticle.getTitle());
        articleInDB.setDescription(reqArticle.getDescription());
        articleInDB.setContent(reqArticle.getContent());
        articleInDB.setThumbnail(reqArticle.getThumbnail());
        articleInDB.setCategory(reqArticle.getCategory());
        articleInDB.setAuthor(reqArticle.getAuthor());
        articleInDB.setFeatured(reqArticle.isFeatured());
        articleInDB.setPublished(reqArticle.isPublished());
        return this.articleRepository.save(articleInDB);
    }

    public void handleDeleteArticle(long id) {
        this.articleRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAllArticles(Specification<Article> spec, Pageable pageable) {
        Page<Article> pageArticle = this.articleRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageArticle.getTotalPages());
        mt.setTotal(pageArticle.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageArticle.getContent());
        return rs;
    }

    public List<Article> fetchFeaturedArticles() {
        return this.articleRepository.findTop6ByIsFeaturedTrueAndIsPublishedTrueOrderByCreatedAtDesc();
    }
}
