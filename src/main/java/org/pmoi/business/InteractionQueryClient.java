package org.pmoi.business;

import java.util.Map;

public interface InteractionQueryClient {
    Map<String, String> getProteinNetwork(String symbol);
}
