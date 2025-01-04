package org.stream.core.sentinel;

import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Sentinel rule type.
 *
 * @author weiguanxiong
 */
public enum SentinelRuleType {

    /**
     * Degrade type.
     */
    DEGRADE("Degrade") {
        /**
         * {@inheritDoc}
         */
        @Override
        public void addRule(final SentinelConfiguration sentinelConfiguration) {
            DegradeRule degradeRule = new DegradeRule();

            degradeRule.setCount(sentinelConfiguration.getValueThreshold());
            degradeRule.setMinRequestAmount(sentinelConfiguration.getMinimalRequests());
            degradeRule.setTimeWindow(sentinelConfiguration.getTimeWindow());
            degradeRule.setSlowRatioThreshold(sentinelConfiguration.getRatioThreshold());
            degradeRule.setStatIntervalMs(sentinelConfiguration.getDuration());
            degradeRule.setResource(sentinelConfiguration.getResourceName());
            degradeRule.setGrade(sentinelConfiguration.getGrade());
            List<DegradeRule> degradeRules = Collections.singletonList(degradeRule);
            DegradeRuleManager.loadRules(degradeRules);
        }
    },

    /**
     * Flow control type.
     */
    FLOW("Flow") {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addRule(final SentinelConfiguration sentinelConfiguration) {
            FlowRule flowRule = new FlowRule();

            flowRule.setCount(sentinelConfiguration.getValueThreshold());
            flowRule.setResource(sentinelConfiguration.getResourceName());
            flowRule.setGrade(sentinelConfiguration.getGrade());
            List<FlowRule> flowRules = Collections.singletonList(flowRule);
            FlowRuleManager.loadRules(flowRules);
        }
    };

    public abstract void addRule(final SentinelConfiguration sentinelConfiguration);

    private final String type;

    SentinelRuleType(final String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }

    /**
     * Get sentinel type instance from the input type name.
     * @param type Type name.
     * @return Sentinel type instance.
     */
    public static SentinelRuleType fromType(final String type) {
        for (SentinelRuleType candidate : SentinelRuleType.values()) {
            if (Objects.equals(candidate.type, type)) {
                return candidate;
            }
        }
        throw new RuntimeException("Invalid sentinel rule type");
    }

}
