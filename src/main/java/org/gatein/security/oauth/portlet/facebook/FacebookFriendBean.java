package org.gatein.security.oauth.portlet.facebook;

import java.util.List;

import com.restfb.types.StatusMessage;

/**
 * Created with IntelliJ IDEA.
 * User: vrockai
 * Date: 4/15/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class FacebookFriendBean {

    public boolean isScope() {
        return scope;
    }

    public void setScope(boolean scope) {
        this.scope = scope;
    }

    boolean scope;

    public List<StatusMessage> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<StatusMessage> statuses) {
        this.statuses = statuses;
    }

    List<StatusMessage> statuses;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    String id;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    private String imageUrl;
    private String name;
    private String profileUrl;
}
