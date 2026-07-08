package com.study;


import com.study.model.DetectRequest;
import com.study.model.DetectResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DetectController {

    @Autowired
    private DetectService detectService;

    @PostMapping("/detect")
    public DetectResponse detect(@RequestBody DetectRequest request) {
        return detectService.detect(request);
    }
}