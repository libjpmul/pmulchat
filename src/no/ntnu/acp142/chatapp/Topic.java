package no.ntnu.acp142.chatapp;

import java.util.ArrayList;

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
 * Container class for a chat topic, maintaining the topic name, the multicast
 * address used to broadcast to it over, and a list of all its subscribers.
 * 
 * @author Thomas Martin Schmid
 * 
 */
public class Topic {

    /**
     * Topic name, must be shorter than 256 characters.
     */
    private final String                name;
    /**
     * List of subscribers
     */
    private final ArrayList<Subscriber> subscriberList;
    /**
     * If false, the topic may not be deleted.
     */
    private final boolean               mutable;
    /**
     * If a filter is applied to the topic list and this topic passes, this is
     * true
     */
    private boolean                     isVisibleThroughFilter;

    /**
     * Creates a new Topic container.
     * 
     * @param name
     *            of the topic.
     * @param mutable
     *            states whether or not the topic may be deleted.
     */
    public Topic(String name, boolean mutable) {
        this.name = name;
        this.mutable = mutable;
        this.subscriberList = new ArrayList<Subscriber>();
        this.isVisibleThroughFilter = true;
    }

    /**
     * Creates a new Topic container.
     * 
     * @param name
     *            of the topic.
     */
    public Topic(String name) {
        this.name = name;
        this.mutable = true; // Topics created over the network are always
                             // mutable.
        this.subscriberList = new ArrayList<Subscriber>();
        this.isVisibleThroughFilter = true;
    }

    /**
     * Adds a new subscriber to the subscriber list of this topic.
     * 
     * @param subscriber
     *            to add.
     */
    public void addSubscriber(Subscriber subscriber) {
        boolean notFound = true;
        for (Subscriber sub : this.subscriberList) {
            if ( sub.getNodeId() == subscriber.getNodeId() ) {
                notFound = false;
            }
        }
        if ( notFound ) {
            this.subscriberList.add(subscriber);
        }
    }

    /**
     * Removes the subscriber with the given node ID from the list of
     * subscribers
     * 
     * @param subscriberNodeId
     *            ID of subscribers' node.
     * @return The subscriber that was removed, null if id was not found
     */
    public Subscriber removeSubscriber(long subscriberNodeId) {
        Subscriber ret = null;
        for (Subscriber sub : this.subscriberList) {
            if ( sub.getNodeId() == subscriberNodeId ) {
                ret = sub;
                this.subscriberList.remove(sub);
                break;
            }
        }
        return ret;
    }

    /**
     * Gets the topic name
     * 
     * @return Name of the topic
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the value of this.mutable. If true, this topic may be deleted. If
     * false it may not. The value is set upon construction and is always true
     * for topics created by other Nodes, received over the network.
     * 
     * @return true if topic may be deleted.
     */
    public boolean isMutable() {
        return this.mutable;
    }

    /**
     * Gets a copy of the subscriber list. This is used by the networking
     * component.
     * 
     * @return A copy of subscriberList
     */
    public ArrayList<Subscriber> getSubscriberList() {
        ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
        for (Subscriber s : this.subscriberList) {
            subscribers.add(s);
        }
        return subscribers;
    }

    /**
     * Sets whether this is visible through the current filter.
     * 
     * @param isVisible
     *            whether it is visible or not
     */
    public void setFiltered(boolean isVisible) {
        this.isVisibleThroughFilter = isVisible;
    }

    /**
     * Gets whether this is visible through the current filter.
     * 
     * @return whether this topic is visible through the current filter
     */
    public boolean isVisibleThroughFilter() {
        return this.isVisibleThroughFilter;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
