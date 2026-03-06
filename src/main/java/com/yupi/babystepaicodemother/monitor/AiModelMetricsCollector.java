package com.yupi.babystepaicodemother.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 鎸囨爣鏀堕泦鍣?
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    // 缂撳瓨宸插垱寤虹殑鎸囨爣锛岄伩鍏嶉噸澶嶅垱寤猴紙鎸夋寚鏍囩被鍨嬪垎绂荤紦瀛橈級
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 璁板綍璇锋眰娆℃暟
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, status);
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total")
                        .description("AI妯″瀷鎬昏姹傛鏁?")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("status", status)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 璁板綍閿欒
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, errorMessage);
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI妯″瀷閿欒娆℃暟")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 璁板綍Token娑堣€?
     */
    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, tokenType);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI妯″瀷Token娑堣€楁€绘暟")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 璁板綍鍝嶅簲鏃堕棿
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        String key = String.format("%s_%s_%s", userId, appId, modelName);
        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI妯″瀷鍝嶅簲鏃堕棿")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .register(meterRegistry)
        );
        timer.record(duration);
    }
}
