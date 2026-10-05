package com.ruralmarket.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 Jackson Hibernate6 模块：序列化实体时正确处理懒加载代理，
 * 避免 ByteBuddyInterceptor 序列化错误；配合 open-in-view 强制加载关联实体。
 * 本工程实体均为多对一单向引用，无双向关联，不会产生序列化循环。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        return module;
    }
}
