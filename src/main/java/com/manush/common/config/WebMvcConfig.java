package com.manush.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /static/** maps to the whole static folder — covers /static/css/, /static/js/, etc.
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        // Convenience shorthand aliases (so /css/x.css also works)
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**", "/img/**")
                .addResourceLocations("classpath:/static/images/", "classpath:/static/img/");
        // favicon — point to the folder, not the file, to avoid trailing-slash WARN
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}
