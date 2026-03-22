package com.campus.trade.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 信用分变更注解容器
 * 用于在同一个方法上标注多个 CreditChange 注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreditChanges {

    /**
     * 信用分变更注解数组
     * @return 注解数组
     */
    CreditChange[] value();
}
