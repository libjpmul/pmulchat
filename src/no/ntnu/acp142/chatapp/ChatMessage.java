package no.ntnu.acp142.chatapp;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
 * Wrapper class for chat messages' bytes. Packs and unpacks bytes according to
 * the correct format.
 * 
 * @author Thomas Martin Schmid
 * 
 */
public class ChatMessage {

    // ----------
    // Constants
    // ----------
    static final byte             ADDRESS_TYPE_IPV4        = 0;
    static final byte             ADDRESS_TYPE_IPV6        = 1;
    static final int              INET_ADDRESS_LENGTH_IPV4 = 4;
    static final int              INET_ADDRESS_LENGTH_IPV6 = 16;

    // --------------
    // Shared fields
    // --------------
    /**
     * Type of the message. All messages have this.
     */
    private ChatMessageType       type;
    /**
     * UNIX timestamp for when the message was received, only affects incoming
     * messages.
     */
    private double                timeReceived;

    // ----------------------
    // Type dependant fields
    // ----------------------

    /**
     * List of topics in case of TOPIC_LIST.
     */
    private ArrayList<Topic>      topics;
    /**
     * Topic in case of NEW_TOPIC, DELETE_TOPIC, JOIN_TOPIC, LEAVE_TOPIC or
     * TOPIC_IN_USE.
     */
    private Topic                 topic;
    /**
     * List of subscribers in case of SUBSCRIBER_LIST
     */
    private ArrayList<Subscriber> subscribers;
    /**
     * Message content in case of SEND_MESSAGE
     */
    private String                message;
    /**
     * Message sender ID in case of SEND_MESSAGE & NODE_LEAVE
     */
    private long                  senderId;
    /**
     * Message sender username, used in case of JOIN_TOPIC created locally
     */
    private String                senderUserName;
    /**
     * List of node IDs.
     */
    private ArrayList<Integer>    nodeIds;

    // -------------
    // Other fields
    // -------------
    /**
     * This is used to keep track of the offset when in methods
     */
    private int                   accumulatedOffset;

