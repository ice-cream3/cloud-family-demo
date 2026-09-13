package com.kfpd.cloud.xxljobadmin.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.kfpd.cloud.xxljobadmin.base.config.XxlJobAdminProperties;
import com.kfpd.cloud.xxljobadmin.pojo.TriggerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LightweightXxlJobAdminService {

    private static final Logger log = LoggerFactory.getLogger(LightweightXxlJobAdminService.class);

    private final Map<String, Set<String>> executorAddresses = new ConcurrentHashMap<>();
    private final AtomicLong logIdGenerator = new AtomicLong(System.currentTimeMillis());
    private final XxlJobAdminProperties properties;

    public LightweightXxlJobAdminService(XxlJobAdminProperties properties) {
        this.properties = properties;
    }

    public ReturnT<String> registry(RegistryParam param) {
        if (param == null || !StringUtils.hasText(param.getRegistryKey()) || !StringUtils.hasText(param.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "registryKey and registryValue are required");
        }
        executorAddresses
                .computeIfAbsent(param.getRegistryKey(), ignored -> new ConcurrentSkipListSet<>())
                .add(param.getRegistryValue());
        log.info("XXL-JOB executor registered: group={}, appName={}, address={}",
                param.getRegistryGroup(), param.getRegistryKey(), param.getRegistryValue());
        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam param) {
        if (param == null || !StringUtils.hasText(param.getRegistryKey()) || !StringUtils.hasText(param.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "registryKey and registryValue are required");
        }
        Set<String> addresses = executorAddresses.get(param.getRegistryKey());
        if (addresses != null) {
            addresses.remove(param.getRegistryValue());
            if (addresses.isEmpty()) {
                executorAddresses.remove(param.getRegistryKey());
            }
        }
        log.info("XXL-JOB executor removed: group={}, appName={}, address={}",
                param.getRegistryGroup(), param.getRegistryKey(), param.getRegistryValue());
        return ReturnT.SUCCESS;
    }

    public ReturnT<String> callback(List<HandleCallbackParam> params) {
        if (params != null) {
            params.forEach(param -> log.info("XXL-JOB callback received: logId={}, handleCode={}, handleMsg={}",
                    param.getLogId(), param.getHandleCode(), param.getHandleMsg()));
        }
        return ReturnT.SUCCESS;
    }

    public Map<String, List<String>> registeredExecutors() {
        Map<String, List<String>> snapshot = new java.util.TreeMap<>();
        executorAddresses.forEach((appName, addresses) -> snapshot.put(appName, new ArrayList<>(addresses)));
        return snapshot;
    }

    public List<ReturnT<String>> trigger(TriggerRequest request) {
        String appName = StringUtils.hasText(request.appName()) ? request.appName() : "cloud-family-job-service";
        String handler = request.handler();
        if (!StringUtils.hasText(handler)) {
            return List.of(new ReturnT<>(ReturnT.FAIL_CODE, "handler is required"));
        }

        List<String> addresses = executorAddresses.getOrDefault(appName, Set.of())
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (addresses.isEmpty()) {
            return List.of(new ReturnT<>(ReturnT.FAIL_CODE, "No executor registered for appName: " + appName));
        }

        if (request.shardingBroadcast()) {
            List<ReturnT<String>> results = new ArrayList<>();
            for (int index = 0; index < addresses.size(); index++) {
                results.add(triggerOne(addresses.get(index), handler, request.param(), index, addresses.size()));
            }
            return results;
        }
        return List.of(triggerOne(addresses.get(0), handler, request.param(), 0, 1));
    }

    private ReturnT<String> triggerOne(String address,
                                       String handler,
                                       String param,
                                       int broadcastIndex,
                                       int broadcastTotal) {
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(Math.abs(handler.hashCode()));
        triggerParam.setExecutorHandler(handler);
        triggerParam.setExecutorParams(param);
        triggerParam.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name());
        triggerParam.setExecutorTimeout(properties.getExecutorTimeoutSeconds());
        triggerParam.setLogId(logIdGenerator.incrementAndGet());
        triggerParam.setLogDateTime(System.currentTimeMillis());
        triggerParam.setGlueType(GlueTypeEnum.BEAN.name());
        triggerParam.setGlueUpdatetime(System.currentTimeMillis());
        triggerParam.setBroadcastIndex(broadcastIndex);
        triggerParam.setBroadcastTotal(broadcastTotal);

        log.info("Trigger XXL-JOB executor: address={}, handler={}, broadcastIndex={}, broadcastTotal={}",
                address, handler, broadcastIndex, broadcastTotal);
        return new ExecutorBizClient(address, properties.getAccessToken()).run(triggerParam);
    }
}
