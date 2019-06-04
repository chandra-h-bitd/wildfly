/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.hotrod.sso;

import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.cache.tx.TransactionBatch;
import org.wildfly.clustering.infinispan.client.InfinispanClientRequirement;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOManagerFactoryServiceConfigurator<A, D, S> extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, HotRodSSOManagerFactoryConfiguration {

    private final String name;
    private final HotRodSSOManagementConfiguration config;

    private volatile SupplierDependency<RemoteCacheContainer> container;

    public HotRodSSOManagerFactoryServiceConfigurator(HotRodSSOManagementConfiguration config, String name) {
        super(ServiceName.JBOSS.append("clustering", "sso", name));

        this.name = name;
        this.config = config;
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.container = new ServiceSupplierDependency<>(InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(support, this.config.getContainerName()));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<SSOManagerFactory<A, D, S, TransactionBatch>> factory = this.container.register(builder).provides(this.getServiceName());
        Service service = Service.newInstance(factory, new HotRodSSOManagerFactory<>(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public <K, V> RemoteCache<K, V> getRemoteCache() {
        return this.container.get().administration().getOrCreateCache(this.name, this.config.getConfigurationName());
    }
}