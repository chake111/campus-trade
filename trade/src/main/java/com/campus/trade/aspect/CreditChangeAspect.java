package com.campus.trade.aspect;

import com.campus.trade.annotation.CreditChange;
import com.campus.trade.annotation.CreditChanges;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.OrderStatus;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.CreditService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 信用分变更切面
 * 自动处理订单状态变更时的信用分调整
 */
@Aspect
@Component
public class CreditChangeAspect {

    private final CreditService creditService;
    private final OrderMapper orderMapper;

    public CreditChangeAspect(CreditService creditService, OrderMapper orderMapper) {
        this.creditService = creditService;
        this.orderMapper = orderMapper;
    }

    /**
     * 定义切点：匹配所有使用 @CreditChange 或 @CreditChanges 注解的方法
     */
    @Pointcut("@annotation(com.campus.trade.annotation.CreditChange) || " +
              "@annotation(com.campus.trade.annotation.CreditChanges)")
    public void creditChangePointcut() {
    }

    /**
     * 后置通知：在方法执行后处理信用分变更
     * @param joinPoint 连接点
     * @param result 方法返回值
     */
    @AfterReturning(pointcut = "creditChangePointcut()", returning = "result", argNames = "joinPoint,result")
    public void handleCreditChange(JoinPoint joinPoint, Object result) {
        // 获取方法上的注解（可能是单个或容器）
        CreditChange[] creditChanges = getCreditChangeAnnotations(joinPoint);
        if (creditChanges == null || creditChanges.length == 0) {
            return;
        }

        // 检查方法执行是否成功（只处理返回 true 的情况）
        if (result instanceof Boolean && !(Boolean) result) {
            return;
        }

        // 从方法参数中提取 orderId（第一个参数）
        Long orderId = extractOrderIdFromArgs(joinPoint.getArgs());
        if (orderId == null) {
            return;
        }

        // 获取新状态（第二个参数）
        OrderStatus newStatus = extractNewStatusFromArgs(joinPoint.getArgs());
        if (newStatus == null) {
            return;
        }

        // 根据 orderId 查询订单信息
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            System.err.println("AOP 处理信用分失败：订单不存在 ID=" + orderId);
            return;
        }

        // 遍历所有注解，找到匹配的并执行
        for (CreditChange creditChange : creditChanges) {
            if (shouldTriggerCreditChange(creditChange, newStatus)) {
                executeCreditChange(order, creditChange);
            }
        }
    }

    /**
     * 获取方法上的 CreditChange 注解数组（支持单个和容器）
     */
    private CreditChange[] getCreditChangeAnnotations(JoinPoint joinPoint) {
        try {
            java.lang.reflect.Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
            
            // 先尝试获取容器注解
            CreditChanges container = method.getAnnotation(CreditChanges.class);
            if (container != null) {
                return container.value();
            }
            
            // 再尝试获取单个注解
            CreditChange single = method.getAnnotation(CreditChange.class);
            if (single != null) {
                return new CreditChange[]{single};
            }
            
            return new CreditChange[0];
        } catch (Exception e) {
            return new CreditChange[0];
        }
    }

    /**
     * 从方法参数中提取订单 ID
     */
    private Long extractOrderIdFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // 查找 Long 类型的参数（假设第一个 Long 类型是 orderId）
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }

    /**
     * 从方法参数中提取新状态
     */
    private OrderStatus extractNewStatusFromArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // 查找 OrderStatus 类型的参数
        for (Object arg : args) {
            if (arg instanceof OrderStatus) {
                return (OrderStatus) arg;
            }
        }

        return null;
    }

    /**
     * 判断是否应该触发信用分变更
     */
    private boolean shouldTriggerCreditChange(CreditChange annotation, OrderStatus currentStatus) {
        OrderStatus[] targetStatuses = annotation.onStatus();
        
        // 如果没有指定状态，默认触发
        if (targetStatuses == null || targetStatuses.length == 0) {
            return true;
        }

        // 检查当前状态是否在目标状态列表中
        return Arrays.asList(targetStatuses).contains(currentStatus);
    }

    /**
     * 执行信用分变更
     */
    private void executeCreditChange(Order order, CreditChange annotation) {
        try {
            int score = annotation.value();
            String reason = buildReason(order, annotation);

            if (score > 0) {
                // 增加信用分（传入 orderId）
                creditService.addCredit(order.getUserId(), score, reason, order.getId());
            } else {
                // 减少信用分（传入 orderId）
                creditService.reduceCredit(order.getUserId(), Math.abs(score), reason, order.getId());
            }
        } catch (Exception e) {
            // 信用分记录失败不影响主流程，只记录日志
            System.err.println("AOP 自动处理信用分失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 构建信用分变更原因
     */
    private String buildReason(Order order, CreditChange annotation) {
        String reasonTemplate = annotation.reason();
        
        if (reasonTemplate == null || reasonTemplate.isEmpty()) {
            return "订单状态变更";
        }

        // 替换占位符 {orderId}
        String result = reasonTemplate.replace("{orderId}", String.valueOf(order.getId()));
        
        // 替换占位符 {userId}
        result = result.replace("{userId}", String.valueOf(order.getUserId()));
        
        return result;
    }
}
