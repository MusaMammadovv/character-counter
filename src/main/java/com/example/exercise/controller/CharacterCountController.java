package com.example.exercise.controller;

import com.example.exercise.model.InputModel;
import com.example.exercise.service.CharacterCounter;
import com.example.exercise.service.UserRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
public class CharacterCountController {

    private final CharacterCounter characterCounter;
    private final UserRegisterService userRegisterService;

    @PostMapping(value = "accept")
    public void accept(@RequestHeader Integer USER_ID, @RequestBody InputModel input) {
        log.debug("Accept method started for USER_ID : {}, input : {}", USER_ID, input);

        userRegisterService.registerUser(USER_ID.toString());
        characterCounter.acceptAndProcess(USER_ID.toString(), input.getInput());

        log.debug("Accept method ended for USER_ID : {}, input : {}", USER_ID, input);
    }
}
