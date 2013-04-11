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
    private static final String PARAM_FRIEND_ID = "_friendID";
    private static final String PARAM_FILTER = "_filter";
    private static final int ITEMS_PER_PAGE = 10;

    private static final String ACTION_FILTER = "_actionFilter";
    private static final String BUTTON_TRIGGER_FILTER = "triggerFilter";
    private static final String BUTTON_CANCEL_FILTER = "cancelFilter";

    @ProcessAction(name = ACTION_FILTER)
    public void actionTriggerFilter(ActionRequest aReq, ActionResponse aResp) throws IOException {
        if (aReq.getParameter(BUTTON_TRIGGER_FILTER) != null) {

            // User pressed 'Submit filter'
            getParameterAndSaveItToSession(PARAM_FILTER, aReq, aReq.getPortletSession());
        } else {

            // User pressed 'Cancel filter'
            aReq.getPortletSession().removeAttribute(PARAM_FILTER);
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

        // Obtain info about "me" including picture and show them
        UserWithPicture me = facebookClient.fetchObject("me", UserWithPicture.class, Parameter.with("fields", "id,name,picture"));
        // TODO: ajax...
        PortletURL myUrlForPersonDetail = response.createRenderURL();
        myUrlForPersonDetail.setParameter(PARAM_FRIEND_ID,  me.getId());
        out.println("<img src=\"" + me.getPicture().getData().getUrl() + "\" /><a style=\"color: blue;\" href=\"" + myUrlForPersonDetail + "\">" + me.getName() + "</a><br>");
        out.println("<hr>");

        out.println("<table border><tr><td width=\"50%\" style=\"vertical-align: top\">");
        out.println("<h3>My friends</h3>");

        String filter = (String)session.getAttribute(PARAM_FILTER);
        List<String> idsOfFriendsToDisplay;
        if (filter != null) {
            idsOfFriendsToDisplay = getIdsOfFilteredFriends(filter, facebookClient);
        } else {
            idsOfFriendsToDisplay = getIdsOfPaginatedFriends(request, response, session, facebookClient, out);
        }

        // Filter results
        PortletURL filterURL = response.createActionURL();
        filterURL.setParameter(ActionRequest.ACTION_NAME, ACTION_FILTER);
        out.println("<form action=\"" + filterURL + "\" method=\"POST\">");
        String tmp = filter==null ? "" : " value=\"" + filter + "\"";
        out.println("Filter: <input name=\"" + PARAM_FILTER + "\"" + tmp + " />");
        out.println("<input type=\"submit\" name=\"" + BUTTON_TRIGGER_FILTER + "\" value=\"Submit Filter\" />");
        out.println("<input type=\"submit\" name=\"" + BUTTON_CANCEL_FILTER + "\" value=\"Cancel Filter\" /><br>");
        out.println("</form>");

        out.println("<br><br><hr><br>");

        if (idsOfFriendsToDisplay.size() > 0) {
            // Fetch all required friends with obtained ids
            JsonObject friendsResult = facebookClient.fetchObjects(idsOfFriendsToDisplay, JsonObject.class, Parameter.with("fields", "id,name,picture"));

            for (String id : idsOfFriendsToDisplay) {
                JsonObject current = friendsResult.getJsonObject(id);
                UserWithPicture friendWithPicture = facebookClient.getJsonMapper().toJavaObject(current.toString(), UserWithPicture.class);

                // TODO: ajax...
                PortletURL urlForPersonDetail = response.createRenderURL();
                urlForPersonDetail.setParameter(PARAM_FRIEND_ID,  friendWithPicture.getId());
                out.println("<img src=\"" + friendWithPicture.getPicture().getData().getUrl() + "\" /><a style=\"color: blue;\" href=\"" + urlForPersonDetail + "\">" + friendWithPicture.getName() + "</a><br><br>");
            }
        }
        out.println("</td><td style=\"vertical-align: top\">");

        String friendId = request.getParameter(PARAM_FRIEND_ID);
        if (friendId != null) {
            displayStatusOfPerson(friendId, out, facebookClient, me);
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

    private List<String> getIdsOfPaginatedFriends(RenderRequest request, RenderResponse response, PortletSession session, FacebookClient facebookClient, PrintWriter out) {
        // Count total number of friends
        Integer friendsCount = (Integer)session.getAttribute(ATTR_FRIENDS_COUNT);
        if (friendsCount == null) {
            Connection<NamedFacebookType> myFriends = facebookClient.fetchConnection("me/friends", NamedFacebookType.class);
            friendsCount = myFriends.getData().size();
            session.setAttribute(ATTR_FRIENDS_COUNT, friendsCount);
        }

        Integer pageNumber;
        if (request.getParameter(PARAM_PAGE) != null) {
            pageNumber = Integer.parseInt(request.getParameter(PARAM_PAGE));
            session.setAttribute(PARAM_PAGE, pageNumber);
        } else {
            pageNumber = (Integer)session.getAttribute(PARAM_PAGE);
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }

        Integer pageCount = ((friendsCount-1) / ITEMS_PER_PAGE) + 1;
        Integer indexStart = (pageNumber - 1) * ITEMS_PER_PAGE;
        List<NamedFacebookType> friendsToDisplay = facebookClient.fetchConnection("me/friends", NamedFacebookType.class, Parameter.with("offset", indexStart), Parameter.with("limit", ITEMS_PER_PAGE)).getData();

        out.println("Count of friends: " + friendsCount + "<br>");
        out.println("Page: " + pageNumber + "<br>");
        out.println("Select page: ");
        for (int i=1 ; i<=pageCount ; i++) {
            // TODO: ajax...
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

    private List<String> getIdsOfFilteredFriends(String filter, FacebookClient facebookClient) {
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

    private void displayStatusOfPerson(String friendId, PrintWriter out, FacebookClient facebookClient, NamedFacebookType me) {
        Connection<StatusMessage> statusMessageConnection = facebookClient.fetchConnection(friendId + "/statuses", StatusMessage.class, Parameter.with("limit", 5));
        List<StatusMessage> statuses = statusMessageConnection.getData();

        if (statuses.size() == 0) {
            // Different scope is needed for me and different for my friends
            String neededScope = friendId.equals(me.getId()) ? "user_status" : "friends_status";
            out.println("<b>WARNING: </b>This user doesn't have any public messages or you have insufficient scope. Make sure your access token have scope: <b>" + neededScope + "</b>");
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
}
