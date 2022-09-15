package com.example.exercise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterCounter {
    @Value("${redis.lock.wait.seconds}")
    private Long lockWaitSeconds;
    @Value("${redis.auto.unlock.seconds}")
    private Long autoUnlockSeconds;
    private final RedissonClient redisson;

    public boolean acceptAndProcess(String userId, String input) {
        RLock lock = redisson.getFairLock("LOCK_" + userId);

        boolean locked = false;
        try {
            locked = lock.tryLock(lockWaitSeconds, autoUnlockSeconds, TimeUnit.SECONDS);
            if (locked) {
                RMap<Long, String> rMap = redisson.getMap(userId);
                int inputLength = input.length();
                while (inputLength != 0) {
                    StringBuffer currentBuffer = new StringBuffer(
                            rMap.getOrDefault(0l, ""));
                    int charsToAdd = Math.min(512 - currentBuffer.length(), inputLength);
                    currentBuffer.append(input, 0, charsToAdd);
                    rMap.fastPut(0L, currentBuffer.toString());
                    if (currentBuffer.length() == 512) {
                        timespanAndSave(rMap);
                    }
                    inputLength -= charsToAdd;
                    input=input.substring(charsToAdd);
                }
            }
            return true;
        } catch (InterruptedException e) {
            log.error("Error occurred while processing request for userId : {}, exception : {}"
                    ,userId, e.getMessage());
            return false;
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("Can not release lock for userId : {}, exception : {}",
                            userId, e.getMessage());
                }
            }
        }
    }

    private void timespanAndSave(RMap<Long, String> map) {
        String current = map.get(0L);
        map.put(System.currentTimeMillis(), countGs(current).toString());
        map.fastRemove(0L);
    }

    private Integer countGs(String input) {
        int answer = 0;
        for(int i = 0; i < input.length(); ++i) {
            if (input.charAt(i) == 'G') ++answer;
        }
        return answer;
    }
}
