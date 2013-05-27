package no.ntnu.acp142.chatapp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
 * The user interface that allows users to send messages and choose topics.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid
 */
public class ChatView extends JPanel implements ListSelectionListener, PropertyChangeListener {

    public TopicModel          topicModel;
    public ChatModel           chatModel;

    private JPanel             panel;
    private GridBagConstraints c;

    private JButton            buttonSend;
    private JButton            buttonClearText;
    private JButton            buttonAddTopic;
    private JButton            buttonConfiguration;
    private JToggleButton      buttonToggleEmcon;
    private JButton            buttonDeleteTopic;

    private JList<Message>     textAreaChat;
    private JTextArea          textAreaCommentField;
    private JList<Topic>       listTopics;
    private JTextField         textFieldSearch;

    private JLabel             labelSearchTopic;
    private JLabel             labelTopics;
    private JLabel             labelChat;

    private JScrollPane        scrollPaneCommentField;
    private JScrollPane        scrollPaneChat;
    private JScrollPane        scrollPaneTopics;

    /**
     * Constructor that initializes and adds the elements on panel.
     * 
     * @param mainView
     *            instance to use for listeners
     * @param chatModel
     *            that handles chats
     * @param topicModel
     *            that handles topics
     * @param inEmcon
     *            describes whether we are inEMCON or not
     */
    public ChatView(MainView mainView, ChatModel chatModel, TopicModel topicModel, boolean inEmcon) {

        // Store models
        this.chatModel = chatModel;
        this.topicModel = topicModel;

        // INITIALIZE
        panel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        buttonClearText = new JButton("Clear");
        buttonSend = new JButton("Send");
        buttonAddTopic = new JButton("Add Topic");
        buttonConfiguration = new JButton("Config");
        buttonDeleteTopic = new JButton("Delete Topic");
        buttonToggleEmcon = new JToggleButton("EMCON", inEmcon);
        listTopics = new JList<Topic>();
        textFieldSearch = new JTextField();
        labelSearchTopic = new JLabel("Topic search:");
        labelTopics = new JLabel("Topics");
        labelChat = new JLabel("Chat");
        textAreaCommentField = new JTextArea(5, 5);
        textAreaChat = new JList<Message>();
        scrollPaneCommentField = new JScrollPane(textAreaCommentField);
        scrollPaneChat = new JScrollPane(textAreaChat);
        scrollPaneTopics = new JScrollPane(listTopics);

        // ADD ACTIONLISTENERS
        buttonConfiguration.addActionListener(mainView);
        buttonSend.addActionListener(mainView);
        buttonAddTopic.addActionListener(mainView);
        buttonClearText.addActionListener(mainView);
        buttonDeleteTopic.addActionListener(mainView);
        buttonToggleEmcon.addActionListener(mainView);

        // ADD KEYLISTENER
        textAreaCommentField.addKeyListener(mainView);
        textFieldSearch.addKeyListener(mainView);

        // SET SIZE
        buttonSend.setPreferredSize(new Dimension(100, 25));
        buttonClearText.setPreferredSize(new Dimension(100, 25));
        buttonAddTopic.setPreferredSize(new Dimension(95, 25));
        buttonConfiguration.setPreferredSize(new Dimension(95, 25));
        buttonDeleteTopic.setPreferredSize(new Dimension(95, 25));
        buttonToggleEmcon.setPreferredSize(new Dimension(95, 25));
        textFieldSearch.setPreferredSize(new Dimension(100, 25));
        labelSearchTopic.setPreferredSize(new Dimension(100, 25));
        scrollPaneCommentField.setPreferredSize(new Dimension(400, 100));
        scrollPaneChat.setPreferredSize(new Dimension(400, 365));
        scrollPaneTopics.setPreferredSize(new Dimension(200, 365));

        // SET FONT
        Font listFont = new Font("Calibri", Font.PLAIN, 12);
        final Font textFont = new Font("Calibri", Font.PLAIN, 12);
        Font normalFont = new Font("Calibri", Font.BOLD, 16);
        Font titleFont = new Font("Calibri", Font.BOLD, 18);
        labelSearchTopic.setFont(normalFont);
        labelTopics.setFont(titleFont);
        labelChat.setFont(titleFont);
        listTopics.setFont(listFont);
        textAreaCommentField.setFont(textFont);
        textFieldSearch.setFont(listFont);

        // SET MODEL
        listTopics.setModel(topicModel);
        textAreaChat.setModel(chatModel.getCurrentChat());

        // SET RENDERER
        textAreaChat.setCellRenderer(new ListCellRenderer<Message>() {

            @Override
            public Component getListCellRendererComponent(JList list, Message value, int index, boolean isSelected,
                    boolean hasFocus) {
                JTextPane component = new JTextPane();
                component.setContentType("text/html");
                component.setFont(textFont); // Use consistent font
                component.setText(value.toString());
                component.setEditable(false);

                return component;
            }

        });
        textAreaChat.setFocusable(false);

        // SET FORMAT FOR JTEXTAREAS
        textAreaCommentField.setWrapStyleWord(true);
        textAreaCommentField.setLineWrap(true);
        // SET SCROLLPANE
        scrollPaneCommentField.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneCommentField.getVerticalScrollBar().setUnitIncrement(15);
        scrollPaneChat.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneChat.getVerticalScrollBar().setUnitIncrement(15);
        scrollPaneTopics.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTopics.getVerticalScrollBar().setUnitIncrement(15);

        // SET BORDER
        listTopics.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textAreaChat.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        textAreaCommentField.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // LABEL CHAT
        c.insets = new Insets(5, 5, 0, 5);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(labelChat, c);

        // CHAT AREA
        c.insets = new Insets(10, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 3;
        panel.add(scrollPaneChat, c);

        // COMMENT FIELD
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 2;
        panel.add(scrollPaneCommentField, c);

        // CLEAR BUTTON
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 7;
        panel.add(buttonClearText, c);

        // SEND BUTTON
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 7;
        panel.add(buttonSend, c);

        // LABEL TOPIC
        c.insets = new Insets(5, 5, 0, 5);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 2;
        c.gridy = 0;
        panel.add(labelTopics, c);

        // TOPIC LIST
        c.insets = new Insets(10, 5, 5, 5);
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 3;
        panel.add(scrollPaneTopics, c);

        // SEARCH TOPIC LABEL
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridheight = 3;
        c.gridx = 2;
        c.gridy = 4;
        panel.add(labelSearchTopic, c);

        // SEARCH FIELD
        c.anchor = GridBagConstraints.NORTHEAST;
        c.gridx = 2;
        c.gridy = 4;
        panel.add(textFieldSearch, c);

        // ADD TOPIC BUTTON
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 2;
        c.gridy = 4;
        panel.add(buttonAddTopic, c);

        // DELETE TOPIC BUTTON
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 2;
        c.gridy = 4;
        panel.add(buttonDeleteTopic, c);

        // TOGGLE EMCON BUTTON
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.gridx = 2;
        c.gridy = 5;
        panel.add(buttonToggleEmcon, c);

        // CONFIGURATION BUTTON
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.gridx = 2;
        c.gridy = 5;
        panel.add(buttonConfiguration, c);

        // ADD PANEL
        add(panel);

        // SET CONFIGURATION ON JLIST
        listTopics.addListSelectionListener(this);
        listTopics.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // LISTEN ON MODELS
        chatModel.addPropertyChangeListener(this);
        topicModel.addPropertyChangeListener(this);

    }

    /**
     * valueChanged updates so that the topic selected is marked selected.
     * 
     * @param arg0
     *            is the topic selected
     */
    @Override
    public void valueChanged(ListSelectionEvent arg0) {
        if ( arg0.getValueIsAdjusting() ) {
            return;
        }
        if ( listTopics.getSelectedIndex() < 0 ) {
            return;
        }
        selectTopic(listTopics.getSelectedIndex());
    }

    /**
     * getCurrentCommentFieldLength method gets current length from the comment
     * field.
     * 
     * @return length of comment
     */
    public int getCurrentCommentFieldLength() {
        return textAreaCommentField.getText().length();
    }

    /**
     * ignoreInputCommentField method ignores input after crossing the
     * MAX_CHARACTERS in MainView.
     */
    public void ignoreInputCommentField() {
        textAreaCommentField.setText(textAreaCommentField.getText().substring(0,
                textAreaCommentField.getText().length() - 1));
    }

    /**
     * clearCommentField method clears the comment field text area.
     */
    public void clearCommentField() {
        textAreaCommentField.setText("");
        textAreaCommentField.setCaretPosition(0);
    }

    /**
     * addMessage method adds a new message to the chat.
     * 
     * @param message
     *            to add
     */
    public void addMessage(Message message) {
        chatModel.addMessage(message);
    }

    /**
     * Updates the chat text area. Replaces the model if it has changed.
     */
    public void updateChatArea() {
        textAreaChat.setModel(chatModel.getCurrentChat());
        textAreaChat.updateUI();
    }

    /**
     * getCommentField method gets the comment field.
     * 
     * @return comment field
     */
    public JTextArea getCommentField() {
        return textAreaCommentField;
    }

    /**
     * getSearchField method gets the search topic field.
     * 
     * @return search field
     */
    public JTextField getSearchField() {
        return textFieldSearch;
    }

    /**
     * propertyChange listens if there is a change in the view that needs to get
     * updated from the ConfigurationModel.
     * 
     * @param evt
     *            name of event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ( name.equals(TopicModel.CREATE_NEW_TOPIC_PROPERTY) ) {
            listTopics.updateUI();
        }
        if ( name.equals(TopicModel.REMOVE_TOPIC_PROPERTY) ) {
            listTopics.updateUI();
        }
        if ( name.equals(TopicModel.REMOVE_TOPIC_FROM_NETWORK_PROPERTY) ) {
            listTopics.updateUI();
        }
        if ( name.equals(TopicModel.ADD_EXISTING_TOPIC_PROPERTY) ) {
            listTopics.updateUI();
        }
        if ( name.equals(TopicModel.TOPIC_LIST_FILTER_UPDATE) ) {
            listTopics.updateUI();
        }
        if ( name.equals(ChatModel.ADD_MESSAGE_PROPERTY) ) {
            updateChatArea();
        }
        if ( name.equals(ChatModel.CURRENT_CHAT_PROPERTY) ) {
            updateChatArea();
        }
    }

    /**
     * Selects the topic at the given index
     * 
     * @param index
     *            of topic to select
     */
    public void selectTopic(int index) {
        Topic topic = topicModel.selectTopic(index);
        if ( topic != null ) {
            chatModel.setCurrentChat(topic);
        }
        updateTopicArea(this.textFieldSearch.getText());
    }

    /**
     * Delete the topic at the given index
     * 
     * @param index
     *            of topic to select
     */
    public void deleteSelectedTopic(int index) {
        topicModel.removeTopic(index);

        if ( topicModel.getSize() == 0 ) {
            // Clear the filter
            chatModel.setCurrentChat(new Topic("", true)); // Create a new topic
                                                           // that is not a
                                                           // part of the topic
                                                           // list.
            chatModel.addMessage(new Message(new Subscriber(-1, "Info"), "This is a temporary chat with no members."));
            this.textFieldSearch.setText("");
            updateTopicArea("");
        } else {
            // Select the top topic
            this.selectTopic(0);
        }
        updateChatArea();
    }

    /**
     * Gets the EMCON toggle button.
     * 
     * @return The EMCON toggle button.
     */
    public JToggleButton getEmconToggleButton() {
        return this.buttonToggleEmcon;
    }

    /**
     * Updates topics with new filter state and then updates the topic list.
     * 
     * @param newContentOfSearchField
     *            Text to filter with
     */
    public void updateTopicArea(String newContentOfSearchField) {
        ArrayList<Topic> topics = topicModel.getTopicList();
        for (int i = 0; i < topics.size(); i++) {
            String topicText = topics.get(i).getName().toLowerCase();
            if ( topicText.contains(newContentOfSearchField.toLowerCase()) || newContentOfSearchField.length() == 0 ) {
                topics.get(i).setFiltered(true);
            } else {
                topics.get(i).setFiltered(false);
            }
        }
        topicModel.updateFilteredList();
        listTopics.setSelectedIndex(0);
        listTopics.updateUI();
    }

    /**
     * Gets the chatModel
     * 
     * @return chatModel
     */
    public ChatModel getChatModel() {
        return chatModel;
    }
}
