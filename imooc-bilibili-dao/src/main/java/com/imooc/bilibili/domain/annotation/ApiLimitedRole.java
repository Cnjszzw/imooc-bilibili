package com.imooc.bilibili.domain.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Component
public @interface ApiLimitedRole {

    //表示该接口权限不足的等级列表
    String[] limitedRoleCodeList() default {};
}
