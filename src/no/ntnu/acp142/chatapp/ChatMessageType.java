package no.ntnu.acp142.chatapp;

/*
 * Copyright (c) 2013, Thomas Martin Schmid
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     (1) Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer. 
 * 
 *     (2) Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.  
 *     
 *     (3) The name of the author may not be used to
 *     endorse or promote products derived from this software without
 *     specific prior written permission.
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
 * Enum identifying ChatMessage types.
 * 
 * @author Thomas Martin Schmid
 */
public enum ChatMessageType {

    // Message types pertaining to topics
    GET_TOPICS,            // Used upon startup to get a list of all existing
                           // topics.
    NEW_TOPIC,             // Used to announce the creation of a new topic.
    DELETE_TOPIC_QUERY,    // Used to delete a topic. Is sent to query if it can
                           // be deleted.
    DELETE_TOPIC_SUCCESS,  // Used to delete a topic. Is sent when a query has
                           // not been answered with a TOPIC_IN_USE.
    JOIN_TOPIC,            // Used to join a topic.
    LEAVE_TOPIC,           // Used to leave a topic.

    // Message types handling responses
    TOPIC_LIST,            // Response to GET_TOPICS, contains a list of available
                           // topics.
    TOPIC_IN_USE,          // Response to NEW_TOPIC if the topic already exists
                           // with the multicast address to use.
                           // Response to DELETE_TOPIC if topic is still in use.
    SUBSCRIBER_LIST,       // Response to JOIN_TOPIC, contains a list of IDs for
                           // all subscribers.

    // Message types pertaining to message sending
    SEND_MESSAGE,          // Used to send messages
    
    // Message types pertaining to node lists
    NODE_LIST,             // Used to respond to UDP requests for all node IDs
    
    // Message types used to notify of departure (signing off)
    NODE_LEAVE,            // Sent on client shutdown to notify other nodes to
                           // remove the ID from their destination array.
    
    // Used to identify invalid messages
    INVALID    
}
