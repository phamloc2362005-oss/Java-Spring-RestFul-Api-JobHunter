package vn.locpham.jobhunter.domain.request;

public class ReqCreateReviewDTO {
    private int rating;
    private boolean isRecommend;
    private String content;
    private String title;
    private String pros;
    private String cons;
    private long companyId;

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public boolean isRecommend() {
        return isRecommend;
    }

    public void setRecommend(boolean isRecommend) {
        this.isRecommend = isRecommend;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPros() {
        return pros;
    }

    public void setPros(String pros) {
        this.pros = pros;
    }

    public String getCons() {
        return cons;
    }

    public void setCons(String cons) {
        this.cons = cons;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }
}
