package no.ntnu.acp142.chatapp;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.MulticastGroup;

import javax.naming.NameAlreadyBoundException;
import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * Handles topics; creation, removal, selection and display in list
 * is all handled through this class. Also handles loading from file.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid
 * 
 */
public class TopicModel extends AbstractListModel<Topic> {

    private static PropertyChangeSupport propertyChangeSupport;

    public static final String           CREATE_NEW_TOPIC_PROPERTY          = "CreateNewTopic";
    public static final String           REMOVE_TOPIC_PROPERTY              = "RemoveTopic";
    public static final String           REMOVE_TOPIC_FROM_NETWORK_PROPERTY = "RemoveTopicFromNetwork";
    public static final String           ADD_EXISTING_TOPIC_PROPERTY        = "AddExistingTopic";
    public static final String           TOPIC_LIST_FILTER_UPDATE           = "TopicListFilterUpdate";

    private final List<Topic>            topicList;
    private final List<Topic>            filteredList;

    private final Networking             networking;

    private Topic                        selectedTopic;

    /**
     * Initializes the topicModel.
     * 
     * @param networking
     *            reference to the Networking class object in use.
     * @param topicListFile
     *            path to the file of static topics, or null of none exists.
     */
    public TopicModel(Networking networking, String topicListFile) {
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.networking = networking;
        this.topicList = Collections.synchronizedList(new ArrayList<Topic>());
        // Add all immutable topics
        if ( topicListFile != null ) {
            try {
                parseTopicsFromFile(topicListFile);
            } catch (FileNotFoundException e) {
                System.out.println("Topic.topic(): Could not find topic list file supplied.");
                e.printStackTrace();
            } catch (IOException e) {
                System.out
                        .println("Topic.topic(): Could not close topic list file or UTF-8 is not supported. Exception caught: ");
                e.printStackTrace();
            }
        }

        this.filteredList = Collections.synchronizedList(new ArrayList<Topic>());
        this.selectedTopic = null;
        updateFilteredList();
    }

