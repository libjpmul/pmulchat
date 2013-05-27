package no.ntnu.acp142.chatapp;

/*
 * Copyright (c) 2013, Thomas Martin Schmid
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * (1) Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * (2) Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * 
 * (3) The name of the author may not be used to
 * endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Class contains the information about a subscriber to a given chat.
 * 
 * @author Thomas Martin Schmid
 */
public class Subscriber {

    /**
     * Source ID of the subscriber
     */
    private long   nodeId;
    /**
     * Username to display with subscriber's messages. Usernames must be
     * shorter than 256 characters.
     */
    private String username;

    /**
     * Creates a new subscriber object.
     * 
     * @param nodeId
     *            of the subscriber
     * @param username
     *            to display with the subscriber
     */
    public Subscriber(long nodeId, String username) {
        this.nodeId = nodeId;
        this.username = username;
    }

    /**
     * Gets the username of the subscriber
     * 
     * @return Username of subscriber
     */
    public String getUserName() {
        return username;
    }

    /**
     * Sets the username of the subscriber
     * 
     * @param username
     *            Username to set for subscriber
     */
    public void setUserName(String username) {
        this.username = username;
    }

    /**
     * Gets the source ID of the subscriber
     * 
     * @return ID of subscriber
     */
    public long getNodeId() {
        return this.nodeId;
    }
}
