package com.example.slabiak.appointmentscheduler.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class VersionInterceptor implements HandlerInterceptor {

    private final BuildProperties buildProperties;

    public VersionInterceptor(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
                           final ModelAndView modelAndView) {

        if (modelAndView != null && !isRedirect(modelAndView)) {
            modelAndView.getModelMap().addAttribute("currentVersion", buildProperties.getVersion());
        }
    }

    private boolean isRedirect(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        return viewName != null && viewName.startsWith("redirect:");
    }
}
