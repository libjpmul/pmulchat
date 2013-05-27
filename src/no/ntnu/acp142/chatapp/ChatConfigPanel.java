package no.ntnu.acp142.chatapp;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
 * The panel that offers a user interface to change the configuration parameters
 * specific to pmulchat.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid
 * 
 */
public class ChatConfigPanel extends JPanel implements PropertyChangeListener {

    private JPanel             chatConfigurationPanel;

    private GridBagConstraints c;
    private JButton            buttonChatConfigApply;
    private JButton            buttonChatConfigClear;

    // --- [JTEXTFIELD CHAT CONFIGURATION PARAMETERS] ----------------------- //
    private JTextField         broadcastGroup;
    private JTextField         broadcastPort;
    private JTextField         maximumWaitForResponseOnDelayedSend;
    private JTextField         defaultTimeToLive;
    private JTextField         waitForInUseResponse;
    // ---------------------------------------------------------------------- //

    // TITLE LABEL FOR CHAT CONFIGURATION
    private JLabel             labelChatConfiguration;

    // --- [JLABEL CHAT CONFIGURATION PARAMETERS] -------------------------- //
    private JLabel             labelBroadcastGroup;
    private JLabel             labelBroadcastPort;
    private JLabel             labelMaximumWaitForResponseOnDelayedSend;
    private JLabel             labelDefaultTimeToLive;
    private JLabel             labelWaitForInUseResponse;

    /**
     * Constructor that initializes and adds the elements on panel.
     * 
     * @param mainView
     *            instance to use for listeners
     **/
    public ChatConfigPanel(MainView mainView) {

        // INITIALIZE
        chatConfigurationPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        buttonChatConfigClear = new JButton("Clear");
        buttonChatConfigApply = new JButton("Apply");

        // JTextFields Chat configuration
        broadcastGroup = new JTextField();
        broadcastPort = new JTextField();
        maximumWaitForResponseOnDelayedSend = new JTextField();
        defaultTimeToLive = new JTextField();
        waitForInUseResponse = new JTextField();

        // JLabels Chat Configuration
        labelChatConfiguration = new JLabel("Chat Configuration:");
        labelBroadcastGroup = new JLabel("Broadcast group:");
        labelBroadcastPort = new JLabel("Broadcast port:");
        labelMaximumWaitForResponseOnDelayedSend = new JLabel("Max wait for response on delayed send:");
        labelDefaultTimeToLive = new JLabel("Default time to live:");
        labelWaitForInUseResponse = new JLabel("Wait for in use response:");

        // SET ACTIONLISTENERS
        buttonChatConfigApply.addActionListener(mainView);
        buttonChatConfigClear.addActionListener(mainView);

        // SET SIZE
        buttonChatConfigClear.setPreferredSize(new Dimension(80, 25));
        buttonChatConfigApply.setPreferredSize(new Dimension(80, 25));

        broadcastGroup.setPreferredSize(new Dimension(200, 25));
        broadcastPort.setPreferredSize(new Dimension(200, 25));
        maximumWaitForResponseOnDelayedSend.setPreferredSize(new Dimension(200, 25));
        defaultTimeToLive.setPreferredSize(new Dimension(200, 25));
        waitForInUseResponse.setPreferredSize(new Dimension(200, 25));

        // SET FONT
        Font fontTitleGroups = new Font("Calibri", Font.BOLD, 18);
        Font fontParameters = new Font("Calibri", Font.PLAIN, 14);
        labelChatConfiguration.setFont(fontTitleGroups);
        labelBroadcastGroup.setFont(fontParameters);
        labelBroadcastPort.setFont(fontParameters);
        labelMaximumWaitForResponseOnDelayedSend.setFont(fontParameters);
        labelDefaultTimeToLive.setFont(fontParameters);
        labelWaitForInUseResponse.setFont(fontParameters);

        // SET DESCRIPTION TO LABEL
        labelBroadcastGroup.setToolTipText("Multicast group to used to share lists of the users in coordinationGroup.");
        labelBroadcastPort.setToolTipText("Port to listen to broadcastGroup on");
        labelMaximumWaitForResponseOnDelayedSend
                .setToolTipText("When we wait for responses before sending , we wait a random amount of time, from 0 to this amount of milliseconds");
        labelDefaultTimeToLive.setToolTipText("Default time to live of ACP142 messages.");
        labelWaitForInUseResponse.setToolTipText("Time to wait for a response before deleting a topic.");

        // --- [JLABEL CHAT CONFIGURATION PARAMETERS] --------------------------
        // //

        // TITLE CHAT CONFIGURATION
        c.insets = new Insets(10, 5, 5, 10);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        chatConfigurationPanel.add(labelChatConfiguration, c);

        // BROADCAST GROUP
        c.gridx = 0;
        c.gridy = 1;
        chatConfigurationPanel.add(labelBroadcastGroup, c);

        // BROADCAST PORT
        c.gridx = 0;
        c.gridy = 2;
        chatConfigurationPanel.add(labelBroadcastPort, c);

        // MAXIMUM WAIT FOR RESPONSE ON DELAYED SEND
        c.gridx = 0;
        c.gridy = 3;
        chatConfigurationPanel.add(labelMaximumWaitForResponseOnDelayedSend, c);

        // DEFAULT TIME TO LIVE
        c.gridx = 0;
        c.gridy = 4;
        chatConfigurationPanel.add(labelDefaultTimeToLive, c);

        // WAIT FOR IN USE RESPONSE
        c.gridx = 0;
        c.gridy = 5;
        chatConfigurationPanel.add(labelWaitForInUseResponse, c);

        // ----------------------------------------------------------------------
        // //

        // --- [ CHAT CONFIGURATION PARAMETERS]
        // --------------------------------- //
        // BROADCAST GROUP
        c.gridx = 1;
        c.gridy = 1;
        chatConfigurationPanel.add(broadcastGroup, c);

        // BROADCAST PORT
        c.gridx = 1;
        c.gridy = 2;
        chatConfigurationPanel.add(broadcastPort, c);

        // MAXIMUM WAIT FOR RESPONSE ON DELAYED SEND
        c.gridx = 1;
        c.gridy = 3;
        chatConfigurationPanel.add(maximumWaitForResponseOnDelayedSend, c);

        // DEFAULT TIME TO LIVE
        c.gridx = 1;
        c.gridy = 4;
        chatConfigurationPanel.add(defaultTimeToLive, c);

        // WAIT FOR IN USE RESPONSE
        c.gridx = 1;
        c.gridy = 5;
        chatConfigurationPanel.add(waitForInUseResponse, c);

        // BUTTON CLEAR
        c.gridx = 1;
        c.gridy = 7;
        chatConfigurationPanel.add(buttonChatConfigClear, c);

        // BUTTON APPLY
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 7;
        chatConfigurationPanel.add(buttonChatConfigApply, c);

        // SET DEFAULT MODEL TO THE ELEMENTS
        setModelChatConfiguration();
    }

