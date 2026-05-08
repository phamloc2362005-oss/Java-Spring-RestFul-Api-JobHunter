package vn.locpham.jobhunter.domain;

public class JobWithScore {
    private final Job job;
    private final int score;
    private final String matchSummary;

    public JobWithScore(Job job, int score) {
        this(job, score, generateMatchSummary(score));
    }

    public JobWithScore(Job job, int score, String matchSummary) {
        this.job = job;
        this.score = score;
        this.matchSummary = matchSummary;
    }

    public Job getJob() {
        return job;
    }

    public int getScore() {
        return score;
    }

    public String getMatchSummary() {
        return matchSummary;
    }

    private static String generateMatchSummary(int score) {
        if (score >= 80) {
            return "Perfect match! ✓";
        } else if (score >= 60) {
            return "Great match ✓";
        } else if (score >= 40) {
            return "Good match";
        } else if (score >= 20) {
            return "Possible match";
        } else {
            return "Low match";
        }
    }
}
