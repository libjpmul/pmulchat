package no.ntnu.acp142.chatapp;

import no.ntnu.acp142.Configuration;

import java.util.ArrayList;

import javax.swing.AbstractListModel;

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
 * Chat<br>
 * Chat container. Contains a list of all messages in order of reception. Each
 * container also has a reference to the topic.
 * 
 * @author Thomas Martin Schmid
 */
public class Chat extends AbstractListModel<Message> {

    /**
     * Dictates how many messages it keeps in its history.
     */
    private int                capacity;
    /**
     * List of all messages in this chat.
     */
    private ArrayList<Message> messageList;
    /**
     * Topic name for this chat.
     */
    private final Topic        topic;
    /**
     * Reference to our own subscriber object.
     */
    private Subscriber         self;

    /**
     * Creates a new chat
     * 
     * @param topic
     *            Topic of this chat
     * @param self
     *            Subscriber reference of the user.
     */
    public Chat(Topic topic, Subscriber self) {
        this.topic = topic;
        this.messageList = new ArrayList<Message>();
        this.self = self;
        this.capacity = 100;
    }

    /**
     * Gets the list of all the messages received for this chat.
     * 
     * @return The list of all messages.
     */
    public ArrayList<Message> getMessageList() {
        if ( this.messageList == null ) {
            this.messageList = new ArrayList<Message>();
        }
        return this.messageList;
    }

    /**
     * Gets the topic for this chat
     * 
     * @return Reference to the topic of this chat.
     */
    public Topic getTopic() {
        return this.topic;
    }

    /**
     * Gets the subscriber object of the current user.
     * 
     * @return Reference to user's subscriber object.
     */
    public Subscriber getSelf() {
        return this.self;
    }

    /**
     * Adds a message to the message list, removing the oldest messages while
     * capacity is exceeded.
     * 
     * @param message
     *            to add
     */
    public void addMessage(Message message) {
        while (this.messageList.size() >= this.capacity) {
            this.messageList.remove(0);
        }
        this.messageList.add(message);
    }

    @Override
    public Message getElementAt(int index) {
        return this.messageList.get(index);
    }

    @Override
    public int getSize() {
        return this.messageList.size();
    }

    @Override
    public String toString() {
        return this.topic == null ? "" : this.topic.getName();
    }

    /**
     * Sets the maximum number of messages to keep in its history.
     * 
     * @param max
     *            Number of messages to keep.
     */
    public void setCapacity(int max) {
        this.capacity = max;
    }

    /**
     * Resets the Self subscriber instance with default values
     * 
     * @param newUserName
     *            New username to use. Null uses a standard username.
     */
    public void resetSelf(String newUserName) {
        if ( newUserName == null ) {
            newUserName = "User_" + Configuration.getNodeId();
        }
        this.self = new Subscriber(Configuration.getNodeId(), newUserName);
    }
}