    /**
     * Creates a ChatMessage object from raw octets.
     * 
     * @param octets
     *            to construct it from.
     */
    public ChatMessage(byte[] octets) {
        // Set everything to null for consistency in case of errors
        type = ChatMessageType.INVALID;
        topics = null;
        topic = null;
        subscribers = null;
        message = null;
        senderId = 0;
        senderUserName = null;
        nodeIds = null;
        // Set time received
        this.timeReceived = (double) System.currentTimeMillis() / 1000.0;
        // Check that octets is not null or empty
        if ( octets == null || octets.length < 1 ) {
            System.out.println("ChatMessage.ChatMessage(): Attempted to parse message with no octets.");
            return;
        }
        // Check that type is valid
        if ( (octets[0] & 0xff) > ChatMessageType.values().length ) {
            System.out.println("ChatMessage.ChatMessage(): ChatMessageType invalid '" + (octets[0] & 0xff) + "'.");
            return;
        }
        // Fetch type
        this.type = ChatMessageType.values()[octets[0] & 0xff];

        try {
            switch ( this.type ) {
            case GET_TOPICS:
                // No data.
                break;
            case NEW_TOPIC:
            case DELETE_TOPIC_QUERY:
            case DELETE_TOPIC_SUCCESS:
            case LEAVE_TOPIC:
            case TOPIC_IN_USE:
                // Grab topic
                this.accumulatedOffset = 1;
                this.topic = extractTopic(octets);
                break;
            case JOIN_TOPIC:
                // Grab topic
                this.accumulatedOffset = 1;
                this.topic = extractTopic(octets);
                int end = (octets[this.accumulatedOffset++] & 0xff) + this.accumulatedOffset;
                byte[] bytes = new byte[end - this.accumulatedOffset];
                for (int i = this.accumulatedOffset; i < end; ++i) {
                    bytes[i - this.accumulatedOffset] = octets[i];
                }
                this.senderUserName = new String(bytes, "UTF-8");

                break;
            case TOPIC_LIST:
                // Grab topics
                this.topics = new ArrayList<Topic>();
                int numberOfTopics = (octets[1] & 0xff); // Maximum of 255
                                                         // topics per
                                                         // message
                this.accumulatedOffset = 2; // Last previously read byte
                for (int i = 0; i < numberOfTopics; ++i) {
                    Topic t = extractTopic(octets);
                    if ( t != null ) { // Invalid addresses give a null-return
                        this.topics.add(t);
                    }
                }
                break;
            case SUBSCRIBER_LIST:
                // Grab topic
                this.accumulatedOffset = 1;
                this.topic = extractTopic(octets);
                // Grab subscribers
                this.subscribers = new ArrayList<Subscriber>();
                int numberOfSubscribers = (octets[this.accumulatedOffset] & 0xff);
                for (int i = 0; i < numberOfSubscribers; ++i) {
                    ++this.accumulatedOffset;
                    this.subscribers.add(extractSubscriber(octets));
                }
                break;
            case SEND_MESSAGE:
                // Grab ID of sender
                this.senderId = 0x0000000FFFFFFFFL & (long) ByteBuffer.wrap(octets).getInt(1);
                // Grab message
                this.accumulatedOffset = 5;
                this.topic = extractTopic(octets);
                int length = (octets[this.accumulatedOffset++] & 0xff) << 8 | (octets[this.accumulatedOffset++] & 0xff);
                byte[] bytes2 = new byte[length];
                for (int i = this.accumulatedOffset; i < length + this.accumulatedOffset; ++i) {
                    bytes2[i - this.accumulatedOffset] = octets[i];
                }
                this.message = new String(bytes2, "UTF-8");
                break;
            case NODE_LIST:
                // Grab the amount of IDs
                int count = ((octets[1] & 0xff) << 8 | (octets[2] & 0xff));
                this.nodeIds = new ArrayList<Integer>();
                for (int i = 0; i < count; ++i) {
                    int id = ((octets[(i * 4) + 3] & 0xff) << 24) + ((octets[(i * 4) + 4] & 0xff) << 16)
                            + ((octets[(i * 4) + 5] & 0xff) << 8) + (octets[(i * 4) + 6] & 0xff);
                    this.nodeIds.add(id);
                }
                break;
            case NODE_LEAVE:
                // Grab ID of sender
                this.senderId = 0x0000000FFFFFFFFL & (long) ByteBuffer.wrap(octets).getInt(1);
                break;
            default:
                System.out.println("ChatMessage.ChatMessage(): Could not parse message of type '" + this.type
                        + "', unknown type.");
                break;
            }
        } catch (IndexOutOfBoundsException e) { // If the message is too short,
                                                // catch the exception here.
            System.out.println("ChatMessage.ChatMessage(): Could not parse message of type '" + this.type
                    + "', index out of bounds.");
            this.type = ChatMessageType.INVALID; // Invalidate the message
        } catch (UnsupportedEncodingException e) {
            System.out.println("ChatMessage.ChatMessage(): UTF-8 not supported.");
            e.printStackTrace();
            this.type = ChatMessageType.INVALID; // Invalidate the message
        }
    }

