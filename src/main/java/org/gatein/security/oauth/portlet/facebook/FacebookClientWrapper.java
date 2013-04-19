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
import java.util.ArrayList;
import java.util.List;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.json.JsonObject;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.StatusMessage;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;

/**
 * Wrapper around {@link FacebookClient}, which handles error situations
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookClientWrapper {

    private final RenderRequest request;
    private final RenderResponse response;
    private final PortletContext portletContext;
    private final OAuthProviderType<FacebookAccessTokenContext> oauthProviderType;
    private final FacebookClient facebookClient;


    FacebookClientWrapper(RenderRequest request, RenderResponse response, PortletContext portletContext,
                          OAuthProviderType<FacebookAccessTokenContext> oauthPrType, String accessToken) {
        this.request = request;
        this.response = response;
        this.portletContext = portletContext;
        this.oauthProviderType = oauthPrType;
        this.facebookClient = new DefaultFacebookClient(accessToken);
    }


    /**
     * @return Info about me (current user)
     */
    public UserWithPicture getMe() throws IOException, PortletException {
        return this.fetchObject("me", UserWithPicture.class, Parameter.with("fields", "id,name,picture"));
    }


    /**
     * @return number of all your friends on Facebook or -1 if error occured
     */
    public int getFriendsCount() throws IOException, PortletException {
        Connection<NamedFacebookType> myFriends = this.fetchConnection("me/friends", NamedFacebookType.class);
        return myFriends != null ? myFriends.getData().size() : -1;
    }


    /**
     * Filter string, which will be used for filtering of users by "contains" rule.
     *
     * For example: You have 3 friends on Facebook with names: "John Woo", "Marc Boo", "Susie Cole" and filter will be "oo"
     * Then output of this method will return "John Woo" and "Marc Boo"
     *
     * @param filter Filter string, which will be used for filtering of users by "contains" rule. For example
     * @return ids of filtered friends
     */
    public List<String> getIdsOfFilteredFriends(String filter) throws PortletException, IOException {
        // Not good to obtain all friends within each request, but we don't have better way atm (limitation of facebook search api...)
        Connection<NamedFacebookType> connection = this.fetchConnection("me/friends", NamedFacebookType.class);
        if (connection == null) {
            return new ArrayList<String>();
        }

        List<NamedFacebookType> allFriends = connection.getData();
        List<String> result = new ArrayList<String>();
        for (NamedFacebookType current : allFriends) {
            if (current.getName().contains(filter)) {
                result.add(current.getId());
            }
        }
        return result;
    }


    /**
     * get Page of friends according to given offset and limit
     * @param offset
     * @param limit
     * @return friends starting from offset and number of returned friends will be limit
     */
    public List<NamedFacebookType> getPageOfFriends(int offset, int limit) throws PortletException, IOException {
        Connection<NamedFacebookType> con = this.fetchConnection("me/friends", NamedFacebookType.class,
                Parameter.with("offset", offset), Parameter.with("limit", limit));
        if (con != null) {
            return con.getData();
        } else {
            return new ArrayList<NamedFacebookType>();
        }
    }


    /**
     * Return list of friends based on list of given ids
     *
     * @param idsOfFriendsToDisplay of friends, which we want to obtain from facebook
     * @return list of friends
     */
    public List<FacebookUserBean> getFriends(final List<String> idsOfFriendsToDisplay) throws IOException, PortletException {
        List<FacebookUserBean> fbFriends = new ArrayList<FacebookUserBean>();

        // Render friends with their pictures
        if (idsOfFriendsToDisplay.size() > 0) {
            // Fetch all required friends with obtained ids
            this.fetchObjects(idsOfFriendsToDisplay, JsonObject.class, Parameter.with("fields", "id,name,picture"));

            JsonObject friendsResult = this.fetchObjects(idsOfFriendsToDisplay, JsonObject.class, Parameter.with("fields", "id,name,picture"));

            if (friendsResult == null) {
                return fbFriends;
            }

            for (String id : idsOfFriendsToDisplay) {
                JsonObject current = friendsResult.getJsonObject(id);
                UserWithPicture friend = facebookClient.getJsonMapper().toJavaObject(current.toString(), UserWithPicture.class);
                fbFriends.add(new FacebookUserBean(friend));
            }
        }

        return fbFriends;
    }

    /**
     * return FacebookUserBean with filled status messages
     *
     * @param friendId
     * @param myId This parameter is used only to recognize Facebook scope, which we need
     * @param accessTokenContext
     * @return FacebookUserBean with filled status messages
     */
    public FacebookUserBean getStatusesOfPerson(String friendId, String myId, FacebookAccessTokenContext accessTokenContext)
            throws PortletException, IOException {
        Connection<StatusMessage> statusMessageConnection = this.fetchConnection(friendId + "/statuses", StatusMessage.class, Parameter.with("limit", 5));
        if (statusMessageConnection == null) {
            return null;
        }

        List<StatusMessage> statuses = statusMessageConnection.getData();

        FacebookUserBean ffb = new FacebookUserBean();
        ffb.setId(friendId);
        ffb.setScope(false);
        ffb.setStatuses(statuses);

        if (statuses.size() == 0) {
            // Different scope is needed for me and different for my friends
            String neededScope = friendId.equals(myId) ? "user_status" : "friends_status";

            if (accessTokenContext.isScopeAvailable(neededScope)) {
                ffb.setScope(true);
            } else {
                ffb.setNeededScope(neededScope);
            }
        } else {
            NamedFacebookType currentFriendToDisplay = this.fetchObject(friendId, NamedFacebookType.class, Parameter.with("fields", "id,name"));
            if (currentFriendToDisplay != null) {
                ffb.setName(currentFriendToDisplay.getName());
            }
        }

        return ffb;
    }


    private <T> T fetchObject(final String object, final Class<T> objectType, final Parameter... parameters) throws PortletException, IOException {
        FacebookRequest<T> facebookRequest = new FacebookRequest<T>(request, response, portletContext, oauthProviderType) {

            @Override
            protected T execute() throws OAuthException, FacebookException {
                return facebookClient.fetchObject(object, objectType, parameters);
            }
        };

        return facebookRequest.sendRequest();
    }


    private <T> Connection<T> fetchConnection(final String connection, final Class<T> connectionType, final Parameter... parameters)
            throws PortletException, IOException {
        FacebookRequest<Connection<T>> facebookRequest = new FacebookRequest<Connection<T>>(request, response, portletContext, oauthProviderType) {

            @Override
            protected Connection<T> execute() throws OAuthException, FacebookException {
                return facebookClient.fetchConnection(connection, connectionType, parameters);
            }
        };

        return facebookRequest.sendRequest();
    }

    private <T> T fetchObjects(final List<String> objects, final Class<T> objectType, final Parameter... parameters) throws PortletException, IOException {
        FacebookRequest<T> facebookRequest = new FacebookRequest<T>(request, response, portletContext, oauthProviderType) {

            @Override
            protected T execute() throws OAuthException, FacebookException {
                return facebookClient.fetchObjects(objects, objectType, parameters);
            }
        };

        return facebookRequest.sendRequest();
    }
}
