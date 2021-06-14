package com.ververica.statefun;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.GenericContainer;

/**
 * A simple wrapper for a {@link GenericContainer} to hook into the Spring lifecycle.
 *
 * @param <C> The container type.
 */
public class ContainerBean<C extends GenericContainer<C>> implements InitializingBean, DisposableBean {
    final C container;

    public ContainerBean(C container) {
        this.container = container;
    }

    public C getContainer() {
        return container;
    }

    @Override
    public void afterPropertiesSet() {
        container.start();
    }

    @Override
    public void destroy() {
        container.close();
    }
}
