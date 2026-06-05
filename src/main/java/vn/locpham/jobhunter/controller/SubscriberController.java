package vn.locpham.jobhunter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.Subscriber;
import vn.locpham.jobhunter.service.SubscriberService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    /**
     * POST /api/v1/subscribers/skills
     * Lấy thông tin subscriber (kèm skills) của user đang đăng nhập.
     * FE dùng để kiểm tra xem user đã subscribe chưa khi mở tab Job Alerts.
     */
    @PostMapping("/subscribers/skills")
    @ApiMessage("Get subscriber skills by current user")
    public ResponseEntity<Subscriber> getSubscriberSkills() {
        Subscriber subscriber = this.subscriberService.getSubscriberByCurrentUser();
        return ResponseEntity.ok(subscriber);
    }

    /**
     * POST /api/v1/subscribers
     * Tạo subscriber mới. Nếu email đã tồn tại thì UPDATE skills thay vì báo lỗi (upsert).
     */
    @PostMapping("/subscribers")
    @ApiMessage("Create a new subscriber")
    public ResponseEntity<Subscriber> createNewSubscriber(@Valid @RequestBody Subscriber postmanSubscriber)
            throws IdInvalidException {
        boolean isEmailExist = this.subscriberService.existsByEmail(postmanSubscriber.getEmail());
        if (isEmailExist) {
            // Upsert: email đã tồn tại → update skills thay vì báo lỗi
            Subscriber existingSubs = this.subscriberService.getSubscriberByEmail(postmanSubscriber.getEmail());
            Subscriber updated = this.subscriberService.updateSubscriber(existingSubs, postmanSubscriber);
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.subscriberService.createSubscriber(postmanSubscriber));
    }

    @PutMapping("/subscribers")
    @ApiMessage("Update a subscriber")
    public ResponseEntity<Subscriber> updateSubscriber(@RequestBody Subscriber postmanSubscriber)
            throws IdInvalidException {
        Subscriber subsDB = this.subscriberService.findById(postmanSubscriber.getId());
        if (subsDB == null) {
            throw new IdInvalidException("Không tìm thấy subscriber với id: " + postmanSubscriber.getId());
        }
        return ResponseEntity.ok(this.subscriberService.updateSubscriber(subsDB, postmanSubscriber));
    }
}
