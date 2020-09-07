package org.pmoi.business.ppi;

import java.util.Map;

public interface InteractionQueryClient {
    Map<String, String> getProteinNetwork(String symbol);
}
