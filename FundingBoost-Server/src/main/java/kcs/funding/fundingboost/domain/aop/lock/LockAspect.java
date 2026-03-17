package kcs.funding.fundingboost.domain.aop.lock;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class LockAspect {

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final @Qualifier("lockTemplate") RedisTemplate<String, String> lockTemplate;

    public LockAspect(@Qualifier("lockTemplate") RedisTemplate<String, String> lockTemplate) {
        this.lockTemplate = lockTemplate;
    }

    @Around("@annotation(redisLock)")
    public Object execute(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        log.info("lock 획득 시도");
        String lockKey = resolveLockKey(joinPoint, redisLock);
        String lockValue = UUID.randomUUID().toString();
        long expire = redisLock.expire();
        long waitTime = redisLock.waitTime();
        long retryInterval = redisLock.retryInterval();

        long startTime = System.currentTimeMillis();
        boolean acquired = false;

        while (!acquired && (System.currentTimeMillis() - startTime) < waitTime) {
            acquired = Boolean.TRUE.equals(
                    lockTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expire, TimeUnit.MILLISECONDS));
            if (!acquired) {
                Thread.sleep(retryInterval); // 잠시 대기 후 재시도
            } else {
                log.info("lock 획득");
            }
        }

        if (acquired) {
            try {
                return joinPoint.proceed(); // 실제 메서드 실행
            } finally {
                releaseLockSafely(lockKey, lockValue);
            }
        } else {
            throw new RuntimeException("Unable to acquire lock for key: " + lockKey);
        }
    }

    private String resolveLockKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
        String baseKey = redisLock.key();
        int argIndex = redisLock.argIndex();
        if (argIndex < 0) {
            return baseKey;
        }

        Object[] args = joinPoint.getArgs();
        if (argIndex >= args.length) {
            throw new IllegalArgumentException("Invalid redis lock argIndex: " + argIndex);
        }

        Object keyPart = args[argIndex];
        if (keyPart == null) {
            throw new IllegalArgumentException("Redis lock key part must not be null");
        }
        return baseKey + ":" + keyPart;
    }

    private void releaseLockSafely(String lockKey, String lockValue) {
        Long released = lockTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
        if (Long.valueOf(1L).equals(released)) {
            log.info("lock 반납");
            return;
        }
        log.info("lock 반납 스킵 (소유권 변경 가능성): key={}", lockKey);
    }
}
