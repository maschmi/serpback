package de.inw.serpent.serpback.configuration;


import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.slf4j.LoggerFactory.getLogger;

import org.ehcache.CacheManager;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import static org.ehcache.config.units.MemoryUnit.MB;

@EnableCaching
@Configuration
public class CacheConfiguration {
    private static final Logger log = getLogger(CacheConfiguration.class);

    @Bean
    public CacheManager cacheManager() {
        log.debug("Creating cache manager");
        return newCacheManagerBuilder()
                .withCache("basicCache",
                        newCacheConfigurationBuilder(Long.class, String.class, heap(100).offheap(1, MB)))
                .build(true);
    }


}
