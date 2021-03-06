/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkState;

import org.locationtech.geogig.di.GeogigModule;
import org.locationtech.geogig.di.HintsModule;
import org.locationtech.geogig.di.PluginsModule;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.impl.ContextBuilder;
import org.locationtech.geogig.storage.GraphDatabase;
import org.locationtech.geogig.storage.IndexDatabase;
import org.locationtech.geogig.storage.ObjectDatabase;
import org.locationtech.geogig.storage.RefDatabase;
import org.locationtech.geogig.storage.StorageProvider;
import org.locationtech.geogig.storage.VersionedFormat;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.util.Modules;

public class GeoServerContextBuilder extends ContextBuilder {

    @Override
    public Context build(Hints hints) {
        return Guice
                .createInjector(Modules
                        .override(new GeogigModule(), new HintsModule(hints))
                        .with(new PluginsModule(), new DefaultPlugins()))
                .getInstance(org.locationtech.geogig.repository.Context.class);
    }

    public static class DefaultPlugins extends AbstractModule {

        @Override
        protected void configure() {

            MapBinder<VersionedFormat, RefDatabase> refPlugins = MapBinder
                    .newMapBinder(binder(), VersionedFormat.class, RefDatabase.class)
                    .permitDuplicates();

            MapBinder<VersionedFormat, ObjectDatabase> objectPlugins = MapBinder
                    .newMapBinder(binder(), VersionedFormat.class, ObjectDatabase.class)
                    .permitDuplicates();

            MapBinder<VersionedFormat, IndexDatabase> indexPlugins = MapBinder
                    .newMapBinder(binder(), VersionedFormat.class, IndexDatabase.class)
                    .permitDuplicates();

            MapBinder<VersionedFormat, GraphDatabase> graphPlugins = MapBinder
                    .newMapBinder(binder(), VersionedFormat.class, GraphDatabase.class)
                    .permitDuplicates();

            Iterable<StorageProvider> providers = StorageProvider.findProviders();

            for (StorageProvider sp : providers) {
                VersionedFormat objectDatabaseFormat = sp.getObjectDatabaseFormat();
                VersionedFormat indexDatabaseFormat = sp.getIndexDatabaseFormat();
                VersionedFormat graphDatabaseFormat = sp.getGraphDatabaseFormat();
                VersionedFormat refsDatabaseFormat = sp.getRefsDatabaseFormat();

                if (objectDatabaseFormat != null) {
                	GeoServerContextBuilder.bind(objectPlugins, objectDatabaseFormat);
                }
                if (indexDatabaseFormat != null) {
                	GeoServerContextBuilder.bind(indexPlugins, indexDatabaseFormat);
                }
                if (graphDatabaseFormat != null) {
                	GeoServerContextBuilder.bind(graphPlugins, graphDatabaseFormat);
                }
                if (refsDatabaseFormat != null) {
                	GeoServerContextBuilder.bind(refPlugins, refsDatabaseFormat);
                }
            }
        }
    }

    static <T> void bind(MapBinder<VersionedFormat, T> plugins, VersionedFormat format) {
        Class<?> implementingClass = format.getImplementingClass();
        checkState(implementingClass != null,
                "If singleton class not provided, this method must be overritten");
        @SuppressWarnings("unchecked")
        Class<? extends T> binding = (Class<? extends T>) implementingClass;
        plugins.addBinding(format).to(binding).in(Scopes.SINGLETON);
    }

}
