package com.atguigu.gmall.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// 自定义注解 指定需要被拦截器拦截
public @interface LoginRequired {

    // 定义注解内属性 是否为必须 校验正确 也就是 必须为用户登录
    boolean loginSuccess() default true;
}
