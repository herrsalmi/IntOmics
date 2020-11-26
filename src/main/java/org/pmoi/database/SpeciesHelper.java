package org.pmoi.database;

public class SpeciesHelper {

    private static Species species;

    private SpeciesHelper(){
    }

    public static void makeSpecies(SupportedSpecies name) {
        species = switch (name) {
            case HUMAN -> new Species.SpeciesBuilder("Homo sapiens")
                    .withTaxonomyId(9606)
                    .withKEGGOrgId("hsa")
                    .withUrl("ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz")
                    .build();
            case MOUSE -> new Species.SpeciesBuilder("Mus musculus")
                    .withTaxonomyId(10090)
                    .withKEGGOrgId("mmu")
                    .withUrl("ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Mus_musculus.gene_info.gz")
                    .build();
            case COW -> new Species.SpeciesBuilder("Bos taurus")
                    .withTaxonomyId(9913)
                    .withKEGGOrgId("bta")
                    .withUrl("ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Bos_taurus.gene_info.gz")
                    .build();
            case RAT -> new Species.SpeciesBuilder("Rattus norvegicus")
                    .withTaxonomyId(10116)
                    .withKEGGOrgId("rno")
                    .withUrl("ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Rattus_norvegicus.gene_info.gz")
                    .build();
        };
    }

    public static Species get() {
        if (species == null) {
            throw new UnsupportedOperationException("using get() before calling makeSpecies()");
        }
        return species;
    }
}
