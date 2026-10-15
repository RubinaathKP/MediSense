package com.medisense.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainer instance providing Redis 7 cache server for integration tests.
 */
public class RedisTestContainer extends GenericContainer<RedisTestContainer> {

    private static final Logger log = LoggerFactory.getLogger(RedisTestContainer.class);
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final int REDIS_PORT = 6379;

    private static RedisTestContainer container;

    private RedisTestContainer() {
        super(REDIS_IMAGE);
        withExposedPorts(REDIS_PORT);
        withReuse(true);
    }

    public static synchronized RedisTestContainer getInstance() {
        if (container == null) {
            container = new RedisTestContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        Integer mappedPort = getMappedPort(REDIS_PORT);
        log.info("RedisTestContainer started at: {}:{}", getHost(), mappedPort);
        System.setProperty("REDIS_TEST_HOST", getHost());
        System.setProperty("REDIS_TEST_PORT", String.valueOf(mappedPort));
    }

    public int getRedisPort() {
        return getMappedPort(REDIS_PORT);
    }
}
