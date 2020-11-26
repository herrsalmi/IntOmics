package org.pmoi.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class Species {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private final String name;
    private final int taxonomyId;
    private final String keggOrgId;
    private final URL url;

    private Species(SpeciesBuilder builder) {
        this.name = builder.name;
        this.taxonomyId = builder.taxonomyId;
        this.url = builder.url;
        this.keggOrgId = builder.keggOrgId;
    }

    public String getName() {
        return name;
    }

    public int getTaxonomyId() {
        return taxonomyId;
    }

    public String getKeggOrgId() {
        return keggOrgId;
    }

    public URL getUrl() {
        return url;
    }

    public static class SpeciesBuilder {
        private final String name;
        private String keggOrgId;
        private int taxonomyId;
        private URL url;

        public SpeciesBuilder(String name) {
            this.name = name;
        }

        public SpeciesBuilder withTaxonomyId(int taxonomyId) {
            this.taxonomyId = taxonomyId;
            return this;
        }

        public SpeciesBuilder withKEGGOrgId(String id) {
            this.keggOrgId = id;
            return this;
        }

        public SpeciesBuilder withUrl(String url) {
            try {
                this.url = new URL(url);
            } catch (MalformedURLException e) {
                LOGGER.error("URL malformed. url={}", url);
            }
            return this;
        }

        public Species build() {
            return new Species(this);
        }
    }

}
