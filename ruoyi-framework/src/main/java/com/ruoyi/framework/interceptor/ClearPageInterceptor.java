package com.ruoyi.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.github.pagehelper.PageHelper;

/**
 * 分页线程变量清理拦截器
 *
 * PageHelper 的 startPage 会把分页参数写入 ThreadLocal，若请求在中途抛出异常
 * 而没有执行查询，参数会残留在 Tomcat 的工作线程上，污染后续复用的请求。
 * 统一在请求结束时清理，避免逐个 Controller 写 try/finally。
 *
 * @author ruoyi
 */
@Component
public class ClearPageInterceptor implements HandlerInterceptor
{
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
    {
        PageHelper.clearPage();
    }
}
