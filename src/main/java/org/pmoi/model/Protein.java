package org.pmoi.model;

import org.pmoi.util.NumberParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Protein extends Feature{
    private List<Pathway> pathways;

    public Protein(String name, String entrezID) {
        this.name = name;
        this.ncbiID = entrezID;
    }

    public Protein(String value) {
        if (NumberParser.tryParseInt(value))
            this.ncbiID = value;
        else
            this.name = value;
        this.pathways = new ArrayList<>();
    }

    public List<Pathway> getPathways() {
        return pathways;
    }

    public void addPathway(Pathway pathway) {
        this.pathways.add(pathway);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Protein protein = (Protein) o;

        if (!Objects.equals(name, protein.name)) return false;
        return ncbiID.equals(protein.ncbiID);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + ncbiID.hashCode();
        return result;
    }

}