    /**
     * setModelChatConfiguration method sets the default configuration model to
     * chatConfigView by getting all the data from the class
     * ChatConfigurationModel.
     */
    public void setModelChatConfiguration() {
        broadcastGroup.setText(ChatConfigurationModel.getBroadcastGroup().getHostAddress());
        broadcastPort.setText(Short.toString(ChatConfigurationModel.getBroadcastPort()));
        maximumWaitForResponseOnDelayedSend.setText(Long.toString(ChatConfigurationModel
                .getMaximumWaitForResponseOnDelayedSend()));
        defaultTimeToLive.setText(Long.toString(ChatConfigurationModel.getDefaultTimeToLive()));
        waitForInUseResponse.setText(Long.toString(ChatConfigurationModel.getWaitForInUseResponse()));

    }

    /**
     * getChatConfigurationPanel gets the chat configuration panel from
     * ConfigView.
     * 
     * @return chat configuration panel
     */
    public JPanel getChatConfigurationPanel() {
        return chatConfigurationPanel;
    }

    /**
     * applyChangesConfiguration adds all changes to the ConfigurationModel when
     * Apply button is clicked.
     */
    public void applyChangesChatConfiguration() {
        ChatConfigurationModel.setBroadcastGroup(broadcastGroup.getText());
        ChatConfigurationModel.setBroadcastPort(Short.parseShort(broadcastPort.getText()));
        ChatConfigurationModel.setMaximumWaitForResponseOnDelayedSend(Long
                .parseLong(maximumWaitForResponseOnDelayedSend.getText()));
        ChatConfigurationModel.setDefaultTimeToLive(Long.parseLong(defaultTimeToLive.getText()));
        ChatConfigurationModel.setWaitForInUseResponse(Long.parseLong(waitForInUseResponse.getText()));
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
        if ( name.equals(ChatConfigurationModel.BROADCAST_GROUP_PROPERTY) ) {
            broadcastGroup.setText(ChatConfigurationModel.getBroadcastGroup().toString());
        }
        if ( name.equals(ChatConfigurationModel.BROADCAST_PORT_PROPERTY) ) {
            broadcastPort.setText(Short.toString(ChatConfigurationModel.getBroadcastPort()));
        }
        if ( name.equals(ChatConfigurationModel.MAXIMUM_WAIT_FOR_RESPONSE_ON_DELAYED_SEND_PROPERTY) ) {
            maximumWaitForResponseOnDelayedSend.setText(Long.toString(ChatConfigurationModel
                    .getMaximumWaitForResponseOnDelayedSend()));
        }
        if ( name.equals(ChatConfigurationModel.DEFAULT_TIME_TO_LIVE_PROPERTY) ) {
            defaultTimeToLive.setText(Long.toString(ChatConfigurationModel.getDefaultTimeToLive()));
        }
        if ( name.equals(ChatConfigurationModel.WAIT_FOR_IN_USE_RESPONSE_PROPERTY) ) {
            waitForInUseResponse.setText(Long.toString(ChatConfigurationModel.getWaitForInUseResponse()));
        }
    }

    /**
     * getButtonChatConfigClear gets the clear button for chat configurations.
     * 
     * @return clear button from chat configuration
     */
    public JButton getButtonChatConfigClear() {
        return buttonChatConfigClear;
    }

    /**
     * getButtonChatConfigClear gets the apply button for chat configurations.
     * 
     * @return apply button from chat configuration
     */
    public JButton getButtonChatConfigApply() {
        return buttonChatConfigApply;
    }
}
