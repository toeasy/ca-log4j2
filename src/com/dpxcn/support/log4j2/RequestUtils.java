package com.dpxcn.support.log4j2;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 非Servlet环境获取request对象
 * Created by liang
 * on 2017/12/21.
 */
public class RequestUtils {

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static String getHeader(String key) {
        HttpServletRequest request = getRequest();
        return request.getHeader(key);

    }


}
