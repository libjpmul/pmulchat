package no.ntnu.acp142.chatapp;

import no.ntnu.acp142.Libjpmul;
import no.ntnu.acp142.Acp142Message;
import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.ILibjpmul;
import no.ntnu.acp142.configui.ConfigurationModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/*
 * Copyright (c) 2013, Thomas Martin Schmid, Luka Cetusic
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
 * Class handling all the network interaction.
 * 
 * @author Thomas Martin Schmid
 */
public class Networking implements PropertyChangeListener {

    /**
     * Instance of the libjpmul library interface used for communication
     */
    private ILibjpmul                            libjpmul;
    /**
     * Normal multicast socket used to coordinate node IDs.
     */
    private MulticastSocket                    broadcastSocket;
    /**
     * TopicModel reference
     */
    private TopicModel                         topicModel;
    /**
     * ChatModel reference
     */
    private final ChatModel                    chatModel;
    /**
     * Boolean used to stop the listening thread
     */
    private boolean                            isRunning;
    /**
     * As long as this is true, we attempt to send the initial messages in
     * listenBroadcast.
     */
    private boolean                            hasReceivedNodeList;

    /**
     * Queue of all the topics currently awaiting their timeout to expire so
     * they can be deleted.
     */
    private ConcurrentLinkedDeque<Topic>       attemptedDeletedTopicsAwaitingTimeout;

    /**
     * Contains all messages of type TOPIC_LIST and SUBSCRIBER_LIST for
     * ChatConfigurationModel.maxWaitForRespond for delayedConditionalSend.
     */
    private ConcurrentLinkedDeque<ChatMessage> notYetExpiredResponsesSeenFromOthers;

    /**
     * Contains the node IDs of all nodes listening on broadcast group.
     */
    private List<Integer>                      destinations;

    /**
     * Node ID of this node. We store this in the initialize method since it
     * <b>should</b> be static and storing it will enable us to debug this class
     * on a single computer.
     */
    private int                                nodeId;
    /**
     * Keeps track of whether we are in EMCON or not. Only used on startup to
     * silence our initial non-libjpmul transmission.
     */
    private boolean                            isInEmcon;

    /**
     * Creates the class. Does nothing but store references and setup.
     * 
     * @param chatModel
     *            reference.
     */
    public Networking(ChatModel chatModel) {
        notYetExpiredResponsesSeenFromOthers = new ConcurrentLinkedDeque<ChatMessage>();
        attemptedDeletedTopicsAwaitingTimeout = new ConcurrentLinkedDeque<Topic>();
        destinations = Collections.synchronizedList(new ArrayList<Integer>());
        this.isRunning = true;
        this.hasReceivedNodeList = false;
        this.chatModel = chatModel;
        this.isInEmcon = false;
    }

    /**
     * Starts the libjpmul interface, then starts the listening thread.
     * 
     * @param topicModel
     *            reference to use.
     * @throws IOException
     *             If the Broadcast socket cannot be bound.
     */
    public void initialize(TopicModel topicModel, ChatConfigurationModel chatConfigModel) throws IOException {
        int id = Configuration.getNodeId(); // Grab this before we start
                                            // libjpmul
        initialize(topicModel, new Libjpmul(), new MulticastSocket(ChatConfigurationModel.getBroadcastPort()), id,
                chatConfigModel);
    }

