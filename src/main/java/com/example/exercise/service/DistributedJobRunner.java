package com.example.exercise.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedJobRunner {

    @Value("${redis.lock.wait.seconds}")
    private Long lockWaitSeconds;
    @Value("${redis.auto.unlock.seconds}")
    private Long autoUnlockSeconds;
    @Value("${job.fixed.interval.milliSeconds}")
    private Long fixedPrintInterval;
    private final RedissonClient redisson;
    public final String DISTRIBUTED_RUNNER_MAP = "DISTRIBUTED_RUNNER_MAP";
    public static final String UNIQUE_USER_ID_KEY = "UNIQUE_USER_ID_KEY";
    public static final String USER_RESULT_TEXT = "\nResults for %s :, periodStart: %s, periodEnd: %s \n";
    private static final String INDIVIDUAL_RESULT = "\nString accepted on %s, number of G : %s";
    private final DateFormat df = new SimpleDateFormat("dd:MM:yy:HH:mm:ss");

    @Scheduled(fixedDelayString = "${job.fixed.interval.milliSeconds}",
            initialDelayString = "${job.initial.delay.milliSeconds}")
    public void printResults() {
        Set<String> userIds = redisson.getSet(UNIQUE_USER_ID_KEY);

        userIds.forEach(userId -> {
            if (isPrinted(userId)) return;
            RLock distributedLock = null;
            boolean distributedLocked = false;
            try {
                distributedLock = redisson.getFairLock(DISTRIBUTED_RUNNER_MAP+userId);
                distributedLocked = distributedLock.tryLock(lockWaitSeconds, autoUnlockSeconds, TimeUnit.SECONDS);

                if (!distributedLocked) {
                    log.error("Waited lock for DISTRIBUTED_RUNNER_MAP, got timeout");
                    return;
                }
                if (isPrinted(userId)) return;

                // if for some reason scheduler is behind current time, which is possible
                // it can be happen when application terminated, all system overloaded, or for initial delay
                while (!isPrinted(userId)) {
                    RMap<String, Long> printTimeMap = redisson.getMap(DISTRIBUTED_RUNNER_MAP);

                    Long periodStart = printTimeMap.getOrDefault(userId, System.currentTimeMillis() - fixedPrintInterval);
                    Long periodEnd = periodStart + fixedPrintInterval;

                    System.out.printf(USER_RESULT_TEXT,
                            userId, df.format(new Date(periodStart)),
                            df.format(new Date(periodEnd)));

                    RMap<Long, String> timeMap = redisson.getMap(userId);
                    Map<Long, String> timeMapC = timeMap.readAllMap();
                    for (Map.Entry<Long, String> entry : timeMapC.entrySet()) {
                        String result = entry.getValue();
                        Long logDateTime = entry.getKey();
                        if (logDateTime > periodStart && logDateTime <= periodEnd) {
                            Date currentDate = new Date(logDateTime);
                            System.out.printf(INDIVIDUAL_RESULT, df.format(currentDate), result);
                        }
                        if (logDateTime != 0 && logDateTime <= periodEnd) {
                            timeMap.fastRemove(logDateTime);
                        }


                    }

                    printTimeMap.fastPut(userId, periodEnd);
                }
            } catch (InterruptedException e) {
                log.error("Can not print results, for userId : {} exception : {}",
                        userId, e.getMessage());
            } finally {
                if (distributedLocked) distributedLock.unlock();
            }
        });
    }

    private boolean isPrinted(String userId) {
        RMap<String, Long> printTimeMap = redisson.getMap(DISTRIBUTED_RUNNER_MAP);
        Long lastPrintTime = printTimeMap.getOrDefault(userId, 0l);
        if (lastPrintTime == 0) return false;
        return lastPrintTime >= System.currentTimeMillis() - fixedPrintInterval;
    }
}
