package org.gatein.security.oauth.portlet.facebook;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: vrockai
 * Date: 4/15/13
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FacebookBean {

    int friendNumber;

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(int currentPageNumber) {
        this.currentPageNumber = currentPageNumber;
    }

    int currentPageNumber;
    List<String> friendPaginatorUrls;

    public int getFriendNumber() {
        return friendNumber;
    }

    public void setFriendNumber(int friendNumber) {
        this.friendNumber = friendNumber;
    }

    public List<String> getFriendPaginatorUrls() {
        return friendPaginatorUrls;
    }

    public void setFriendPaginatorUrls(List<String> friendPaginatorUrls) {
        this.friendPaginatorUrls = friendPaginatorUrls;
    }
}
