package com.example.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Set;


import static com.example.exercise.service.DistributedJobRunner.UNIQUE_USER_ID_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegisterService {

    private final RedissonClient redisson;

    public void registerUser(String userId) {
        Set<String> userIds = redisson.getSet(UNIQUE_USER_ID_KEY);
        userIds.add(userId);
    }
}
