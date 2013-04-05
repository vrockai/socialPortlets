package org.gatein.security.oauth.portlet.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.ActivityFeed;
import com.google.api.services.plus.model.PeopleFeed;
import com.google.api.services.plus.model.Person;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.google.GoogleProcessor;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleFriendsPortlet extends AbstractSocialPortlet<GoogleTokenResponse> {

    private static final String ATTR_PAGINATION_CONTEXT = "paginationContext";
    private static final String PARAM_PAGE = "page";
    private static final String PREV = "prev";
    private static final String NEXT = "next";

    private GoogleProcessor googleProcessor;


    @Override
    protected void afterInit(ExoContainer container) {
        this.googleProcessor = (GoogleProcessor)container.getComponentInstanceOfType(GoogleProcessor.class);
    }


    @Override
    protected OAuthProviderType<GoogleTokenResponse> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_GOOGLE);
    }


    @Override
    protected void handleRenderAction(RenderRequest request, RenderResponse response, String renderAction, GoogleTokenResponse accessToken) throws PortletException, IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    // See https://developers.google.com/+/api/latest/people/list for details
    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, GoogleTokenResponse accessToken) throws PortletException, IOException {
        Plus service = googleProcessor.getPlusService(accessToken);
        final Plus.People.List list = service.people().list("me", "visible");
        // Possible values are "alphabetical", "best"
        list.setOrderBy("alphabetical");
        list.setMaxResults(10L);

        // Try to obtain last pagination token
        PortletSession session = request.getPortletSession();
        PaginationState pgState = (PaginationState)session.getAttribute(ATTR_PAGINATION_CONTEXT);
        if (pgState == null) {
            pgState = new PaginationState();
        }

        // Try to update pgState with number of current page
        String pageParam = request.getParameter(PARAM_PAGE);
        if (pageParam != null) {
            if (PREV.equals(pageParam)) {
                pgState.decreaseCurrentPage();
            } else if (NEXT.equals(pageParam)) {
                pgState.increaseCurrentPage();
            } else {
                throw new PortletException("Illegal value of request parameter 'page'. Value was " + pageParam);
            }
        }

        list.setPageToken(pgState.getTokenOfCurrentPage());

        PeopleFeed peopleFeed = new GoogleRequest<PeopleFeed>(response, "https://www.googleapis.com/auth/plus.login") {

            @Override
            PeopleFeed run() throws IOException {
                return list.execute();
            }

        }.sendRequest();

        if (peopleFeed != null) {
            List<Person> people = peopleFeed.getItems();

            PrintWriter writer = response.getWriter();
            writer.println("<h2>Your google+ friends</h2>");
            writer.println("Total number of friends: " + peopleFeed.getTotalItems() + "<br>");

            for (Person person : people) {
                String displayName = person.getDisplayName();
                String imageURL = person.getImage().getUrl();
                String personUrl = person.getUrl();

                writer.println("<a href=\"" + personUrl + "\"><img src=\"" + imageURL + "\" title=\"" + displayName + "\" /></a>");
            }

            // Obtain next token to session if it's available
            String nextPageToken = peopleFeed.getNextPageToken();
            int currentPage = pgState.getCurrentPage();

            writer.println("<br>Current page: " + currentPage + "<br>");
            // Show link for previous page
            if (currentPage > 1) {
                // TODO: ajax...
                PortletURL portletURL = response.createRenderURL();
                portletURL.setParameter(PARAM_PAGE, PREV);
                writer.println("<a href=\"" + portletURL + "\" style=\"color: blue; \">Previous</a> ");
            }
            // Show link for next page
            if (nextPageToken != null) {
                // TODO: ajax...
                pgState.setTokenForPage(pgState.getCurrentPage() + 1, nextPageToken);
                PortletURL portletURL = response.createRenderURL();
                portletURL.setParameter(PARAM_PAGE, NEXT);
                writer.println("<a href=\"" + portletURL + "\" style=\"color: blue; \">Next</a>");
            }

            session.setAttribute(ATTR_PAGINATION_CONTEXT, pgState);
        }
    }
}
