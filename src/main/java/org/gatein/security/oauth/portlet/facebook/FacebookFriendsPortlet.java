/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ProcessAction;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.json.JsonObject;
import com.restfb.types.Comment;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.StatusMessage;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookFriendsPortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

    private static final String ATTR_FRIENDS_COUNT = "friendsCount";
    private static final String PARAM_PAGE = "_page";
    private static final String PARAM_PERSON_ID = "_personID";
    private static final String PARAM_USER_FILTER = "_userFilter";

    private static final int ITEMS_PER_PAGE = 10;

    private static final String ACTION_USER_FILTER = "_actionUserFilter";
    private static final String BUTTON_TRIGGER_FILTER = "triggerFilter";
    private static final String BUTTON_CANCEL_FILTER = "cancelFilter";

    @ProcessAction(name = ACTION_USER_FILTER)
    public void actionTriggerFilter(ActionRequest aReq, ActionResponse aResp) throws IOException {
        if (aReq.getParameter(BUTTON_TRIGGER_FILTER) != null) {

            // User pressed 'Submit filter'
            getParameterAndSaveItToSession(PARAM_USER_FILTER, aReq, aReq.getPortletSession());
        } else {

            // User pressed 'Cancel filter'
            aReq.getPortletSession().removeAttribute(PARAM_USER_FILTER);
        }
    }

    @Override
    protected void afterInit(ExoContainer container) {
    }

    @Override
    protected OAuthProviderType<FacebookAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }


    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, FacebookAccessTokenContext accessToken) throws IOException {
        PortletSession session = request.getPortletSession();
        PrintWriter out = response.getWriter();

        FacebookClient facebookClient = new DefaultFacebookClient(accessToken.getAccessToken());

        // Obtain info about "me" including picture and render them
        UserWithPicture me = facebookClient.fetchObject("me", UserWithPicture.class, Parameter.with("fields", "id,name,picture"));
        renderPerson(response, out, me);
        out.println("<hr>");

        out.println("<table border><tr><td width=\"50%\" style=\"vertical-align: top\">");
        out.println("<h3>My friends</h3>");

        String filter = (String)session.getAttribute(PARAM_USER_FILTER);
        List<String> idsOfFriendsToDisplay;
        if (filter != null) {
            idsOfFriendsToDisplay = getIdsOfFilteredFriends(filter, facebookClient);
        } else {
            idsOfFriendsToDisplay = getIdsOfPaginatedFriends(request, response, session, facebookClient, out);
        }

        // Render form with user filter
        PortletURL userFilterURL = response.createActionURL();
        userFilterURL.setParameter(ActionRequest.ACTION_NAME, ACTION_USER_FILTER);
        out.println("<form action=\"" + userFilterURL + "\" method=\"POST\">");
        String tmp = filter==null ? "" : " value=\"" + filter + "\"";
        out.println("Filter: <input name=\"" + PARAM_USER_FILTER + "\"" + tmp + " />");
        out.println("<input type=\"submit\" name=\"" + BUTTON_TRIGGER_FILTER + "\" value=\"Submit Filter\" />");
        out.println("<input type=\"submit\" name=\"" + BUTTON_CANCEL_FILTER + "\" value=\"Cancel Filter\" /><br>");
        out.println("</form>");

        out.println("<br><br><hr><br>");

        // Render friends with their pictures
        if (idsOfFriendsToDisplay.size() > 0) {
            // Fetch all required friends with obtained ids
            JsonObject friendsResult = facebookClient.fetchObjects(idsOfFriendsToDisplay, JsonObject.class, Parameter.with("fields", "id,name,picture"));

            for (String id : idsOfFriendsToDisplay) {
                JsonObject current = friendsResult.getJsonObject(id);
                UserWithPicture friend = facebookClient.getJsonMapper().toJavaObject(current.toString(), UserWithPicture.class);

                renderPerson(response, out, friend);
                out.println("<br>");
            }
        }
        out.println("</td><td style=\"vertical-align: top\">");

        // Render
        String friendId = request.getParameter(PARAM_PERSON_ID);
        if (friendId != null) {
            displayStatusOfPerson(friendId, out, facebookClient, me, accessToken, response);
        }
        out.println("</td></tr></table>");
    }


    private String getLikersText(List<NamedFacebookType> likers) {
        StringBuilder builder = new StringBuilder();
        for (NamedFacebookType like : likers) {
            builder.append(like.getName() + "\n");
        }
        return builder.toString();
    }


    private List<String> getIdsOfPaginatedFriends(RenderRequest request, RenderResponse response, PortletSession session,
                                                  FacebookClient facebookClient, PrintWriter out) {
        // Count total number of friends
        Integer friendsCount = (Integer)session.getAttribute(ATTR_FRIENDS_COUNT);
        if (friendsCount == null) {
            Connection<NamedFacebookType> myFriends = facebookClient.fetchConnection("me/friends", NamedFacebookType.class);
            friendsCount = myFriends.getData().size();
            session.setAttribute(ATTR_FRIENDS_COUNT, friendsCount);
        }

        // Obtain number of current page
        Integer currentPage;
        if (request.getParameter(PARAM_PAGE) != null) {
            currentPage = Integer.parseInt(request.getParameter(PARAM_PAGE));
            session.setAttribute(PARAM_PAGE, currentPage);
        } else {
            currentPage = (Integer)session.getAttribute(PARAM_PAGE);
        }
        if (currentPage == null) {
            currentPage = 1;
        }

        Integer pageCount = ((friendsCount-1) / ITEMS_PER_PAGE) + 1;
        Integer indexStart = (currentPage - 1) * ITEMS_PER_PAGE;
        List<NamedFacebookType> friendsToDisplay = facebookClient.fetchConnection("me/friends", NamedFacebookType.class, Parameter.with("offset", indexStart), Parameter.with("limit", ITEMS_PER_PAGE)).getData();

        out.println("Count of friends: " + friendsCount + "<br>");
        out.println("Current page: " + currentPage + "<br>");
        out.println("Select page: ");
        for (int i=1 ; i<=pageCount ; i++) {
            PortletURL url = response.createRenderURL();
            url.setParameter(PARAM_PAGE,  String.valueOf(i));
            out.print("<a style=\"color: blue;\" href=\"" + url + "\">" + i + "</a> ");
        }

        // Collect IDS of friends to display
        List<String> ids = new ArrayList<String>();
        for (NamedFacebookType current : friendsToDisplay) {
            ids.add(current.getId());
        }

        return ids;
    }


    // Pagination is skipped if user filtering is enabled
    private List<String> getIdsOfFilteredFriends(String filter, FacebookClient facebookClient) {
        // Not good to obtain all friends within each request, but we don't have better way atm (limitation of facebook search api...)
        // TODO: Cache it?
        Connection<NamedFacebookType> connection = facebookClient.fetchConnection("me/friends", NamedFacebookType.class);
        List<NamedFacebookType> allFriends = connection.getData();
        List<String> result = new ArrayList<String>();
        for (NamedFacebookType current : allFriends) {
            if (current.getName().contains(filter)) {
                result.add(current.getId());
            }
        }
        return result;
    }


    private void displayStatusOfPerson(String friendId, PrintWriter out, FacebookClient facebookClient, NamedFacebookType me, FacebookAccessTokenContext accessTokenContext, RenderResponse response) {
        Connection<StatusMessage> statusMessageConnection = facebookClient.fetchConnection(friendId + "/statuses", StatusMessage.class, Parameter.with("limit", 5));
        List<StatusMessage> statuses = statusMessageConnection.getData();

        if (statuses.size() == 0) {
            // Different scope is needed for me and different for my friends
            String neededScope = friendId.equals(me.getId()) ? "user_status" : "friends_status";

            if (accessTokenContext.isScopeAvailable(neededScope)) {
                out.println("This user doesn't have any public messages");
            } else {
                out.println("<b>WARNING: </b>You have insufficient privileges (Facebook scope) to show status on FB wall. Your access token needs to have scope: <b>" + neededScope + "</b><br>");

                // Create URL for start OAuth2 flow with custom scope added
                PortletURL actionURL = response.createActionURL();
                actionURL.setParameter(ActionRequest.ACTION_NAME, AbstractSocialPortlet.ACTION_OAUTH_REDIRECT);
                actionURL.setParameter(OAuthConstants.PARAM_CUSTOM_SCOPE, neededScope);
                out.println("Click <a style=\"color: blue;\" href=\"" + actionURL + "\">here</a> to fix it<br>");
            }
        } else {
            NamedFacebookType currentFriendToDisplay = facebookClient.fetchObject(friendId, NamedFacebookType.class, Parameter.with("fields", "id,name"));
            out.println("<h3>" + currentFriendToDisplay.getName() + "</h3>");
            for (StatusMessage statusMessage : statuses) {
                out.println("<b>Status message: </b>" + statusMessage.getMessage() + "<br>");
                out.println("<div style=\"font-size: 13px;\">");
                out.println("Time: " + statusMessage.getUpdatedTime() + " - ");
                out.println("<img src=\"TODO:some-thumbs-picture.gif\" alt=\"Likes: " + statusMessage.getLikes().size() + "\" title=\"" + getLikersText(statusMessage.getLikes()) + "\" /></div><br><hr>");

                List<Comment> comments = statusMessage.getComments();
                out.println("<b>Comments: </b><br>");
                for (Comment comment : comments) {
                    out.println("<i>" + comment.getFrom().getName() + "</i>: " + comment.getMessage() + "<br>");
                    out.println("<div style=\"font-size: 11px;\">Time: " + comment.getCreatedTime() + " - Likes: " + comment.getLikeCount() + "</div><br>");
                }
                out.println("<br><br><hr>");
            }
        }
    }


    private void renderPerson(RenderResponse response, PrintWriter out, UserWithPicture personToRender) {
        PortletURL myUrlForPersonDetail = response.createRenderURL();
        myUrlForPersonDetail.setParameter(PARAM_PERSON_ID, personToRender.getId());
        out.println("<img src=\"" + personToRender.getPicture().getData().getUrl() + "\" /><a style=\"color: blue;\" href=\"" + myUrlForPersonDetail + "\">" + personToRender.getName() + "</a><br>");
    }
}
