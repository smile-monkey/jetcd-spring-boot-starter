package com.technology.service;

import com.google.common.base.Charsets;
import com.technology.config.JetcdProperties;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class JetcdDistributedLock {
    private static final Logger LOGGER = LoggerFactory.getLogger(JetcdDistributedLock.class);
    /**
     * 租约默认时间
     */
    private static final int DEFAULT_GRANT_TTL = 30;
    /**
     * 默认超时时间
     */
    private static final long DEFAULT_OVERTIME_MILLISECONDS = 1000;

    private Client client;
    private Lease leaseClient;
    private Lock lockClient;
    /**
     * 获取锁之后  创建一个定时任务作为“心跳”，保证等待锁释放期间，租约不失效; 释放锁的时候注意回收
     */
    private ScheduledExecutorService heartPopService;

    private JetcdProperties jetcdProperties;

    public JetcdDistributedLock() {
    }

    public JetcdDistributedLock(JetcdProperties jetcdProperties) {
        jetcdProperties = jetcdProperties;
        client = Client.builder().endpoints(jetcdProperties.getEndpoints()).build();
        leaseClient = client.getLeaseClient();
        lockClient = client.getLockClient();
    }


    /**
     * 获取锁
     *
     * @param key
     * @param expireSecond 占有锁的最长时间  -1 表示一直持有锁(心跳) 0 使用默认值
     */
    public Long setLock(String key, long expireSecond) {
        long grantTtl = expireSecond <= 0 ? DEFAULT_GRANT_TTL : expireSecond;
        Long grantId = null;
        try {
            // 获取租约id (防止死锁)
            LeaseGrantResponse leaseGrantResponse = leaseClient.grant(grantTtl).get();
            grantId = leaseGrantResponse.getID();
            CompletableFuture<LockResponse> lockFeature = lockClient.lock(ByteSequence.from(key, Charsets.UTF_8), grantId);
            LockResponse lockResponse = null;
            try {
                lockResponse = lockFeature.get(DEFAULT_OVERTIME_MILLISECONDS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                // 这里可以考虑超时重试
                LOGGER.info("jetcd lock using : {}", key);
                throw ex;
            }
            if (lockResponse != null) {
                if (expireSecond < 0) {
                    // 启动定时任务续约，心跳周期和初次启动延时计算公式
                    long period = grantTtl / 3;
                    heartPopService = Executors.newSingleThreadScheduledExecutor();
                    Long finalGrantId = grantId;
                    heartPopService.scheduleAtFixedRate(() -> {
                        leaseClient.keepAliveOnce(finalGrantId);
                    }, period, period, TimeUnit.SECONDS);
                }
                return grantId;
            }
        } catch (Exception e) {
            LOGGER.error("jetcd setLock error", e);
            revokeGrantQuitely(grantId);
        }
        return null;
    }

    /**
     * 释放锁
     */
    public boolean releaseLock(String key, Long grantId) {
        try {
            Lock lockClient = client.getLockClient();
            CompletableFuture<UnlockResponse> unlockFeature = lockClient.unlock(ByteSequence.from(key, Charsets.UTF_8));
            UnlockResponse unlockResponse = unlockFeature.get();
            if (unlockResponse != null) {
                revokeGrantQuitely(grantId);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("jetcd releaseLock error", e);
        }
        return false;
    }

    /**
     * 静默删除租约
     *
     * @param grantId
     */
    public void revokeGrantQuitely(Long grantId) {
        if (heartPopService != null) {
            heartPopService.shutdown();
        }
        if (grantId != null && grantId != 0) {
            try {
                leaseClient.revoke(grantId).get();
            } catch (InterruptedException e1) {
                LOGGER.error("jetcd revoke grant error", e1);
            } catch (ExecutionException e1) {
                LOGGER.error("jetcd revoke grant error", e1);
            }
        }
    }

    public static void main(String[] args) {
        Client client = Client.builder().endpoints("http://192.168.2.30:2379", "http://192.168.2.31:2379").build();
        Runnable r = () -> {
            try {
                Lease leaseClient = client.getLeaseClient();
                String currentThreadName = Thread.currentThread().getName();
                // 获取租约,到期后将会移除(防止死锁)
                LeaseGrantResponse leaseGrantResponse = leaseClient.grant(10).get();
                long id = leaseGrantResponse.getID();

                System.out.println("id:" + Long.toHexString(id));
                Lock lockClient = client.getLockClient();
                CompletableFuture<LockResponse> lockFeature = lockClient.lock(ByteSequence.from("sample_name88", Charsets.UTF_8), id);
                System.out.println(currentThreadName + "尝试获取锁...");
                LockResponse lockResponse = null;
                try {
                    lockResponse = lockFeature.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    System.out.println(currentThreadName + ",锁被占用获取不到锁");
                    return;
                }
                ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> {
                    leaseClient.keepAliveOnce(id);
                }, 5, 3, TimeUnit.SECONDS);
                System.err.println(lockResponse);
                System.out.println(currentThreadName + "已获取锁..." + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss sss").format(new Date()));
                Thread.sleep(8000);
                System.out.println(currentThreadName + "正在释放锁...");
                CompletableFuture<UnlockResponse> unlockFeature = lockClient.unlock(lockResponse.getKey());
                UnlockResponse unlockResponse = unlockFeature.get();
                System.err.println(unlockResponse);
                System.out.println(currentThreadName + "释放锁成功...");
                // 删除租约
                leaseClient.revoke(id);
//                scheduledExecutorService.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(r);
        thread1.setName("thread1");
        Thread thread2 = new Thread(r);
        thread2.setName("thread2");
        Thread thread3 = new Thread(r);
        thread3.setName("thread3");
        Thread thread4 = new Thread(r);
        thread4.setName("thread4");
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

//        client.close();
    }

}
