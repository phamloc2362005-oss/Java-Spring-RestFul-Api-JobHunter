package vn.locpham.jobhunter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@Service
public class FavoriteJobService {
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final UserService userService;

    public FavoriteJobService(UserRepository userRepository, JobRepository jobRepository, UserService userService) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.userService = userService;
    }

    @Transactional
    public List<Job> getFavoriteJobs(String email) throws IdInvalidException {
        User user = requireUser(email);
        List<Job> favorites = user.getFavoriteJobs();
        return favorites == null ? List.of() : favorites;
    }

    @Transactional
    public boolean isFavorite(String email, Long jobId) throws IdInvalidException {
        User user = requireUser(email);
        Job job = requireJob(jobId);
        List<Job> favorites = user.getFavoriteJobs();
        if (favorites == null) {
            return false;
        }
        return favorites.stream().anyMatch(item -> item.getId() == job.getId());
    }

    @Transactional
    public boolean addFavorite(String email, Long jobId) throws IdInvalidException {
        User user = requireUser(email);
        Job job = requireJob(jobId);
        List<Job> favorites = user.getFavoriteJobs();
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        boolean exists = favorites.stream().anyMatch(item -> item.getId() == job.getId());
        if (!exists) {
            favorites.add(job);
            user.setFavoriteJobs(favorites);
            this.userRepository.save(user);
        }
        return true;
    }

    @Transactional
    public boolean removeFavorite(String email, Long jobId) throws IdInvalidException {
        User user = requireUser(email);
        Job job = requireJob(jobId);
        List<Job> favorites = user.getFavoriteJobs();
        if (favorites == null || favorites.isEmpty()) {
            return false;
        }
        boolean removed = favorites.removeIf(item -> item.getId() == job.getId());
        if (removed) {
            user.setFavoriteJobs(favorites);
            this.userRepository.save(user);
        }
        return removed;
    }

    private User requireUser(String email) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }
        return user;
    }

    private Job requireJob(Long jobId) throws IdInvalidException {
        Optional<Job> job = this.jobRepository.findById(jobId);
        if (job.isEmpty()) {
            throw new IdInvalidException("Job với id = " + jobId + " không tồn tại");
        }
        return job.get();
    }
}
