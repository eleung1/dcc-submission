package org.icgc.dcc.http.jersey;

import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.HttpHandlerProvider;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class JerseyModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ResourceConfig.class).toInstance(new ResourceConfig());
    Multibinder<HttpHandlerProvider> uriBinder = Multibinder.newSetBinder(binder(), HttpHandlerProvider.class);
    uriBinder.addBinding().to(JerseyHandler.class);
    install(new InjectModule());
  }
}