    /**
     * Starts the listening thread. This version takes in the ILibjpmul interface
     * and broadcast socket to use, which makes it usable for testing and
     * debugging, or if other implementations are wanted. It is used by the
     * default initialize, after creating the interface in that method.
     * 
     * @param topicModel
     *            reference to use.
     * @param acpInterface
     *            to use
     * @param broadcastSocket
     *            to use
     * @param nodeId
     *            of this node
     * @param chatConfigModel
     *            reference to listen on for changes to Node ID
     * @throws IOException
     *             If the Broadcast socket cannot be bound.
     */
    public void initialize(TopicModel topicModel, ILibjpmul acpInterface, MulticastSocket broadcastSocket, int nodeId,
            ChatConfigurationModel chatConfigModel) throws IOException {
        // Listen on configModel for changes to Node ID
        chatConfigModel.addPropertyChangeListener(this);

        if ( nodeId == 0 ) { // If it was not set manually, grab it after
                             // Libjpmul
                             // init.
            nodeId = Configuration.getNodeId();
        }
        this.nodeId = nodeId;
        this.topicModel = topicModel;
        // Start the sockets
        this.broadcastSocket = broadcastSocket;
        this.broadcastSocket.setInterface(Configuration.getBindInterfaceAddress());
        this.broadcastSocket.joinGroup(ChatConfigurationModel.getBroadcastGroup());
        this.libjpmul = acpInterface;

        // Now start the two listening threads. (the first of which is only
        // started with dynamic MC groups)
        if ( ChatConfigurationModel.useDynamicMulticast() ) {
            new Thread() {
                public void run() {
                    listenBroadcast();
                }
            }.start();
        }
        new Thread() {
            public void run() {
                listenLibjpmul();
            }
        }.start();

        // And finally add the shutdown hook to stop the threads in case of
        // crashes/interrupts.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * Stops the listening threads.
     */
    public void shutdown() {
        if ( ChatConfigurationModel.useDynamicMulticast() ) {
            // Tell others to stop sending to us if dynamic multicast is in use
            Acp142Message message = new Acp142Message();
            ChatMessage msg = ChatMessage.createNodeLeaveMessage(this.nodeId);
            message.setData(msg.getRawMessage());
            ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
            message.setDestinations(dests);
            message.setDynamic(ChatConfigurationModel.useDynamicMulticast());
            message.setSourceID(this.nodeId);
            /*
             * When this is called the user wants to shut down the app. This
             * means, that even though we would like to tell everyone that we
             * have left, we cannot wait that long. Thus, the 3s expiry time
             * should be enough to warn off some nodes. Not being informed just
             * means more useless traffic from other nodes, and is not tragic,
             * although undesired.
             */
            message.setExpiryTime((System.currentTimeMillis() / 1000) + 3);
            message.setPersistent(false);
            libjpmul.send(message);
        }
        // Then stop the threads.
        this.isRunning = false;
    }

    /**
     * Listens for incoming messages on the libjpmul sockets, then handles them.
     * For each of the types given, this table explains the action taken:
     * <table border=1>
     * <tr>
     * <td>ChatMessageType</td>
     * <td>Action taken</td>
     * </tr>
     * <tr>
     * <td>LEAVE_TOPIC</td>
     * <td>If the message affects the chatModel.currentChat, the subscriber is
     * removed from the list of subscribers for this topic.</td>
     * </tr>
     * <tr>
     * <td>SUBSCRIBER_LIST</td>
     * <td>If the topic affected is chatModel.currentChat, the subscriber list
     * is populated.</td>
     * </tr>
     * <tr>
     * <td>SEND_MESSAGE</td>
     * <td>If the topic affected is chatModel.currentChat, the message is
     * inserted into the chat.</td>
     * </tr>
     * <tr>
     * <td>DELETE_TOPIC_QUERY</td>
     * <td>If we are listening to the attempted deleted topic, we respond with a
     * TOPIC_IN_USE message immediately.</td>
     * </tr>
     * <tr>
     * <td>DELETE_TOPIC_SUCCESS</td>
     * <td>The topic is deleted from our topicModel</td>
     * </tr>
     * <tr>
     * <td>JOIN_TOPIC</td>
     * <td>If the message affects the chatModel.currentChat, the subscriber is
     * added to our list of subscribers for this topic. Delayed respond with a
     * list of all subscribers to that topic.</td>
     * </tr>
     * <tr>
     * <td>TOPIC_LIST</td>
     * <td>topicModel's topicList is populated with the these topics.</td>
     * </tr>
     * <tr>
     * <td>TOPIC_IN_USE</td>
     * <td>If the topic is in attemptedDeletedTopicsAwaitingTimeout, it is
     * removed from there.</td>
     * </tr>
     * <tr>
     * <td>GET_TOPICS</td>
     * <td>A delayed response with all topics we are aware of is sent.</td>
     * </tr>
     * <tr>
     * <td>NEW_TOPIC</td>
     * <td>If the topic does not already exist, we add it to our list. Otherwise
     * we delayed respond with a SUBSCRIBER_LIST message for that topic.</td>
     * </tr>
     * <tr>
     * <td>NODE_LIST</td>
     * <td>Adds the received node IDs to the list of destinations. Used to
     * populate our destination list on startup.</td>
     * </tr>
     * </table>
     * For the workings of delayed responses, see
     * Networking.delayedConditionalSend
     */
    public void listenLibjpmul() {
        System.out.println("Started listenLibjpmul thread");
        boolean notSentInitialGetTopic = true;

        while (this.isRunning) {
            Acp142Message message = this.libjpmul.receive();
            if ( message == null ) {
                System.out.println("Networking.listenLibjpmul(): libjpmul message reception timed out.");
                continue;
            }
            ChatMessage chatMessage = new ChatMessage(message.getData());

            // If we are the sender, skip it.
            if ( message.getSourceID() == this.chatModel.getCurrentChat().getSelf().getNodeId() ) {
                continue;
            }

            Topic topic;
            // Handle incoming messages
            switch ( chatMessage.getType() ) {
            case LEAVE_TOPIC:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                if ( this.chatModel.getCurrentChat().getTopic().getName().equals(chatMessage.getTopic().getName()) ) {
                    Subscriber subscriber = this.chatModel.getCurrentChat().getTopic()
                            .removeSubscriber(chatMessage.getSenderId());
                    if ( subscriber != null ) {
                        this.chatModel.getCurrentChat().addMessage(
                                new Message(new Subscriber(0, ChatModel.getTimeAsString()), "User '"
                                        + subscriber.getUserName() + "' left channel."));
                    }
                } else {
                    // Log the leave
                    topic = null;
                    for (Topic t : this.topicModel.getTopicList()) {
                        if ( t.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                            topic = t;
                            break;
                        }
                    }
                    if ( topic != null ) {
                        Chat chat = null;
                        for (Chat c : this.chatModel.getChatList()) {
                            if ( c.getTopic() == topic ) {
                                chat = c;
                                break;
                            }
                        }
                        if ( chat == null ) {
                            // The chat has not yet been created in chatmodel.
                            // Create it here.
                            chat = new Chat(topic, this.chatModel.getCurrentChat().getSelf());
                            this.chatModel.getChatList().add(chat);
                        }
                        Subscriber sub = topic.removeSubscriber(chatMessage.getSenderId());
                        if ( sub != null ) {
                            chat.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), "User '"
                                    + sub.getUserName() + "' left channel."));
                        }
                    }
                }
                break;
            case SEND_MESSAGE:
                // If we have received it it is meant for us
                // This means, if we are in static multicast mode, it may be to
                // any topic
                // but in dynamic mode it must be the active one (or one we just
                // left).
                topic = null;
                for (Topic t : this.topicModel.getTopicList()) {
                    if ( t.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                        topic = t;
                        break;
                    }
                }
                if ( topic != null ) {
                    // Check if it is sent to the active chat
                    if ( topic == this.chatModel.getCurrentChat().getTopic() ) {
                        ArrayList<Subscriber> list = topic.getSubscriberList();
                        for (Subscriber s : list) {
                            if ( s.getNodeId() == chatMessage.getSenderId() ) {
                                Message messageToSend = new Message(s, chatMessage.getMessage());
                                this.chatModel.addMessage(messageToSend);
                                break;
                            }
                        }
                    } else {
                        // Message meant for an inactive chat, find it
                        Chat chat = null;
                        for (Chat c : this.chatModel.getChatList()) {
                            if ( c.getTopic() == topic ) {
                                chat = c;
                                break;
                            }
                        }
                        if ( chat == null ) {
                            // The chat has not yet been created in chatmodel.
                            // Create it here.
                            chat = new Chat(topic, this.chatModel.getCurrentChat().getSelf());
                            this.chatModel.getChatList().add(chat);
                        }
                        ArrayList<Subscriber> list = topic.getSubscriberList();
                        for (Subscriber s : list) {
                            if ( s.getNodeId() == chatMessage.getSenderId() ) {
                                Message messageToSend = new Message(s, chatMessage.getMessage());
                                chat.addMessage(messageToSend);
                                break;
                            }
                        }
                    }
                }
                break;
            case SUBSCRIBER_LIST:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                if ( this.chatModel.getCurrentChat().getTopic().getName().toLowerCase()
                        .equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                    String sublist = "Users seen in channel at this time:\n";
                    for (Subscriber s : chatMessage.getSubscribers()) {
                        this.chatModel.getCurrentChat().getTopic().addSubscriber(s);
                        sublist += "'" + s.getUserName() + "', ";
                    }
                    sublist = sublist.substring(0, sublist.length() - 2);
                    this.chatModel.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), sublist));
                }
                this.notYetExpiredResponsesSeenFromOthers.add(chatMessage);
                break;
            case DELETE_TOPIC_QUERY:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                Topic dtopic = this.chatModel.getCurrentChat().getTopic();
                if ( dtopic != null
                        && dtopic.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                    // We are in the chat being deleted, respond!
                    ChatMessage response = ChatMessage.createTopicInUseMessage(chatMessage.getTopic());
                    final Acp142Message aResp = new Acp142Message();
                    aResp.setData(response.getRawMessage());
                    ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
                    aResp.setDestinations(dests);
                    aResp.setDynamic(ChatConfigurationModel.useDynamicMulticast());
                    aResp.setExpiryTime((System.currentTimeMillis() / 1000)
                            + ChatConfigurationModel.getDefaultTimeToLive());
                    aResp.setSourceID(this.nodeId);
                    aResp.setPersistent(ChatConfigurationModel.useDynamicMulticast()
                            && ChatConfigurationModel.usePersistantGroups());
                    new Thread() {
                        public void run() {
                            libjpmul.send(aResp);
                        }
                    }.start();
                }
                break;
            case DELETE_TOPIC_SUCCESS:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                this.topicModel.removeTopic(chatMessage.getTopic());
                break;
            case GET_TOPICS:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                ArrayList<Topic> listCopy = new ArrayList<Topic>(this.topicModel.getTopicList());
                delayedConditionalSend(ChatMessage.createTopicListMessage(listCopy));
                break;
            case JOIN_TOPIC:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                topic = null;
                for (Topic t : this.topicModel.getTopicList()) {
                    if ( t.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                        topic = t;
                        break;
                    }
                }
                if ( topic != null ) {
                    // Add the subscriber
                    Subscriber subscriber = new Subscriber(message.getSourceID(), chatMessage.getSenderUserName());
                    topic.addSubscriber(subscriber);

                    if ( this.chatModel.getCurrentChat().getTopic() == topic ) {
                        // If we are in the chat, respond with the subscriber
                        // list
                        delayedConditionalSend(ChatMessage
                                .createSubscriberListMessage(topic, topic.getSubscriberList()));
                    }
                    // Now log that the user joined
                    Chat chat = null;
                    for (Chat c : this.chatModel.getChatList()) {
                        if ( c.getTopic() == topic ) {
                            chat = c;
                            break;
                        }
                    }
                    if ( chat == null ) {
                        // The chat has not yet been created in chatmodel.
                        // Create it here.
                        chat = new Chat(topic, this.chatModel.getCurrentChat().getSelf());
                        this.chatModel.getChatList().add(chat);
                    }
                    if ( chat == this.chatModel.getCurrentChat() ) {
                        // This call includes UI updates
                        this.chatModel.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), "User '"
                                + subscriber.getUserName() + "' joined channel."));
                    } else {
                        chat.addMessage(new Message(new Subscriber(0, ChatModel.getTimeAsString()), "User '"
                                + subscriber.getUserName() + "' joined channel."));
                    }
                }
                break;
            case NEW_TOPIC:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                // Check to see if the topic already exists
                for (Topic t : this.topicModel.getTopicList()) {
                    if ( t.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                        // If it does, then delayed respond with the subscriber
                        // list.
                        delayedConditionalSend(ChatMessage.createSubscriberListMessage(t, t.getSubscriberList()));
                    }
                }
                // Then add it to our model. The model will ignore duplicates.
                this.topicModel.addExistingTopic(chatMessage.getTopic());
                break;
            case TOPIC_IN_USE:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                for (Topic t : this.attemptedDeletedTopicsAwaitingTimeout) {
                    if ( t.getName().toLowerCase().equals(chatMessage.getTopic().getName().toLowerCase()) ) {
                        this.attemptedDeletedTopicsAwaitingTimeout.remove(t);
                        break;
                    }
                }
                break;
            case TOPIC_LIST:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                // Early break if we don't support dynamic topics
                if ( !ChatConfigurationModel.useDynamicTopics() ) {
                    break;
                }
                for (Topic t : chatMessage.getTopics()) {
                    if ( !this.attemptedDeletedTopicsAwaitingTimeout.contains(t) ) {
                        // Don't add topics we are trying to delete!
                        this.topicModel.addExistingTopic(t);
                    }
                }
                this.notYetExpiredResponsesSeenFromOthers.add(chatMessage);
                break;
            case NODE_LIST:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                this.hasReceivedNodeList = true; // Note that we have received
                                                 // it
                for (int id : chatMessage.getNodeList()) {
                    addId(id);
                }
                // We only get NODE_LIST as a response to the broadcast packet
                // we send on startup, so after receiving this, we send a
                // GET_TOPICS once!
                if ( notSentInitialGetTopic ) {
                    final Acp142Message msg = new Acp142Message();
                    ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
                    msg.setDestinations(dests);
                    msg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
                    msg.setExpiryTime((System.currentTimeMillis() / 1000L)
                            + ChatConfigurationModel.getDefaultTimeToLive());
                    msg.setSourceID(this.nodeId);
                    ChatMessage cmsg = ChatMessage.createGetTopicsMessage();
                    msg.setData(cmsg.getRawMessage());
                    msg.setPersistent(false);
                    new Thread() {
                        public void run() {
                            libjpmul.send(msg);
                        }
                    }.start();
                    notSentInitialGetTopic = false;
                }
                break;
            case NODE_LEAVE:
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    break; // Does not concern us if static MC groups are used.
                }
                removeId((int) chatMessage.getSenderId());
                break;
            default:
                System.out.println("Networking.listenLibjpmul(): Unknown message type '" + chatMessage.getType() + "'.");
                break;

            }
        }
        System.out.println("Stopped listenLibjpmul thread");
    }

    /**
     * Listens for incoming messages on our broadcastSocket, then handles them.
     * 
     * For the workings of delayed responses, see
     * Networking.delayedConditionalSend
     * 
     * Is not started if static multicast groups are used.
     */
    public void listenBroadcast() {
        // First make sure that we wait until we are out of EMCON to do
        // anything.
        while (this.isInEmcon) {
            // Sleep half a second
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Networking.listenLibjpmul(): Could not sleep while in EMCON");
                e.printStackTrace();
            }
        }

        System.out.println("Started listenBroadcast thread");

        new Thread() {
            public void run() {
                InetSocketAddress addr;
                MulticastSocket sendSocket;
                try {
                    addr = new InetSocketAddress(Configuration.getBindInterfaceAddress(),
                            ConfigurationModel.getBroadcastPort());
                    sendSocket = new MulticastSocket(addr);
                } catch (IOException e1) {
                    System.out.println("Could not bind to broadcast send socket in Networking.");
                    return;
                }
                while (!hasReceivedNodeList) {
                    // Start by sending out our node id to get the destination
                    // list!
                    DatagramPacket outMessage = new DatagramPacket(new byte[4], 4);
                    outMessage.getData()[0] = (byte) ((nodeId >> 24) & 0xff);
                    outMessage.getData()[1] = (byte) ((nodeId >> 16) & 0xff);
                    outMessage.getData()[2] = (byte) ((nodeId >> 8) & 0xff);
                    outMessage.getData()[3] = (byte) (nodeId & 0xff);
                    outMessage.setAddress(ChatConfigurationModel.getBroadcastGroup());
                    outMessage.setPort(ChatConfigurationModel.getBroadcastPort());
                    try {
                        sendSocket.send(outMessage);
                    } catch (IOException e) {
                        System.out.println("Networking.listenBroadcast(): Could not ask for destinations, exception:");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        System.out.println("Networking.listenLibjpmul(): Could not sleep");
                        e.printStackTrace();
                    }
                }
                sendSocket.close();
            }
        }.start();

        // Now listen for incoming messages
        byte[] data = new byte[1024];
        while (this.isRunning) {
            DatagramPacket message = new DatagramPacket(data, data.length);
            try {
                this.broadcastSocket.receive(message);
            } catch (IOException e) {
                if ( e instanceof SocketTimeoutException ) {
                    System.out.println("Networking.listenBroadcast(): Message receive timed out.");
                } else {
                    System.out.println("Networking.listenBroadcast(): Error receiving message:");
                    e.printStackTrace();
                }
                continue;
            }
            // Check for edge cases.
            if ( message.getData() == null || message.getData().length < 4 ) {
                continue;
            }
            // Handle incoming messages
            int id = ((message.getData()[0] & 0xff) << 24) + ((message.getData()[1] & 0xff) << 16)
                    + ((message.getData()[2] & 0xff) << 8) + (message.getData()[3] & 0xff);
            // Check if it is from ourselves, if so, skip it
            if ( id == this.nodeId || id == 0 ) {
                continue;
            }
            // Add ourselves to the list to respond with
            ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
            dests.add(this.nodeId);
            // Add this new id to our local list
            addId(id);
            // Answer
            ChatMessage msg = ChatMessage.createNodeIdListMessage(dests);
            final Acp142Message aResp = new Acp142Message();
            aResp.setData(msg.getRawMessage());
            ArrayList<Integer> dest = new ArrayList<Integer>();
            dest.add(id);
            aResp.setDestinations(dest);
            aResp.setDynamic(ChatConfigurationModel.useDynamicMulticast());
            aResp.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
            aResp.setSourceID(this.nodeId);
            aResp.setPersistent(false);
            new Thread() {
                public void start() {
                    libjpmul.send(aResp);
                }
            }.start();
        }
    }

    /**
     * Changes topic from the given old one to the given new one. Alerts the
     * network that it has left a topic and joined a new one.
     * 
     * @param newTopic
     *            Topic to join
     */
    public void changeTopic(Topic newTopic) {
        if ( !ChatConfigurationModel.useDynamicMulticast() ) {
            return; // If static multicast is used, we don't actually transmit
                    // anything here.
        }
        // Notify old topic
        Topic oldTopic = this.chatModel.getCurrentChat().getTopic();
        if ( oldTopic != null ) {
            // Is null in the initial non-topic topic.
            ChatMessage message = ChatMessage.createLeaveTopicMessage(oldTopic);
            final Acp142Message amsg = new Acp142Message();
            ArrayList<Integer> destinations = new ArrayList<Integer>();
            for (Subscriber s : oldTopic.getSubscriberList()) {
                if ( s.getNodeId() == this.nodeId ) {
                    continue;
                }
                destinations.add((int) s.getNodeId());
            }
            amsg.setDestinations(destinations);
            amsg.setData(message.getRawMessage());
            amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
            amsg.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
            amsg.setSourceID(this.nodeId);
            amsg.setPersistent(false); // Destroy the multicast groups if there
                                       // is one.
            new Thread() {
                public void run() {
                    libjpmul.send(amsg);
                }
            }.start();
        }

        // Notify new topic (this should trigger someone to respond with
        // subscribers)
        ChatMessage message = ChatMessage.createJoinTopicMessage(newTopic, this.chatModel.getCurrentChat().getSelf()
                .getUserName());
        final Acp142Message amsg = new Acp142Message();
        ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
        amsg.setDestinations(dests);
        amsg.setData(message.getRawMessage());
        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
        amsg.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
        amsg.setSourceID(this.nodeId);
        amsg.setPersistent(false); // Destroy the multicast groups if there is
                                   // one.
        new Thread() {
            public void run() {
                libjpmul.send(amsg);
            }
        }.start();
    }

    /**
     * Creates a new topic. Simply announces it's existence. If it already
     * exists, the announce should trigger someone to respond with the
     * subscriber list.
     * 
     * @param topic
     *            The newly created topic.
     */
    public void createTopic(Topic topic) {
        if ( !ChatConfigurationModel.useDynamicMulticast() ) {
            return; // If static multicast is used, we can't create topics
        }
        // Early break if we don't support dynamic topics
        if ( !ChatConfigurationModel.useDynamicTopics() ) {
            return;
        }
        ChatMessage message = ChatMessage.createNewTopicMessage(topic);
        final Acp142Message amsg = new Acp142Message();
        ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
        amsg.setDestinations(dests);
        amsg.setData(message.getRawMessage());
        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
        amsg.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
        amsg.setSourceID(this.nodeId);
        amsg.setPersistent(ChatConfigurationModel.useDynamicMulticast() && ChatConfigurationModel.usePersistantGroups());
        new Thread() {
            public void run() {
                libjpmul.send(amsg);
            }
        }.start();
    }

    /**
     * Attempts to delete a topic. Announces the wanted delete to the network,
     * then waits for ChatConfigurationModel.waitForInUseResponse before sending
     * a successful delete message. If a TOPIC_IN_USE message is received before
     * the timeout, an exception is thrown.
     * 
     * @param topic
     *            to delete
     */
    public void deleteTopic(final Topic topic) {
        if ( !ChatConfigurationModel.useDynamicMulticast() ) {
            return; // If static multicast is used, we can't delete topics.
        }
        // Early break if we don't support dynamic topics
        if ( !ChatConfigurationModel.useDynamicTopics() ) {
            return;
        }
        // Store the topic in our list
        this.attemptedDeletedTopicsAwaitingTimeout.add(topic);
        // Announce the delete
        ChatMessage message = ChatMessage.createDeleteTopicQueryMessage(topic);
        final Acp142Message amsg = new Acp142Message();
        ArrayList<Integer> dests = new ArrayList<Integer>(this.destinations);
        amsg.setDestinations(dests);
        amsg.setData(message.getRawMessage());
        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
        amsg.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
        amsg.setSourceID(this.nodeId);
        amsg.setPersistent(ChatConfigurationModel.useDynamicMulticast() && ChatConfigurationModel.usePersistantGroups());
        new Thread() {
            public void run() {
                libjpmul.send(amsg);
            }
        }.start();

        // Sleep in a new thread
        new Thread() {
            public void run() {
                // Sleep
                try {
                    sleep((long) (ChatConfigurationModel.getWaitForInUseResponse() * 1000.0));
                } catch (InterruptedException e) {
                    System.out.println("Networking.deleteTopic(): Sleep interrupted:");
                    e.printStackTrace();
                }
                // Check if it is still wanted deleted
                if ( attemptedDeletedTopicsAwaitingTimeout.contains(topic) ) {
                    // Announce the successful delete
                    ChatMessage message = ChatMessage.createDeleteTopicSuccessMessage(topic);
                    final Acp142Message amsg = new Acp142Message();
                    ArrayList<Integer> dests = new ArrayList<Integer>(destinations);
                    amsg.setDestinations(dests);
                    amsg.setData(message.getRawMessage());
                    amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
                    amsg.setExpiryTime((System.currentTimeMillis() / 1000)
                            + ChatConfigurationModel.getDefaultTimeToLive());
                    amsg.setSourceID(nodeId);
                    amsg.setPersistent(ChatConfigurationModel.useDynamicMulticast()
                            && ChatConfigurationModel.usePersistantGroups());
                    new Thread() {
                        public void run() {
                            libjpmul.send(amsg);
                        }
                    }.start();
                    // In case we have gotten a TOPIC_LIST with this topic in is
                    // since we started the deletion, delete the topic again.
                    topicModel.removeTopic(topic);
                } else {
                    // It has been deleted in the meantime, so add it again!
                    topicModel.addExistingTopic(topic);
                }
                // Return
            }
        }.start();
    }

    /**
     * Sends the message given to all subscribers in the current chat.
     * 
     * @param message
     *            to send
     */
    public void sendMessage(Message message) {
        // Then send it
        ChatMessage msg = ChatMessage.createSendMessageMessage(message, this.chatModel.getCurrentChat().getTopic());
        final Acp142Message amsg = new Acp142Message();
        ArrayList<Integer> destinations = new ArrayList<Integer>();
        for (Subscriber s : this.chatModel.getCurrentChat().getTopic().getSubscriberList()) {
            if ( s.getNodeId() == this.nodeId ) {
                continue;
            }
            destinations.add((int) s.getNodeId());
        }
        amsg.setDestinations(destinations);
        amsg.setData(msg.getRawMessage());
        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
        amsg.setSourceID(this.nodeId);
        amsg.setExpiryTime((System.currentTimeMillis() / 1000) + ChatConfigurationModel.getDefaultTimeToLive());
        amsg.setPersistent(ChatConfigurationModel.useDynamicMulticast() && ChatConfigurationModel.usePersistantGroups());
        new Thread() {
            public void run() {
                libjpmul.send(amsg);
            }
        }.start();
    }

    /**
     * This method will start a thread that sleeps some random amount of time in
     * the window [0, ChatConfigurationModel.maxWaitToRespond]. After waking it
     * will check whether someone else has already responded with the same
     * answer, and if not, it will send the given message.
     * 
     * @param message
     *            to send.
     */
    private void delayedConditionalSend(final ChatMessage message) {
        new Thread() {
            public void run() {
                // Sleep
                double oldTime = ((double) System.currentTimeMillis() / (double) 1000);
                long wait = (long) (Math.random() * (double) ChatConfigurationModel
                        .getMaximumWaitForResponseOnDelayedSend());
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    System.out.println("Networking.delayedConditionalSend(): Sleep interrupted:");
                    e.printStackTrace();
                }

                // Check for answers
                double currentTime = (double) (System.currentTimeMillis() / 1000L);
                boolean noAnswerSeen = true;

                for (ChatMessage msg : notYetExpiredResponsesSeenFromOthers) {
                    // Prune messages that are too old anyway
                    if ( msg.getTimeReceived() < currentTime
                            - (double) (ChatConfigurationModel.getMaximumWaitForResponseOnDelayedSend() / 1000L) ) {
                        notYetExpiredResponsesSeenFromOthers.remove(msg);
                        continue;
                    }
                    // For each of the correct type, check if we have seen an
                    // answer
                    if ( msg.getType() == message.getType() ) {
                        if ( msg.getTimeReceived() > oldTime ) {
                            if ( msg.getType() == ChatMessageType.SUBSCRIBER_LIST ) {
                                // Only delete the ones concerning this
                                // topic
                                if ( msg.getTopic().getName().toLowerCase()
                                        .equals(message.getTopic().getName().toLowerCase()) ) {
                                    noAnswerSeen = false;
                                }
                            } else {
                                noAnswerSeen = false;
                            }
                        }
                    }
                }

                // If no answer was seen in the time period of sleep, send an
                // answer
                if ( noAnswerSeen ) {
                    if ( message.getType() == ChatMessageType.SUBSCRIBER_LIST ) {
                        Acp142Message amsg = new Acp142Message();
                        ArrayList<Integer> ldestinations = new ArrayList<Integer>();
                        for (Subscriber s : message.getSubscribers()) {
                            if ( s.getNodeId() == chatModel.getCurrentChat().getSelf().getNodeId() ) {
                                continue;
                            }
                            ldestinations.add((int) s.getNodeId());
                        }
                        amsg.setDestinations(ldestinations);
                        amsg.setData(message.getRawMessage());
                        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
                        amsg.setSourceID(nodeId);
                        amsg.setExpiryTime((System.currentTimeMillis() / 1000)
                                + ChatConfigurationModel.getDefaultTimeToLive());
                        amsg.setPersistent(false); // Delete the old group
                        libjpmul.send(amsg);
                    } else {
                        Acp142Message amsg = new Acp142Message();
                        ArrayList<Integer> dests = new ArrayList<Integer>(destinations);
                        amsg.setDestinations(dests);
                        amsg.setData(message.getRawMessage());
                        amsg.setDynamic(ChatConfigurationModel.useDynamicMulticast());
                        amsg.setSourceID(nodeId);
                        amsg.setExpiryTime((System.currentTimeMillis() / 1000)
                                + ChatConfigurationModel.getDefaultTimeToLive());
                        amsg.setPersistent(false); // Delete the old group
                        libjpmul.send(amsg);
                    }
                }
            }
        }.start();
    }

    /**
     * Adds a node ID to our destinations list given it is not already in the
     * list, and is not our ID.
     * 
     * @param id
     *            to add
     */
    private synchronized void addId(int id) {
        boolean add = id != this.nodeId;
        for (int i : this.destinations) {
            if ( i == id ) {
                add = false;
                break;
            }
        }
        if ( add ) {
            this.destinations.add(id);
        }
    }

    /**
     * Removes a node ID from our destinations list.
     * 
     * @param senderId
     *            to remove
     */
    private synchronized void removeId(int senderId) {
        for (int i = 0; i < this.destinations.size(); ++i) {
            if ( this.destinations.get(i) == senderId ) {
                this.destinations.remove(i);
                break;
            }
        }
    }

    /**
     * Gets the number of nodes we have know are currently online and are
     * broadcasting to.
     * 
     * @return number of online nodes.
     */
    public int getNumberOfNodes() {
        return this.destinations.size();
    }

    /**
     * Sets this class' internal node ID used to transmit. This is called when
     * nodeID is changed in ConfigurationModel so we send with the correct ID,
     * and is needed to allow for single computer debugging of this class.
     * 
     * @param nodeId
     *            new node ID of this node.
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        if ( arg0.getPropertyName().equals(ConfigurationModel.NODE_ID_PROPERTY) ) {
            setNodeId((int) arg0.getNewValue());
        }
    }

    /**
     * Changes the EMCON state in libjpmul. Also tracks this change locally.
     * 
     * @param b
     *            New EMCON state to set.
     */
    public void setEmcon(boolean b) {
        if ( this.libjpmul != null ) {
            if ( b ) {
                this.libjpmul.enterEmcon();
            } else {
                this.libjpmul.leaveEmcon();
            }
        }
        this.isInEmcon = b;
    }
}
