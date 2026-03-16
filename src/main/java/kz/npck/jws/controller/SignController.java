package kz.npck.jws.controller;

import kz.npck.jws.model.SignRequest;
import kz.npck.jws.model.SignResponse;
import kz.npck.jws.service.SignService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sign")
public class SignController {

    private final SignService signService;

    public SignController(SignService signService) {
        this.signService = signService;
    }

    @PostMapping
    public SignResponse sign(@RequestBody SignRequest request) throws Exception {
        return signService.signText(request.getText());
    }

}