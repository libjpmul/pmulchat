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
 * Contains a reference to the sender, and the content of a message.
 * 
 * @author Thomas Martin Schmid
 * 
 */
public class Message {

    /**
     * Reference to the sender
     */
    private Subscriber sender;
    /**
     * Message content
     */
    private String     content;

    /**
     * Creates a new Message object with the given content and sender reference.
     * 
     * @param sender
     *            Reference to sender Subscriber object
     * @param content
     *            Content of message
     */
    public Message(Subscriber sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    /**
     * Gets the sender of this message
     * 
     * @return Subscriber reference of sender
     */
    public Subscriber getSender() {
        return this.sender;
    }

    /**
     * Gets the content of the message
     * 
     * @return Message content
     */
    public String getContent() {
        return this.content;
    }

    @Override
    public String toString() {
        return "<font color=\"#990000\">" + getSender().getUserName() + ":</font> " + getContent();
    }
}
