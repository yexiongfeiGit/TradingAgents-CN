package com.tradingagents.workflow;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工作流条件
 * 定义工作流执行的条件逻辑
 */
@Data
@Slf4j
public class WorkflowCondition {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowCondition.class);
    
    private String conditionType;
    private String parameter;
    private String operator;
    private Object value;
    private boolean isMet;
    
    public WorkflowCondition(String conditionType, String parameter, String operator, Object value) {
        this.conditionType = conditionType;
        this.parameter = parameter;
        this.operator = operator;
        this.value = value;
        this.isMet = false;
    }
    
    /**
     * 评估条件
     */
    public boolean evaluate(Object contextValue) {
        try {
            boolean result = false;
            
            switch (operator) {
                case "equals":
                    result = contextValue != null && contextValue.equals(value);
                    break;
                case "not_equals":
                    result = contextValue == null || !contextValue.equals(value);
                    break;
                case "greater_than":
                    result = compareNumbers(contextValue, value) > 0;
                    break;
                case "less_than":
                    result = compareNumbers(contextValue, value) < 0;
                    break;
                case "greater_equals":
                    result = compareNumbers(contextValue, value) >= 0;
                    break;
                case "less_equals":
                    result = compareNumbers(contextValue, value) <= 0;
                    break;
                case "contains":
                    result = containsString(contextValue, value);
                    break;
                case "not_contains":
                    result = !containsString(contextValue, value);
                    break;
                case "is_empty":
                    result = isEmpty(contextValue);
                    break;
                case "not_empty":
                    result = !isEmpty(contextValue);
                    break;
                case "is_true":
                    result = isTrue(contextValue);
                    break;
                case "is_false":
                    result = !isTrue(contextValue);
                    break;
                default:
                    log.warn("未知的操作符: {}", operator);
                    result = false;
            }
            
            this.isMet = result;
            log.debug("评估条件 - 类型: {}, 参数: {}, 操作符: {}, 期望值: {}, 实际值: {}, 结果: {}",
                    conditionType, parameter, operator, value, contextValue, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("评估条件时出错", e);
            return false;
        }
    }
    
    /**
     * 比较数字
     */
    private int compareNumbers(Object value1, Object value2) {
        try {
            double num1 = convertToDouble(value1);
            double num2 = convertToDouble(value2);
            return Double.compare(num1, num2);
        } catch (Exception e) {
            log.error("数字比较失败", e);
            return 0;
        }
    }
    
    /**
     * 转换为双精度数字
     */
    private double convertToDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else {
            throw new IllegalArgumentException("无法转换为数字: " + value);
        }
    }
    
    /**
     * 检查字符串包含
     */
    private boolean containsString(Object contextValue, Object searchValue) {
        if (contextValue == null || searchValue == null) {
            return false;
        }
        String contextStr = contextValue.toString().toLowerCase();
        String searchStr = searchValue.toString().toLowerCase();
        return contextStr.contains(searchStr);
    }
    
    /**
     * 检查是否为空
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof java.util.Collection) {
            return ((java.util.Collection<?>) value).isEmpty();
        }
        return false;
    }
    
    /**
     * 检查是否为真
     */
    private boolean isTrue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value) || "1".equals(value);
        }
        return false;
    }
    
    /**
     * 获取条件描述
     */
    public String getDescription() {
        return String.format("%s %s %s", parameter, operator, value);
    }
    
    /**
     * 创建简单的等于条件
     */
    public static WorkflowCondition equals(String parameter, Object value) {
        return new WorkflowCondition("simple", parameter, "equals", value);
    }
    
    /**
     * 创建大于条件
     */
    public static WorkflowCondition greaterThan(String parameter, Number value) {
        return new WorkflowCondition("numeric", parameter, "greater_than", value);
    }
    
    /**
     * 创建小于条件
     */
    public static WorkflowCondition lessThan(String parameter, Number value) {
        return new WorkflowCondition("numeric", parameter, "less_than", value);
    }
    
    /**
     * 创建包含条件
     */
    public static WorkflowCondition contains(String parameter, String value) {
        return new WorkflowCondition("string", parameter, "contains", value);
    }
    
    /**
     * 创建不为空条件
     */
    public static WorkflowCondition notEmpty(String parameter) {
        return new WorkflowCondition("validation", parameter, "not_empty", null);
    }
    
    /**
     * 获取参数
     */
    public String getParameter() {
        return parameter;
    }
}