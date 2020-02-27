package com.atguigu.gmall.interceptors;

import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter{

        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            // 拦截代码

            // 判断被拦截的方法是否带有 @LoginRequired 注解
            // handler 请求方法所携带的信息
            HandlerMethod hm = (HandlerMethod) handler;
            LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

            if (methodAnnotation == null){
                return true;
            }

            boolean loginSuccess = methodAnnotation.loginSuccess();
            if (loginSuccess){

            }

            //请求没有带 @LoginRequired 注解 则开始进行校验

            return true;
        }
}