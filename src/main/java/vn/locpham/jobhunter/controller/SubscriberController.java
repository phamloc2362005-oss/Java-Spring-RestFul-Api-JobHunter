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

    @PostMapping("/subscribers")
    @ApiMessage("Create a new subscriber")
    public ResponseEntity<Subscriber> createNewSubscriber(@Valid @RequestBody Subscriber postmanSubscriber)
            throws IdInvalidException {
        boolean isEmailExist = this.subscriberService.existsByEmail(postmanSubscriber.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postmanSubscriber.getEmail() + "đã tồn tại, vui lòng sử dụng email khác");
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
