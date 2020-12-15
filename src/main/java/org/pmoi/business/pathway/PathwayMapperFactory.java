package org.pmoi.business.pathway;

import org.pmoi.model.PathwayMode;

import java.util.EnumMap;

public final class PathwayMapperFactory {

    private static final EnumMap<PathwayMode, PathwayMapper> pathwayMap = new EnumMap<>(PathwayMode.class);

    public static PathwayMapper getPathwayMapper(PathwayMode type) {
        return pathwayMap.computeIfAbsent(type, k -> {
            switch (k) {
                case KEGG:
                    return new KEGGPathwayMapper();
                case WIKIPATHWAYS:
                    return new WikiPathwaysMapper();
                case REACTOME:
                    return new ReactomePathwayMapper();
            }
            return null;
        });
    }
}
