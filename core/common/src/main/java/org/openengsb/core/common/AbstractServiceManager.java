/**
 * Copyright 2010 OpenEngSB Division, Vienna University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.common;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.openengsb.core.common.descriptor.ServiceDescriptor;
import org.openengsb.core.common.util.BundleStrings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.context.BundleContextAware;

/**
 * Base class for {@link ServiceManager} implementations. Handles all OSGi
 * related stuff and exporting the right service properties that are needed for
 * service discovery.
 *
 * All service-specific action, like descriptor building, service instantiation
 * and service updating are encapsulated in a {@link ServiceInstanceFactory}.
 * Creating a new service manager should be as simple as implementing the
 * {@link ServiceInstanceFactory} and creating a subclass of this class:
 *
 * <pre>
 * public class ExampleServiceManager extends AbstractServiceManager&lt;ExampleDomain, TheInstanceType&gt; {
 *     public ExampleServiceManager(ServiceInstanceFactory&lt;ExampleDomain, TheInstanceType&gt; factory) {
 *         super(factory);
 *     }
 * }
 * </pre>
 *
 * @param <DomainType> interface of the domain this service manages
 * @param <InstanceType> actual service implementation this service manages
 */
public abstract class AbstractServiceManager<DomainType extends Domain, InstanceType extends DomainType> implements
        ServiceManager, BundleContextAware {

    private class DomainRepresentation {
        private final InstanceType service;
        private final ServiceRegistration registration;

        public DomainRepresentation(InstanceType service, ServiceRegistration registration) {
            this.service = service;
            this.registration = registration;
        }
    }

    private BundleContext bundleContext;
    private BundleStrings strings;
    private final Map<String, DomainRepresentation> services = new HashMap<String, DomainRepresentation>();
    private final ServiceInstanceFactory<DomainType, InstanceType> factory;
    private final Map<String, Map<String,String>> attributeValues = new HashMap<String, Map<String, String>>();

    public AbstractServiceManager(ServiceInstanceFactory<DomainType, InstanceType> factory) {
        this.factory = factory;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        strings = new BundleStrings(bundleContext.getBundle());
    }

    @Override
    public ServiceDescriptor getDescriptor() {
        return getDescriptor(Locale.getDefault());
    }

    @Override
    public ServiceDescriptor getDescriptor(Locale locale) {
        return factory.getDescriptor(ServiceDescriptor.builder(locale, strings).id(getImplementationClass().getName())
                .serviceType(getDomainInterface()).implementationType(getImplementationClass()));
    }

    @Override
    public void update(String id, Map<String, String> attributes) {
        synchronized (services) {
            if (!services.containsKey(id)) {
                InstanceType instance = factory.createServiceInstance(id, attributes);
                Hashtable<String, String> serviceProperties = createNotificationServiceProperties(id);
                ServiceRegistration registration = bundleContext.registerService(new String[] {
                        getImplementationClass().getName(), getDomainInterface().getName(), Domain.class.getName() },
                        instance, serviceProperties);
                services.put(id, new DomainRepresentation(instance, registration));
            } else {
                factory.updateServiceInstance(services.get(id).service, attributes);
            }
            if (attributeValues.containsKey(id)) {
                attributeValues.get(id).putAll(attributes);
            } else {
                attributeValues.put(id, attributes);
            }

        }
    }

    @Override
    public void delete(String id) {
        synchronized (services) {
            services.get(id).registration.unregister();
            services.remove(id);
            attributeValues.remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    protected Class<DomainType> getDomainInterface() {
        return (Class<DomainType>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @SuppressWarnings("unchecked")
    protected Class<InstanceType> getImplementationClass() {
        return (Class<InstanceType>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    private Hashtable<String, String> createNotificationServiceProperties(String id) {
        Hashtable<String, String> serviceProperties = new Hashtable<String, String>();
        serviceProperties.put("id", id);
        serviceProperties.put("domain", getDomainInterface().getName());
        serviceProperties.put("class", getImplementationClass().getName());
        serviceProperties.put("managerId", getDescriptor().getId());
        return serviceProperties;
    }

    @Override
    public Map<String, String> getAttributeValues(String id) {
        Map<String, String> returnValues = new HashMap<String, String>();
        synchronized (attributeValues) {
            if (attributeValues.containsKey(id)) {
                Map<String, String> attributes = attributeValues.get(id);
                returnValues.putAll(attributes);
            } else {
                throw new IllegalArgumentException("the specified service instance does not exist");
            }
        }
        return returnValues;
    }
}