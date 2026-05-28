package vn.locpham.jobhunter.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.Article;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.service.ArticleService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/articles")
    @ApiMessage("Create a new article")
    public ResponseEntity<Article> createArticle(@Valid @RequestBody Article reqArticle) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.articleService.handleCreateArticle(reqArticle));
    }

    @PutMapping("/articles")
    @ApiMessage("Update an article")
    public ResponseEntity<Article> updateArticle(@Valid @RequestBody Article reqArticle) throws IdInvalidException {
        Article articleInDB = this.articleService.findById(reqArticle.getId());
        if (articleInDB == null) {
            throw new IdInvalidException("Article with id = " + reqArticle.getId() + " not found");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.articleService.handleUpdateArticle(reqArticle, articleInDB));
    }

    @DeleteMapping("/articles/{id}")
    @ApiMessage("Delete an article")
    public ResponseEntity<Void> deleteArticle(@PathVariable("id") long id) throws IdInvalidException {
        Article articleInDB = this.articleService.findById(id);
        if (articleInDB == null) {
            throw new IdInvalidException("Article with id = " + id + " not found");
        }
        this.articleService.handleDeleteArticle(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/articles")
    @ApiMessage("Get all articles (paginated)")
    public ResponseEntity<ResultPaginationDTO> getAllArticles(
            @Filter Specification<Article> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.articleService.fetchAllArticles(spec, pageable));
    }

    @GetMapping("/articles/{id}")
    @ApiMessage("Get article by id")
    public ResponseEntity<Article> getArticleById(@PathVariable("id") long id) throws IdInvalidException {
        Article article = this.articleService.fetchArticleById(id);
        if (article == null) {
            throw new IdInvalidException("Article with id = " + id + " not found");
        }
        return ResponseEntity.status(HttpStatus.OK).body(article);
    }

    @GetMapping("/articles/featured")
    @ApiMessage("Get featured articles")
    public ResponseEntity<List<Article>> getFeaturedArticles() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.articleService.fetchFeaturedArticles());
    }
}
