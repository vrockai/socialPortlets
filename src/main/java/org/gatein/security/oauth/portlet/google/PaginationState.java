package org.gatein.security.oauth.portlet.google;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class PaginationState implements Serializable {

    private int currentPage = 1;
    private Map<Integer, String> paginationMapping = new HashMap<Integer, String>();

    public int getCurrentPage() {
        return currentPage;
    }

    public void increaseCurrentPage() {
        currentPage++;
    }

    public void decreaseCurrentPage() {
        currentPage--;
    }

    public void setTokenForPage(int page, String token) {
        paginationMapping.put(page, token);
    }

    public String getTokenOfCurrentPage() {
        return paginationMapping.get(currentPage);
    }
}
