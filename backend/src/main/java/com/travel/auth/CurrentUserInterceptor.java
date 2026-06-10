package com.travel.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.travel.entity.User;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

@Component
public class CurrentUserInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;

    public CurrentUserInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requireAdmin = hasAnnotation(handlerMethod, RequireAdmin.class);
        boolean requireLogin = requireAdmin || hasAnnotation(handlerMethod, RequireLogin.class);

        if (requireLogin) {
            User user = tokenService.loadActiveUser(StpUtil.getLoginIdAsLong());
            request.setAttribute(CurrentUserContext.REQUEST_ATTRIBUTE, user);
            if (requireAdmin && !"ADMIN".equals(user.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无管理员权限");
            }
            return true;
        }

        if (hasCurrentUserParameter(handlerMethod)) {
            User user = tokenService.getUserByToken(request.getHeader("Authorization"));
            if (user != null) {
                request.setAttribute(CurrentUserContext.REQUEST_ATTRIBUTE, user);
            }
        }
        return true;
    }

    private boolean hasCurrentUserParameter(HandlerMethod handlerMethod) {
        for (Parameter parameter : handlerMethod.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(CurrentUser.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationType) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), annotationType)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), annotationType);
    }
}