    /**
     * Creates a byte array and populates it with the data to be sent.
     * 
     * @return The byte array of the message data.
     */
    public byte[] getRawMessage() {
        byte[] octets = null;
        try {
            byte type = (byte) this.type.ordinal();
            int size;

            switch ( this.type ) {
            case GET_TOPICS:
                octets = new byte[1];
                octets[0] = type;
                break;
            case NEW_TOPIC:
            case DELETE_TOPIC_QUERY:
            case DELETE_TOPIC_SUCCESS:
            case LEAVE_TOPIC:
            case TOPIC_IN_USE:
                if ( this.topic == null ) {
                    System.out
                            .println("ChatMessage.getRawMessage(): Tried packing a incomplete message. Type: " + type);
                    return null;
                }
                size = 1 // Message type
                + 1 // Length of name
                + this.topic.getName().getBytes("UTF-8").length; // Name content
                octets = new byte[size];
                octets[0] = type;
                this.accumulatedOffset = 1;
                packTopic(octets, this.topic);
                break;
            case JOIN_TOPIC:
                if ( this.topic == null ) {
                    System.out
                            .println("ChatMessage.getRawMessage(): Tried packing a incomplete message. Type: " + type);
                    return null;
                }
                size = 1 // Message type
                        + 1 // Length of name
                        + this.topic.getName().getBytes("UTF-8").length // Name
                                                                        // content
                        + 1 // Length of username
                        + this.senderUserName.getBytes("UTF-8").length;
                octets = new byte[size];
                octets[0] = type;
                this.accumulatedOffset = 1;
                packTopic(octets, this.topic);
                octets[this.accumulatedOffset++] = (byte) (this.senderUserName.getBytes("UTF-8").length & 0xff);
                for (int i = 0; i < this.senderUserName.getBytes("UTF-8").length; ++this.accumulatedOffset, ++i) {
                    octets[this.accumulatedOffset] = this.senderUserName.getBytes("UTF-8")[i];
                }
                break;
            case TOPIC_LIST:
                if ( this.topics == null ) {
                    System.out
                            .println("ChatMessage.getRawMessage(): Tried packing a incomplete message. Type: " + type);
                    return null;
                }
                // Calculate size
                size = 2;
                for (Topic t : this.topics) {
                    size += 1;
                    size += t.getName().getBytes("UTF-8").length;
                }
                // Fill the array
                octets = new byte[size];
                octets[0] = type;
                octets[1] = (byte) this.topics.size();
                this.accumulatedOffset = 2;
                for (Topic t : this.topics) {
                    packTopic(octets, t);
                }
                break;
            case SUBSCRIBER_LIST:
                if ( this.subscribers == null || this.topic == null ) {
                    System.out
                            .println("ChatMessage.getRawMessage(): Tried packing a incomplete message. Type: " + type);
                    return null;
                }
                // Calculate size
                size = 2;
                for (Subscriber s : this.subscribers) {
                    size += 5;
                    size += s.getUserName().getBytes("UTF-8").length;
                }
                size += 1 + this.topic.getName().length();
                // Put the type
                octets = new byte[size];
                octets[0] = type;
                // Put topic
                this.accumulatedOffset = 1;
                packTopic(octets, this.topic);
                // Put subscribers
                octets[this.accumulatedOffset++] = (byte) this.subscribers.size();
                for (Subscriber s : this.subscribers) {
                    packSubscriber(octets, s);
                }
                break;
            case SEND_MESSAGE:
                if ( this.message == null ) {
                    System.out
                            .println("ChatMessage.getRawMessage(): Tried packing a incomplete message. Type: " + type);
                    return null;
                }
                size = 8 + this.topic.getName().getBytes("UTF-8").length + this.message.getBytes("UTF-8").length;
                octets = new byte[size];
                octets[0] = type;
                // Put ID of sender
                octets[1] = (byte) ((this.senderId >> 24) & 0xff);
                octets[2] = (byte) ((this.senderId >> 16) & 0xff);
                octets[3] = (byte) ((this.senderId >> 8) & 0xff);
                octets[4] = (byte) (this.senderId & 0xff);
                // Put the topic we are sending to
                this.accumulatedOffset = 5;
                packTopic(octets, this.topic);
                // Put message length
                octets[this.accumulatedOffset++] = (byte) ((this.message.getBytes("UTF-8").length >> 8) & 0xff);
                octets[this.accumulatedOffset++] = (byte) (this.message.getBytes("UTF-8").length & 0xff);
                // Put message content
                for (int i = this.accumulatedOffset, j = 0; i < size; ++i, ++j) {
                    octets[i] = this.message.getBytes("UTF-8")[j];
                }
                break;
            case NODE_LIST:
                int sizeNodes = 3 + (Math.min(nodeIds.size(), 65535) * 4);
                octets = new byte[sizeNodes];
                octets[0] = type;
                octets[1] = (byte) ((nodeIds.size() >> 8) & 0xff);
                octets[2] = (byte) (nodeIds.size() & 0xff);
                for (int i = 0; i < Math.min(nodeIds.size(), 65535); ++i) {
                    octets[(i * 4) + 3] = (byte) ((nodeIds.get(i) >> 24) & 0xff);
                    octets[(i * 4) + 4] = (byte) ((nodeIds.get(i) >> 16) & 0xff);
                    octets[(i * 4) + 5] = (byte) ((nodeIds.get(i) >> 8) & 0xff);
                    octets[(i * 4) + 6] = (byte) (nodeIds.get(i) & 0xff);
                }
                break;
            case NODE_LEAVE:
                octets = new byte[5];
                octets[0] = type;
                // Put ID of sender
                octets[1] = (byte) ((this.senderId >> 24) & 0xff);
                octets[2] = (byte) ((this.senderId >> 16) & 0xff);
                octets[3] = (byte) ((this.senderId >> 8) & 0xff);
                octets[4] = (byte) (this.senderId & 0xff);
                break;
            case INVALID:
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("ChatMessage.getRawBytes(): UTF-8 not supported.");
            octets = new byte[1];
            octets[0] = (byte) ChatMessageType.INVALID.ordinal();
        }
        return octets;
    }

    /**
     * Gets the type of this ChatMessage
     * 
     * @return Type of message
     */
    public ChatMessageType getType() {
        return type;
    }

    /**
     * Used in TOPIC_LIST messages, this gets the list of topics attached to the
     * message, or an empty list if none is attached.
     * 
     * @return List of topics
     */
    public ArrayList<Topic> getTopics() {
        if ( topics == null ) {
            topics = new ArrayList<Topic>();
        }
        return topics;
    }

    /**
     * Used in NEW_TOPIC, DELETE_TOPIC, JOIN_TOPIC, LEAVE_TOPIC, SUBSCRIBER_LIST
     * and TOPIC_IN_USE messages, this gets the attached single topic
     * 
     * @return Topic
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * Used in NODE_LIST, this gets the list of all node IDs received.
     * 
     * @return List of node IDs
     */
    public ArrayList<Integer> getNodeList() {
        return nodeIds;
    }

    /**
     * Used in SUBSCRIBER_LIST messages, this gets the list of subscribers to
     * the topic.
     * 
     * @return List of subscribers
     */
    public ArrayList<Subscriber> getSubscribers() {
        return subscribers;
    }

    /**
     * Used in SEND_MESSAGE messages, this gets the message content that was
     * sent.
     * 
     * @return Message content
     */
    public String getMessage() {
        return message;
    }

    /**
     * Used in SEND_MESSAGE messages, this gets the senderID of the subscriber
     * that sent this message.
     * 
     * @return Message sender's ID
     */
    public long getSenderId() {
        return this.senderId;
    }

    /**
     * Used in JOIN_TOPIC messages, this gets the sender's username.
     * 
     * @return Username of the sender
     */
    public String getSenderUserName() {
        return this.senderUserName;
    }

    /**
     * Gets the time the message was received.
     * 
     * @return UNIX timestamp when message was received
     */
    public double getTimeReceived() {
        return timeReceived;
    }

    // ----------------
    // Private methods
    // ----------------

    /**
     * Extracts a topic from the given byte array starting from
     * accumulatedOffset. Each topic is packed as follows: <br>
     * <table border=1>
     * <tr>
     * <td>size(B):</td>
     * <td>1</td>
     * <td>name_length</td>
     * </tr>
     * <tr>
     * <td>content:</td>
     * <td>name_length</td>
     * <td>name</td>
     * </tr>
     * </table>
     * As the data is extracted, accumulatedOffset is stepped along, and it
     * will, on return, contain the offset of the last already read byte.
     * 
     * @param octets
     *            array to extract from
     * @return New Topic object with the extracted name and address, no
     *         subscribers. May return null if the address received is invalid.
     * @throws UnsupportedEncodingException
     *             if UTF-8 is not supported
     */
    private Topic extractTopic(byte[] octets) throws UnsupportedEncodingException {
        // Grab name
        int end = (octets[this.accumulatedOffset++] & 0xff) + this.accumulatedOffset;
        byte[] bytes = new byte[end - this.accumulatedOffset];
        for (int i = 0; this.accumulatedOffset < end; ++this.accumulatedOffset, ++i) {
            bytes[i] = octets[this.accumulatedOffset];
        }
        return new Topic(new String(bytes, "UTF-8"));
    }

    /**
     * Extracts a subscriber from the given byte array starting from
     * accumulatedOffset. Each subscriber is packed as follows: <br>
     * <table border=1>
     * <tr>
     * <td>size(B):</td>
     * <td>4</td>
     * <td>1</td>
     * <td>name_length</td>
     * </tr>
     * <tr>
     * <td>content:</td>
     * <td>id</td>
     * <td>name_length</td>
     * <td>name</td>
     * </tr>
     * </table>
     * As the data is extracted, accumulatedOffset is stepped along, and it
     * will, on return, contain the offset of the last already read byte.
     * 
     * @param octets
     *            array to extract from
     * @return New Subscriber object with the extracted name and id
     * @throws UnsupportedEncodingException
     *             if UTF-8 is not supported
     */
    private Subscriber extractSubscriber(byte[] octets) throws UnsupportedEncodingException {
        // Grab ID
        long id = 0x0000000FFFFFFFFL & (long) ByteBuffer.wrap(octets).getInt(this.accumulatedOffset);
        this.accumulatedOffset += 4;
        // Grab name
        int end = (octets[this.accumulatedOffset++] & 0xff) + this.accumulatedOffset;
        byte[] bytes = new byte[end - this.accumulatedOffset];
        for (int i = 0; this.accumulatedOffset < end; ++this.accumulatedOffset, ++i) {
            bytes[i] = octets[this.accumulatedOffset];
        }
        --this.accumulatedOffset;
        return new Subscriber(id, new String(bytes, "UTF-8"));
    }

    /**
     * Packs the given topic into the given byte array, starting at the location
     * pointed to by accumulatedOffset. Each topic is packed as follows: <br>
     * <table border=1>
     * <tr>
     * <td>size(B):</td>
     * <td>1</td>
     * <td>name_length</td>
     * </tr>
     * <tr>
     * <td>content:</td>
     * <td>name_length</td>
     * <td>name</td>
     * </tr>
     * </table>
     * accumulatedOffset is stepped along as the array is packed, and is left
     * pointing to the last location we have written to.
     * 
     * @param octets
     *            Byte array to pack into
     * @param topic
     *            Topic to pack
     * @throws UnsupportedEncodingException
     *             If UTF-8 is not supported
     */
    private void packTopic(byte[] octets, Topic topic) throws UnsupportedEncodingException {
        octets[this.accumulatedOffset++] = (byte) (topic.getName().getBytes("UTF-8").length & 0xff);
        int end = this.accumulatedOffset + topic.getName().getBytes("UTF-8").length;
        for (int i = 0; this.accumulatedOffset < end; ++this.accumulatedOffset, ++i) {
            octets[this.accumulatedOffset] = topic.getName().getBytes("UTF-8")[i];
        }
    }

    /**
     * Packs a subscriber into the given byte array starting from
     * accumulatedOffset. Each subscriber is packed as follows: <br>
     * <table border=1>
     * <tr>
     * <td>size(B):</td>
     * <td>4</td>
     * <td>1</td>
     * <td>name_length</td>
     * <td>
     * </tr>
     * <tr>
     * <td>content:</td>
     * <td>id</td>
     * <td>name_length</td>
     * <td>name</td>
     * </tr>
     * </table>
     * accumulatedOffset is stepped along as the array is packed, and is left
     * pointing to the last location written to.
     * 
     * @param octets
     *            array to pack into from
     * @param subscriber
     *            The subscriber instance to pack
     * @throws UnsupportedEncodingException
     *             If UTF-8 is not supported
     */
    private void packSubscriber(byte[] octets, Subscriber subscriber) throws UnsupportedEncodingException {
        // Set ID
        octets[this.accumulatedOffset++] = (byte) ((subscriber.getNodeId() >> 24) & 0xFF);
        octets[this.accumulatedOffset++] = (byte) ((subscriber.getNodeId() >> 16) & 0xFF);
        octets[this.accumulatedOffset++] = (byte) ((subscriber.getNodeId() >> 8) & 0xFF);
        octets[this.accumulatedOffset++] = (byte) (subscriber.getNodeId() & 0xFF);
        // Set name
        octets[this.accumulatedOffset++] = (byte) (subscriber.getUserName().getBytes("UTF-8").length & 0xff);
        int end = this.accumulatedOffset + (subscriber.getUserName().getBytes("UTF-8").length & 0xff);
        for (int i = 0; this.accumulatedOffset < end; ++this.accumulatedOffset, ++i) {
            octets[this.accumulatedOffset] = subscriber.getUserName().getBytes("UTF-8")[i];
        }
    }

    /**
     * Empty constructor is made private.
     * 
     * @param type
     *            of message to create
     */
    private ChatMessage(ChatMessageType type) {
        this.type = type;
    }

    @Override
    public String toString() {

        switch ( this.type ) {
        case JOIN_TOPIC:
            return " (" + this.type + " | " + this.topic.getName() + " | " + this.senderUserName + " )";
        case GET_TOPICS:
            return "(" + this.type + " )";
        case NEW_TOPIC:
        case DELETE_TOPIC_SUCCESS:
        case DELETE_TOPIC_QUERY:
        case LEAVE_TOPIC:
        case TOPIC_IN_USE:
            return "( " + this.type + " | " + this.topic.getName() + " )";
        case TOPIC_LIST:
            String out = "( " + this.type + " | " + this.topics.size();
            for (Topic t : this.topics) {
                out += " [" + t.getName() + "]";
            }
            out += " )";
            return out;
        case SUBSCRIBER_LIST:
            String out2 = "( " + this.type + " | " + this.subscribers.size();
            for (Subscriber s : this.subscribers) {
                out2 += " [" + s.getNodeId() + "," + s.getUserName() + "]";
            }
            out2 += " )";
            return out2;
        case SEND_MESSAGE:
            return "( " + this.type + " | " + this.senderId + " | " + this.message + " )";
        case NODE_LIST:
            String out3 = "( " + this.type + " | " + this.nodeIds.size();
            for (int id : this.nodeIds) {
                out3 += " [" + id + "] ";
            }
            out3 += " )";
            return out3;
        case NODE_LEAVE:
            return "( " + this.type + " | " + this.senderId + " )";
        case INVALID:
        default:
            return "( INVALID )";
        }
    }

    // ---------------
    // Static methods
    // ---------------

    /**
     * Creates a GET_TOPICS message
     */
    static public ChatMessage createGetTopicsMessage() {
        return new ChatMessage(ChatMessageType.GET_TOPICS);
    }

    /**
     * Creates a JOIN_TOPIC message
     * 
     * @param topic
     *            Topic to join
     * @param username
     *            Username of user joining
     */
    static public ChatMessage createJoinTopicMessage(Topic topic, String username) {
        ChatMessage message = new ChatMessage(ChatMessageType.JOIN_TOPIC);
        message.topic = topic;
        message.senderUserName = username;
        return message;
    }

    /**
     * Creates a NEW_TOPIC message
     * 
     * @param topic
     *            Topic to create
     */
    static public ChatMessage createNewTopicMessage(Topic topic) {
        ChatMessage message = new ChatMessage(ChatMessageType.NEW_TOPIC);
        message.topic = topic;
        return message;
    }

    /**
     * Creates a DELETE_TOPIC_QUERY message
     * 
     * @param topic
     *            Topic to delete
     */
    static public ChatMessage createDeleteTopicQueryMessage(Topic topic) {
        ChatMessage message = new ChatMessage(ChatMessageType.DELETE_TOPIC_QUERY);
        message.topic = topic;
        return message;
    }

    /**
     * Creates a DELETE_TOPIC_SUCCESS message
     * 
     * @param topic
     *            Topic to delete
     */
    static public ChatMessage createDeleteTopicSuccessMessage(Topic topic) {
        ChatMessage message = new ChatMessage(ChatMessageType.DELETE_TOPIC_SUCCESS);
        message.topic = topic;
        return message;
    }

    /**
     * Creates a LEAVE_TOPIC message
     * 
     * @param topic
     *            Topic to leave
     */
    static public ChatMessage createLeaveTopicMessage(Topic topic) {
        ChatMessage message = new ChatMessage(ChatMessageType.LEAVE_TOPIC);
        message.topic = topic;
        return message;
    }

    /**
     * Creates a TOPIC_IN_USE message
     * 
     * @param topic
     *            Topic attempted deleted
     */
    static public ChatMessage createTopicInUseMessage(Topic topic) {
        ChatMessage message = new ChatMessage(ChatMessageType.TOPIC_IN_USE);
        message.topic = topic;
        return message;
    }

    /**
     * Creates a SEND_MESSAGE message
     * 
     * @param message
     *            to send
     * @param topic
     *            to send to
     */
    static public ChatMessage createSendMessageMessage(Message message, Topic topic) {
        ChatMessage msg = new ChatMessage(ChatMessageType.SEND_MESSAGE);
        msg.senderId = message.getSender().getNodeId();
        msg.message = message.getContent();
        msg.topic = topic;
        return msg;
    }

    /**
     * Creates a SUBSCRIBER_LIST message
     * 
     * @param topic
     *            topic for which this list goes
     * @param subscriberList
     *            list of subscribers to send
     */
    static public ChatMessage createSubscriberListMessage(Topic topic, ArrayList<Subscriber> subscriberList) {
        ChatMessage msg = new ChatMessage(ChatMessageType.SUBSCRIBER_LIST);
        msg.subscribers = subscriberList;
        msg.topic = topic;
        return msg;
    }

    /**
     * Creates a TOPIC_LIST message
     * 
     * @param topicList
     *            list of topics to send
     */
    static public ChatMessage createTopicListMessage(ArrayList<Topic> topicList) {
        ChatMessage message = new ChatMessage(ChatMessageType.TOPIC_LIST);
        message.topics = topicList;
        return message;
    }

    /**
     * Creates a NODE_LIST message
     * 
     * @param nodeIds
     *            of all nodes connected.
     */
    static public ChatMessage createNodeIdListMessage(ArrayList<Integer> nodeIds) {
        if ( nodeIds.size() > 65535 ) {
            System.out.println("ChatMessage.createNodeIDList(): Too many node IDs "
                    + "(> 65535), split up the array into multiple messages!");
        }
        ChatMessage message = new ChatMessage(ChatMessageType.NODE_LIST);
        message.nodeIds = nodeIds;
        return message;
    }

    /**
     * Creates a NODE_LEAVE message
     * 
     * @param nodeId
     *            of sender
     */
    static public ChatMessage createNodeLeaveMessage(long nodeId) {
        ChatMessage message = new ChatMessage(ChatMessageType.NODE_LEAVE);
        message.senderId = nodeId;
        return message;
    }
}
