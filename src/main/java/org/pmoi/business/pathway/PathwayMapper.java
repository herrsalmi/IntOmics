package org.pmoi.business.pathway;

import org.pmoi.model.Pathway;

import java.util.List;

public interface PathwayMapper {
    List<Pathway> getPathways(String gene);
    boolean isInAnyPathway(String gene);
}
