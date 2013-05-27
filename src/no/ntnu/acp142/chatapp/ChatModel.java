package no.ntnu.acp142.chatapp;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import no.ntnu.acp142.Configuration;

/*
 * Copyright (c) 2013, Luka Cetusic, Thomas Martin Schmid
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
 * Contains a list of all Chat object instances.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid
 */
public class ChatModel {

    private static PropertyChangeSupport propertyChangeSupport;

    public static final String           ADD_MESSAGE_PROPERTY  = "AddMessage";
    public static final String           CURRENT_CHAT_PROPERTY = "CurrentChat";

    /**
     * currentChat keeps track of which chat is used.
     */
    private Chat                         currentChat;
    /**
     * List of all messages in this chat.
     */
    private ArrayList<Chat>              chatList;

    /**
     * Initializes the chat list.
     */
    public ChatModel() {
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.chatList = new ArrayList<Chat>();
        this.currentChat = new Chat(null,
                new Subscriber(Configuration.getNodeId(), "User_" + Configuration.getNodeId()));
        addMessage(new Message(new Subscriber(0, "Admin"), "Choose a topic from the list."));
    }

    /**
     * Initializes the chat list taking the nodeID as a parameter. Useful for
     * debugging.
     * 
     * @param nodeId
     *            to use
     */
    public ChatModel(int nodeId) {
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.chatList = new ArrayList<Chat>();
        this.currentChat = new Chat(null, new Subscriber(nodeId, "User_" + nodeId));
        addMessage(new Message(new Subscriber(0, "Admin"), "Choose a topic from the list."));
    }

    /**
     * Gets the topic for this chat
     * 
     * @return Reference to the topic of this chat.
     */
    public Chat getCurrentChat() {
        return this.currentChat;
    }

    /**
     * Gets the list of all the messages received for this chat.
     * 
     * @return The list of all messages.
     */
    public ArrayList<Chat> getChatList() {
        if ( this.chatList == null ) {
            this.chatList = new ArrayList<Chat>();
        }
        return this.chatList;
    }

    /**
     * Adds a message to the current chat and notifies the view that the chat
     * has changed.
     * 
     * @param message
     *            to add
     */
    public void addMessage(Message message) {
        // Add the chat
        this.currentChat.addMessage(message);
        propertyChangeSupport.firePropertyChange(ADD_MESSAGE_PROPERTY, null, message);
    }

    /**
     * Switches currentChat to the one corresponding to the new topic. If the
     * chat does not yet exist, it is created. Also moves the user from
     * subscribing to the old chat's topic to the new ones.
     * 
     * @param topic
     *            to switch to
     */
    public void setCurrentChat(Topic topic) {
        String oldValue = getCurrentChat().toString();
        if ( this.currentChat.getTopic() != null ) {
            this.currentChat.getTopic().removeSubscriber(this.currentChat.getSelf().getNodeId());
            this.currentChat.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), "You left."));
            this.currentChat.setCapacity(ChatConfigurationModel.getMaximumMessagesToKeepForInactiveChat());
        }

        Chat newChat = null;
        for (Chat c : this.chatList) {
            if ( c.getTopic() == topic ) {
                newChat = c;
                break;
            }
        }
        if ( newChat == null ) {
            newChat = new Chat(topic, this.currentChat.getSelf());
            this.chatList.add(newChat);
        }
        this.currentChat = newChat;
        this.currentChat.setCapacity(ChatConfigurationModel.getMaximumMessagesToKeepForActiveChat());

        this.currentChat.getTopic().addSubscriber(this.currentChat.getSelf());
        this.currentChat.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), "You joined."));

        propertyChangeSupport.firePropertyChange(CURRENT_CHAT_PROPERTY, oldValue, topic);
    }

    /**
     * Adds the propertyChangeListener to our propertyChangeSupport object
     * 
     * @param listener
     *            to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Gets the current time as a string in a neat format.
     * 
     * @return Current time formatted into a string.
     */
    public static String getTimeAsString() {
        return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));
    }
}