    /**
     * This creates a new topic if the topic name is not already in use, then
     * notifies the network that the topic was created. If another node already
     * has this topic created, it will return a list of subscribers, which is
     * set if this is the currentChat in ChatModel upon receiving the answer. It
     * then selects the topic with selectTopic.
     * 
     * @param name
     *            of topic to create.
     * @return The new topic.
     * @throws NameAlreadyBoundException
     *             if a topic with the given name already exists.
     */
    public synchronized Topic createNewTopic(String name) throws NameAlreadyBoundException {
        for (Topic t : topicList) {
            if ( t.getName().toLowerCase().equals(name.toLowerCase()) ) {
                throw new NameAlreadyBoundException("TopicModel.createNewTopic(): Topic of name " + name
                        + " already exists.");
            }
        }
        // Make sure name fits into 255 bytes.
        try {
            while (name.getBytes("UTF-8").length > 255) {
                name = name.substring(0, name.length() - 1);
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("Topic.createNewTopic(): UTF-8 not supported!");
            return null;
        }
        Topic topic = new Topic(name);
        topicList.add(topic);
        networking.createTopic(topic);
        filteredList.add(0, topic);
        selectTopic(0);
        propertyChangeSupport.firePropertyChange(CREATE_NEW_TOPIC_PROPERTY, null, topic);
        return topic;
    }

    /**
     * Removes the topic at the index given. Called from the front end. This
     * negotiates the deletion over the network before deletion. Fails to delete
     * the topic if the network negotiations fail.
     * 
     * @param index
     *            of topic to remove.
     */
    public synchronized void removeTopic(int index) {
        if ( index >= filteredList.size() ) {
            return;
        }
        if ( filteredList.get(index).isMutable() ) {
            Topic topic = filteredList.get(index);
            topicList.remove(topic);
            filteredList.remove(topic);
            if ( topic == this.selectedTopic ) {
                this.selectedTopic = null;
            }
            networking.deleteTopic(topic); // Delete the topic
            propertyChangeSupport.firePropertyChange(REMOVE_TOPIC_PROPERTY, null, index);
        }
    }

    /**
     * Removes the given topic. Called from the back end (Networking).
     * 
     * @param topic
     *            to remove
     */
    public synchronized void removeTopic(Topic topic) {
        if ( topic.isMutable() ) {
            for (Topic t : topicList) {
                if ( t.getName().toLowerCase().equals(topic.getName().toLowerCase()) ) {
                    topicList.remove(t);
                    filteredList.remove(t);
                    if ( t == this.selectedTopic ) {
                        this.selectedTopic = null;
                    }
                    break;
                }
            }
            propertyChangeSupport.firePropertyChange(REMOVE_TOPIC_FROM_NETWORK_PROPERTY, null, topic);
        }
    }

    /**
     * Adds an already created topic to the topic list. This is called from
     * Networking if ConfigurationModel.useDynamicTopics is true.
     * 
     * @param topic
     *            to add
     */
    public synchronized void addExistingTopic(Topic topic) {
        boolean notFound = true;
        for (Topic t : topicList) {
            if ( t.getName().toLowerCase().equals(topic.getName().toLowerCase()) ) {
                notFound = false;
            }
        }
        if ( notFound ) {
            topicList.add(topic);
            // Update the filtered list
            updateFilteredList();
            propertyChangeSupport.firePropertyChange(ADD_EXISTING_TOPIC_PROPERTY, null, topic);
        }
    }

    /**
     * Called when a topic is selected for use in the topic list in ChatView,
     * this notifies the network of the topic change and returns the selected
     * topic. It does not change chat in ChatModel!
     * 
     * @param index
     *            of topic that was selected
     * @return Topic we changed to, null if no change was made.
     */
    public synchronized Topic selectTopic(int index) {
        Topic topic = this.filteredList.get(index);
        networking.changeTopic(topic);
        if ( this.selectedTopic != topic ) {
            this.selectedTopic = topic;
            return topic;
        } else {
            return null;
        }
    }

    /**
     * getTopic gets the specific topic element by an index.
     */
    @Override
    public synchronized Topic getElementAt(int index) {
        return this.filteredList.get(index);
    }

    /**
     * getSize gets the size of the topicList.
     */
    @Override
    public synchronized int getSize() {
        return this.filteredList.size();
    }

    /**
     * Gets the list of all topics. Used by Networking when sending a TOPIC_LIST
     * message.
     * 
     * @return topicList reference, final.
     */
    final synchronized ArrayList<Topic> getTopicList() {
        return new ArrayList<Topic>(this.topicList);
    }

    /**
     * Repopulates the filtered list with the current filter status.
     */
    public synchronized void updateFilteredList() {
        this.filteredList.clear();
        // Add the current topic
        if ( this.selectedTopic != null ) {
            this.filteredList.add(this.selectedTopic);
        }

        for (Topic t : this.topicList) {
            if ( t.isVisibleThroughFilter() && t != this.selectedTopic ) {
                this.filteredList.add(t);
            }
        }
        propertyChangeSupport.firePropertyChange(TOPIC_LIST_FILTER_UPDATE, null, null);
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
     * Parses a list of topic from the file at the given location.
     * 
     * @param topicListFile
     *            path to file.
     * @throws IOException
     *             If UTF-8 is not supported, file is missing or cannot be
     *             read/closed.
     */
    private void parseTopicsFromFile(String topicListFile) throws IOException {
        File f = new File(topicListFile);
        FileReader fr = new FileReader(f);
        BufferedReader reader = new BufferedReader(fr);
        while (true) {
            // Line format: [topic name] <multicast group address>
            // Where the address is only used if we use static multicast.
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                System.out.println("Topic.topic(): Could not read line from topic list file. Exception caught: ");
                e.printStackTrace();
                continue;
            }
            if ( line == null ) {
                break; // EOF
            }
            if ( line.length() == 0 ) {
                continue; // Empty line
            }
            if ( line.charAt(0) == '#' ) {
                continue; // Comment
            }
            String[] parts = line.split("&");
            if ( parts.length == 0 || line.split(" ").length == 0 ) {
                continue; // Empty line.
            }
            // Make sure the name fits into 255 bytes.
            String name = parts[0].substring(0, Math.min(parts[0].length(), 255));
            while (name.getBytes("UTF-8").length > 255) {
                name = name.substring(0, name.length() - 1);
            }
            if ( name.length() == 0 ) {
                continue; // Empty
            }
            Topic t = new Topic(name, false);
            boolean notDuplicate = true;
            for (Topic to : topicList) {
                if ( to.getName().equals(t.getName()) ) {
                    notDuplicate = false;
                }
            }
            if ( notDuplicate ) {
                boolean areMember = false;
                // If static multicast, add recipients to subscribers.
                if ( !ChatConfigurationModel.useDynamicMulticast() ) {
                    MulticastGroup group = null;
                    for (MulticastGroup g : Configuration.getMulticastGroups()) {
                        String addr = InetAddress.getByName(parts[1]).getHostAddress();
                        if ( g.getMulticastAddress().getHostAddress().equals(addr) ) {
                            group = g;
                            break;
                        }
                    }
                    if ( group != null ) {
                        // Check if we are a member of this group, if not, don't
                        // show it!
                        for (int id : group.getSourceIds()) {
                            if ( id == Configuration.getNodeId() ) {
                                areMember = true;
                            }
                            t.addSubscriber(new Subscriber(id, "User_" + id));
                        }
                    }
                }
                // If we use static multicast, only add groups which we are a
                // member of, otherwise add all
                if ( areMember || ChatConfigurationModel.useDynamicMulticast() ) {
                    topicList.add(t);
                }
            }
        }
        reader.close();
    }
}
