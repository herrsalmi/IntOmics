package org.pmoi.database;

import java.net.MalformedURLException;
import java.net.URL;

public class Species {

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
        public String keggOrgId;
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
                //TODO handle this
                e.printStackTrace();
            }
            return this;
        }

        public Species build() {
            return new Species(this);
        }
    }

}
