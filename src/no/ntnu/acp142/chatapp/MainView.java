package no.ntnu.acp142.chatapp;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.naming.NameAlreadyBoundException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import no.ntnu.acp142.Configuration;
import no.ntnu.acp142.Log;
import no.ntnu.acp142.configui.ConfigurationModel;
import no.ntnu.acp142.configui.ConfigPanel;

/*
 * Copyright (c) 2013, Luka Cetusic, Thomas Martin Schmid, Karl Mardoff Kittilsen
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
 * This class initializes the whole user interface. It creates a ChatView and
 * and allows for the opening of ConfigPanel and ChatConfigPanel in two separate
 * windows. It is also the entry point of pmulchat.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid, Karl Mardoff Kittilsen
 */
public class MainView extends JFrame implements ActionListener, KeyListener {

    private ChatConfigPanel chatConfigPanel;
    private ConfigPanel configPanel;
    private ChatConfigurationModel chatConfigurationModel;
    private ConfigurationModel configurationModel;
    private ChatView chatPanel;
    private Networking networking;
    private TopicModel topicModel;

    private JFrame chatView;
    private JFrame configView;

    private JTabbedPane tabbedPane;
    private JScrollPane scrollPaneChatConfigPanel;
    private JMenuItem menuLoadFromFile;
    private JMenuItem menuWriteToFile;
    private JMenuItem menuQuit;
    private JMenuBar menuBar;
    private JMenu menuFileMenu;
    private JFileChooser fileChooser;

    // MAX CHARACTERS PARAMETER
    private static final int MAX_CHARACTERS = 65535;

    // Boolean that checks if configuration windows is open.
    private boolean isOpen;

    /**
     * Initializes the class.
     * 
     * @param networking
     *            instance to use for network transmission
     * @param topicModel
     *            that handles topics
     * @param chatModel
     *            that handles chats
     * @param configModel
     *            that handles configuration for the protocol
     * @param chatConfigModel
     *            that handles configuration for the application
     * @param inEmcon
     *            describes whether we are inEMCON or not
     */
    public MainView(Networking networking, TopicModel topicModel,
            ChatModel chatModel, ConfigurationModel configModel,
            ChatConfigurationModel chatConfigModel, boolean inEmcon) {
        // Sets networking reference
        this.networking = networking;

        // Sets isOpen to false because configuration windows is not open
        isOpen = false;

        // Set Look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Throws error message if LAF could not be set
            throw new Error("Error setting native LAF: " + e);
        }

        // Create menu items and set their mnemonic, accelerator, enabled.
        menuLoadFromFile = new JMenuItem("Load from file");
        menuWriteToFile = new JMenuItem("Write to file");
        menuQuit = new JMenuItem("Quit");

        // Build menu bar, menus, and add menu items.
        menuBar = new JMenuBar(); // Create new menu bar
        menuFileMenu = new JMenu("File"); // Create new menu
        menuFileMenu.setMnemonic('F');
        menuBar.add(menuFileMenu); // Add menu to the menu bar
        menuFileMenu.add(menuLoadFromFile); // Add menu item to the menu
        menuFileMenu.addSeparator(); // Add separator line to menu
        menuFileMenu.add(menuWriteToFile); // Add menu item to the menu
        menuFileMenu.addSeparator(); // Add separator line to menu
        menuFileMenu.add(menuQuit);

        // Add listeners to menu items
        menuLoadFromFile.addActionListener(this);
        menuWriteToFile.addActionListener(this);
        menuQuit.addActionListener(this);

        // Initializes file chooser
        fileChooser = new JFileChooser();

        // Initializes frame, panel, models and scrollPane
        chatView = new JFrame("Chat");
        this.topicModel = topicModel;
        configurationModel = configModel;
        chatConfigurationModel = chatConfigModel;
        scrollPaneChatConfigPanel = new JScrollPane();
        chatPanel = new ChatView(this, chatModel, topicModel, inEmcon);

        // Adds chatPanel to the frame and sets it visible
        chatView.getContentPane().add(chatPanel);
        chatView.setSize(new Dimension(655, 600));
        chatView.setResizable(false);
        chatView.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatView.setVisible(true);

        chatView.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        // Sets frame to the center of the screen
        centerSingleFrame();

        // Add listener to ConfigurationModel and ChatConfigurationModel
        configurationModel.addPropertyChangeListener(configPanel);
        chatConfigurationModel.addPropertyChangeListener(chatConfigPanel);

    }

    /**
     * dispose method shuts down the network.
     */
    @Override
    public void dispose() {
        this.networking.shutdown();
    }

    /**
     * centerSingleFrame method that sets the frames in the center of the
     * screen.
     */
    private void centerSingleFrame() {

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int w = chatView.getSize().width;
        int h = chatView.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;
        chatView.setLocation(x, y);

    }

    /**
     * centerTwoFrame method that sets the frames next to each other and in the
     * center of the screen.
     */
    private void centerTwoFrames() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int w = chatView.getSize().width + configView.getSize().width;
        int h = chatView.getSize().height;
        int x = (dim.width - w) / 2;
        int y = (dim.height - h) / 2;
        chatView.setLocation(x, y);
        configView.setLocation(x + 655, y);
    }

    /**
     * actionPerformed method listens to the JButtons on the panel and interacts
     * with the proper action.
     * 
     * @param e
     *            is the event
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // CONFIGURATION BUTTON IN CHATVIEW
        if (e.getActionCommand().equals("Config") && !isOpen) {

            // Sets isOpen to true because configuration windows is open
            isOpen = true;

            // Initialize frame ConfigPanel and ChatConfigPanel and panels
            // chatConfigPanel and configPanel
            configView = new JFrame("Configuration");
            configPanel = new ConfigPanel();
            chatConfigPanel = new ChatConfigPanel(this);
            tabbedPane = new JTabbedPane();

            // Sets scrollPane to the configPanel
            configPanel
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            configPanel.getVerticalScrollBar().setUnitIncrement(15);
            configPanel.setViewportView(configPanel.getConfigurationPanel());
            scrollPaneChatConfigPanel
                    .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPaneChatConfigPanel.getVerticalScrollBar().setUnitIncrement(
                    15);
            scrollPaneChatConfigPanel.setViewportView(chatConfigPanel
                    .getChatConfigurationPanel());

            // Adds configuration and chat configuration tabs
            tabbedPane.addTab("Configuration", configPanel);
            tabbedPane.addTab("Chat Configuration", scrollPaneChatConfigPanel);

            // Sets JMenu to the frame
            configView.setJMenuBar(menuBar);

            // Adds configPanel to the frame and sets it visible
            configView.getContentPane().add(tabbedPane);
            configView.setSize(new Dimension(655, 600));
            configView.setResizable(false);
            configView.setVisible(true);
            configPanel.setSize(50, 50);

            // Sets screens to the center of the screen
            centerTwoFrames();

            configView.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    // Sets isOpen to false because configuration windows is
                    // closed
                    isOpen = false;
                }
            });

        }

        // SEND BUTTON IN CHATVIEW
        if (e.getActionCommand().equals("Send")) {
            String msg = cleanInput();
            // Check if a command
            if (!parseCommand(msg)) {
                // Otherwise send
                sendMessage(msg);
            }
        }

        // CLEAR BUTTON IN CHATVIEW
        if (e.getActionCommand().equals("Clear")) {
            chatPanel.clearCommentField();
        }

        // ADD TOPIC BUTTON IN CHATVIEW
        if (e.getActionCommand().equals("Add Topic")) {
            // Update UI
            chatPanel.updateChatArea();
            try {
                if (chatPanel.getSearchField().getText().length() > 0) {
                    Topic topic = topicModel.createNewTopic(chatPanel
                            .getSearchField().getText());
                    // Select the topic
                    chatPanel.chatModel.setCurrentChat(topic);
                    // Update the UI
                    chatPanel.updateChatArea();
                }
            } catch (NameAlreadyBoundException e1) {
                // If the name already exists, it will be at the top of the list
                // (unless you are in a chat, in which case it will be second
                // from the top. Just select it.
                if (topicModel.getSize() == 1) {
                    chatPanel.selectTopic(0);
                    chatPanel.chatModel
                            .addMessage(new Message(new Subscriber(0, "Error"),
                                    "Topic with that name already exists, and you are currently in it!"));
                } else {
                    chatPanel.selectTopic(1);
                    chatPanel.chatModel
                            .addMessage(new Message(new Subscriber(0, "Error"),
                                    "Topic with that name already exists. You are now in it."));
                }
            }
            chatPanel.updateTopicArea(chatPanel.getSearchField().getText());
        }

        // DELETE TOPIC BUTTON
        if (e.getActionCommand().equals("Delete Topic")) {
            // Delete the topic
            chatPanel.deleteSelectedTopic(0);
            // Update UI
            chatPanel.updateChatArea();
            chatPanel.updateTopicArea(chatPanel.getSearchField().getText());
        }

        // TOGGLE EMCON BUTTON
        if (e.getActionCommand().equals("EMCON")) {
            if (((JToggleButton) e.getSource()).isSelected()) {
                this.networking.setEmcon(true);
            } else {
                this.networking.setEmcon(false);
            }
        }

        // APPLY CHANGES TO CONFIGURATION
        if (e.getActionCommand().equals("Apply")
                && e.getSource() == configPanel.getButtonConfigApply()) {
            try {
                configPanel.applyChangesConfiguration();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
        }

        // CLEAR CHANGES TO CONFIGURATION
        if (e.getActionCommand().equals("Clear")
                && e.getSource() == configPanel.getButtonConfigClear()) {
            configPanel.setModelConfiguration();
        }

        // CLEAR IN CHAT CONFIGURATION
        if (e.getActionCommand().equals("Clear")
                && e.getSource() == chatConfigPanel.getButtonChatConfigClear()) {
            chatConfigPanel.setModelChatConfiguration();
        }

        // APPLY IN CHAT CONFIGURATION
        if (e.getActionCommand().equals("Apply")
                && e.getSource() == chatConfigPanel.getButtonChatConfigApply()) {
            chatConfigPanel.applyChangesChatConfiguration();
        }

        // WRITE TO FILE IN CONFIGURATION
        if (e.getActionCommand().equals("Write to file")) {
            int userSelection = fileChooser.showSaveDialog(configView);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File newFile = new File(fileChooser.getSelectedFile() + ".conf");
                if (fileChooser.getDialogType() == JFileChooser.SAVE_DIALOG) {
                    if ((newFile != null) && newFile.exists()) {
                        int response = JOptionPane
                                .showConfirmDialog(
                                        this,
                                        "The file "
                                                + newFile.getName()
                                                + " already exists. Do you want to replace the existing file?",
                                        "Overwrite file",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE);
                        if (response != JOptionPane.YES_OPTION) {
                            return;
                        }
                        try {
                            Configuration.save(fileChooser.getSelectedFile()
                                    .getAbsolutePath() + ".conf");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        try {
                            Configuration.save(fileChooser.getSelectedFile()
                                    .getAbsolutePath() + ".conf");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }

        // LOAD FROM FILE IN CONFIGURATION
        if (e.getActionCommand().equals("Load from file")) {
            int userSelection = fileChooser.showOpenDialog(configView);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                try {
                    Configuration.load(fileChooser.getSelectedFile()
                            .getAbsolutePath());
                } catch (NumberFormatException | IndexOutOfBoundsException
                        | IOException e1) {
                    e1.printStackTrace();
                }
                configPanel.setModelConfiguration();
            }
        }

        // LOAD FROM FILE IN CONFIGURATION
        if (e.getActionCommand().equals("Quit")) {
            System.exit(0);
            chatView.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
        }
    }

    // MAIN METHOD
    /**
     * main method starts the MainView constructor that starts the chat
     * application.
     */
    public static void main(String[] args) {

        Log.setLogLevel(Log.LOG_LEVEL_NORMAL);

        // Create the configuration model
        ConfigurationModel configModel = new ConfigurationModel();

        // Create the chat configuration model
        ChatConfigurationModel chatConfigModel = new ChatConfigurationModel();

        // Handle command line arguments
        boolean inEmcon = false;
        boolean useStaticTopics = false;
        String topicList = null;
        boolean customNodeId = false;

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
            case "-c":
                // ACP142 Configuration file supplied.
                try {
                    int pre = Configuration.getNodeId();
                    Configuration.load(args[++i]);
                    if (pre != Configuration.getNodeId()) {
                        customNodeId = true;
                    }
                } catch (FileNotFoundException e) {
                    System.out
                            .println("The supplied configuration file was not found.");
                } catch (IOException | IndexOutOfBoundsException
                        | NumberFormatException e) {
                    System.out
                            .println("Could not load supplied configuration file. Exception caught: ");
                    e.printStackTrace();
                }
                break;
            case "-t":
                // Topic list file supplied.
                topicList = args[++i];
                break;
            case "-s":
                // Use static topics (complain if no topic list is given!).
                useStaticTopics = true;
                ChatConfigurationModel.setUseDynamicMulticast(false);
                break;
            case "-p":
                // Parameter list supplied.
                while (i < args.length - 1) {
                    String param = args[++i];
                    boolean breakMe = false;
                    switch (param) {
                    // ACP142 parameters
                    case "WAIT_FOR_REJECT_TIME":
                        Configuration.setWaitForRejectTime(Integer
                                .valueOf(args[++i]));
                        break;
                    case "ANNOUNCE_DELAY":
                        Configuration.setAnnounceDelay(Integer
                                .valueOf(args[++i]));
                        break;
                    case "ANNOUNCE_CT":
                        Configuration.setAnnounceCt(Integer.valueOf(args[++i]));
                        break;
                    case "ACK_RE-TRANSMISSION_TIME":
                        Configuration.setAckRetransmissionTime(Integer
                                .valueOf(args[++i]));
                        break;
                    case "BACK-OFF_FACTOR":
                        Configuration.setBackoffFactor(Double
                                .valueOf(args[++i]));
                        break;
                    case "EMCON_RTC":
                        Configuration.setEmconRtc(Integer.valueOf(args[++i]));
                        break;
                    case "EMCON_RTI":
                        Configuration.setEmconRti(Integer.valueOf(args[++i]));
                        break;
                    case "MM":
                        Configuration.setMm(Integer.valueOf(args[++i]));
                        break;
                    case "ACK_PDU_TIME":
                        Configuration.setAckPduTime(Integer.valueOf(args[++i]));
                        break;
                    case "GG":
                        Configuration.setGg(args[++i]);
                        break;
                    case "TPORT":
                        Configuration.setTPort(Integer.valueOf(args[++i]));
                        break;
                    case "RPORT":
                        Configuration.setRPort(Integer.valueOf(args[++i]));
                        break;
                    case "DPORT":
                        Configuration.setDPort(Integer.valueOf(args[++i]));
                        break;
                    case "APORT":
                        Configuration.setAPort(Integer.valueOf(args[++i]));
                        break;
                    case "MULTICAST_START_RANGE":
                        Configuration.setMulticastStartRange(args[++i]);
                        break;
                    case "MULTICAST_END_RANGE":
                        Configuration.setMulticastEndRange(args[++i]);
                        break;
                    case "PDU_MAX_SIZE":
                        Configuration.setPduMaxSize(Integer.valueOf(args[++i]));
                        break;
                    case "PDU_EXPIRY_TIME":
                        Configuration.setUndefinedPduExpiryTime(Integer
                                .valueOf(args[++i]));
                        break;
                    case "NODE_ID":
                        Configuration.setNodeId((int) Long.valueOf(args[++i])
                                .longValue());
                        customNodeId = true;
                        break;
                    case "ACK_DELAY_UPPER_BOUND":
                        Configuration.setAckDelayUpperBound(Long
                                .valueOf(args[++i]));
                        break;
                    case "BIND_INTERFACE_ADDRESS":
                        try {
                            Configuration.setBindInterfaceAddress(InetAddress
                                    .getByName(args[++i]));
                        } catch (UnknownHostException e) {
                            System.out
                                    .println("Bind interface address could not be resolved: "
                                            + args[i]);
                        }
                        break;
                    // ChatApp settings
                    case "BROADCAST_GROUP":
                        ChatConfigurationModel.setBroadcastGroup(args[++i]);
                        break;
                    case "BROADCAST_PORT":
                        ChatConfigurationModel.setBroadcastPort(Short
                                .valueOf(args[++i]));
                        break;
                    case "MAXIMUM_WAIT_FOR_RESPONSE_ON_DELAYED_SEND":
                        ChatConfigurationModel
                                .setMaximumWaitForResponseOnDelayedSend(Long
                                        .valueOf(args[++i]));
                        break;
                    case "DEFAULT_TIME_TO_LIVE":
                        ChatConfigurationModel.setDefaultTimeToLive(Long
                                .valueOf(args[++i]));
                        break;
                    case "WAIT_FOR_IN_USE_RESPONSE":
                        ChatConfigurationModel.setWaitForInUseResponse(Long
                                .valueOf(args[++i]));
                        break;
                    default:
                        // Unknown parameter, we are done with the list. Step i
                        // back once, then break.
                        --i;
                        breakMe = true;
                        break;
                    }

                    if (breakMe) {
                        break;
                    }
                }
                break;
            case "-e":
                // Start in EMCON mode.
                inEmcon = true;
                break;
            case "-v":
                // Verbose mode wanted
                Log.setLogLevel(Log.LOG_LEVEL_VERBOSE);
                break;
            case "-d":
                // Debug mode wanted
                Log.setLogLevel(Log.LOG_LEVEL_DEBUG);
                break;
            case "-q":
                // Quiet mode wanted
                Log.setLogLevel(Log.LOG_LEVEL_QUIET);
                break;
            case "-g":
                // Persistant dynamic multicast groups
                // We can set this blindly since we check for both the use of
                // this and dynamic multicast in Networking
                ChatConfigurationModel.setUsePersistantGroups(true);
                break;
            case "-h":
            case "--help":
                // Help wanted
                System.out.println("\nChatApp for ACP142 - v0.1a");
                System.out
                        .println("Usage: java -jar chatapp.jar [option <arguments>]");
                System.out.println("\nOptions:");
                System.out
                        .println("-c [file path] \tLoad ACP142 configuration from the given file on startup.");
                System.out
                        .println("-t [file path] \tLoad a list of topics from the given file on startup.");
                System.out
                        .println("-s \t\tUse static multicast & topics. Only sensible used together with -t");
                System.out.println("\t\tand multicast groups supplied in -c");
                System.out.println("\t\tfile on startup.");
                System.out
                        .println("-e \t\tStart the application in EMCON mode.");
                System.out
                        .println("-g \t\tUse persistant dynamic multicast groups. Only makes sense without -s\n");
                System.out.println("-p [<parameter_name> <value>...]");
                System.out
                        .println("\t\tSet any number of parameters with name-value pairs separated by");
                System.out
                        .println("\t\tspace. See configuration file for parameter names.\n");
                System.out.println("-v \t\tSet the log to verbose.");
                System.out
                        .println("-d \t\tSet the log to print debug messages. Also prints every");
                System.out.println("\t\tlog message to standard output.");
                System.out
                        .println("-q \t\tSet the log to quiet mode. Only unrecoverable errors are logged.");
                return;
            default:
                System.out.println("Unknown argument '" + args[i]
                        + "' ignored. Try -h for help.");
            }
        }

        if (useStaticTopics) {
            if (topicList == null) {
                System.out
                        .println("Static topics will only work with a topic list file. Supply one with -t.");
                return;
            }
            ChatConfigurationModel.setUseDynamicTopics(false);
        }

        // Create the chat model
        ChatModel chatModel = new ChatModel();
        // Create the networking instance
        Networking networking = new Networking(chatModel);
        networking.setEmcon(inEmcon);
        // Create the topic model
        TopicModel topicModel = new TopicModel(networking, topicList);
        // Start listening
        int preId = Configuration.getNodeId();
        try {
            networking.initialize(topicModel, chatConfigModel);
        } catch (IOException e) {
            System.out.println("main(): Could not start networking. Error:");
            e.printStackTrace();
            return;
        }
        if (customNodeId) {
            Configuration.setNodeId(preId);
        }
        // Reset the Admin chat now that Node ID is correct
        chatModel.getCurrentChat().resetSelf(null);

        // Shut the awt crashes the hell up
        // System.setErr(null);

        // Create the view.
        new MainView(networking, topicModel, chatModel, configModel,
                chatConfigModel, inEmcon);
    }

    /**
     * keyReleased listens if the enter button is pressed in the comment field.
     * 
     * @param e
     *            is the key event
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource().equals(chatPanel.getCommentField())
                && e.getKeyChar() == KeyEvent.VK_ENTER) {
            String msg = cleanInput();
            // Check if a command
            if (!parseCommand(msg)) {
                // Otherwise send
                sendMessage(msg);
            }
        }
    }

    /**
     * Parses the given message for commands, and executes them. If there is no
     * command, it returns false, otherwise it returns true. Commands are
     * messages on the form "/command [arguments]".
     * 
     * @param msg
     *            the Message to parse for commands
     * @return True if a command was found.
     */
    private boolean parseCommand(String msg) {
        if (msg.charAt(0) == '/') {
            String command = msg.split(" ")[0];
            Subscriber sub = new Subscriber(0, "Help");
            boolean success = false;
            if (command.equals("/nick") || command.equals("/name")) {
                String[] parts = msg.split(" ");
                if (parts.length > 1) {
                    String nick = parts[1].trim();
                    chatPanel.getChatModel().getCurrentChat().resetSelf(nick);
                    chatPanel.addMessage(new Message(sub,
                            "Username changed to " + nick));
                    success = true;
                }
            }
            if (command.equals("/help")) {
                String[] parts = msg.split(" ");
                if (parts.length > 1) {
                    String command2 = parts[1].trim();
                    if (command2.equals("nick") || command2.equals("name")) {
                        chatPanel
                                .addMessage(new Message(
                                        sub,
                                        "'/"
                                            + command2 + " [username]' is used to change to " +
                                            "the<br>supplied username.<br>/nick and /name are " +
                                            "equivalent and do not work with<br>static multicast groups."));
                    } else if (command2.equals("help")) {
                        chatPanel
                                .addMessage(new Message(
                                        sub,
                                        "'/help [command]' displays help for the given<br>command." +
                                        " If no command is given, it displays available<br>commands."));
                    }
                } else {
                    chatPanel.addMessage(new Message(sub,
                            "Available commands:<br>/nick /name /help"));
                }
                success = true;
            }
            if (!success) {
                chatPanel
                        .addMessage(new Message(sub,
                                "Unknown command. Use /help to see available<br>commands"));
            }
            return true;
        }
        return false; // To change body of created methods use File | Settings |
                        // File Templates.
    }

    /**
     * keyPressed listens to how many keys are pressed in the comment field.
     * keyPressed listens to what keys are pressed in the search topic text
     * field.
     * 
     * @param e
     *            is the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getSource().equals(chatPanel.getCommentField())) {
            // Keeps hold of how many characters there are in comment field
            int totalCharacters;

            // Checks the comment field's current text length
            int currentTextLength = chatPanel.getCurrentCommentFieldLength();

            // Checks if users deletes characters, if true it updates
            // totalCharacter
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                totalCharacters = currentTextLength - 1;
            }
            // If false, total characters incremented with 1
            else {
                totalCharacters = currentTextLength + 1;
            }

            // If totalCharacters is more or equal to MAX_CHARACTERS,
            // a warning message will show up and tell the user that max
            // characters
            // has been exceeded
            if (totalCharacters >= MAX_CHARACTERS) {
                JOptionPane.showMessageDialog(chatView,
                        "You have exceeded max characters!",
                        "Max Character warning", JOptionPane.WARNING_MESSAGE);
                chatPanel.ignoreInputCommentField();
            }
        }
        if (e.getSource().equals(chatPanel.getSearchField())) {
            String temp = "";
            if ((e.getKeyChar() >= '0' && e.getKeyChar() <= '9')
                    || (e.getKeyChar() >= 'a' && e.getKeyChar() <= 'z')
                    || (e.getKeyChar() >= 'A' && e.getKeyChar() <= 'Z')) {
                temp = chatPanel.getSearchField().getText() + e.getKeyChar();
            } else if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
                temp = chatPanel.getSearchField().getText();
                if (temp.length() > 0) {
                    int remove = chatPanel.getSearchField().getSelectedText() == null ? 1
                            : chatPanel.getSearchField().getSelectedText()
                                    .length();
                    temp = temp.substring(0, temp.length() - remove);
                }
            } else if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                // If only one topic is left unfiltered, switch to it.
                // If none are left create a new one.
                // Otherwise do nothing.
                if (topicModel.getSize() == 1
                        || topicModel.getTopicList().size() == 0) {
                    try {
                        // If dynamic topics, create a new one & switch to it.
                        if (ChatConfigurationModel.useDynamicTopics()) {
                            if (chatPanel.getSearchField().getText().length() > 0) {
                                Topic topic = topicModel
                                        .createNewTopic(chatPanel
                                                .getSearchField().getText());
                                // Select the topic
                                chatPanel.chatModel.setCurrentChat(topic);
                            }
                        }
                    } catch (NameAlreadyBoundException e1) {
                        // This can only happen if you are already in this
                        // topic.
                    }
                    // Update UI
                    chatPanel.updateChatArea();
                } else if (topicModel.getSize() == 2) {
                    chatPanel.selectTopic(1);
                }
                temp = chatPanel.getSearchField().getText();
            }
            // Update the filtered list.
            chatPanel.updateTopicArea(temp);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Do nothing!
    }

    /**
     * Removes trailing newlines from the input from the comment field and
     * clears the field.
     * 
     * @return The cleaned string
     */
    private String cleanInput() {
        // Remove trailing newlines
        String msg = chatPanel.getCommentField().getText();
        if (msg.length() == 0) {
            return "";
        }
        int i = msg.length() - 1;
        while (msg.charAt(i) == '\n' && i > 0) {
            --i;
        }

        // Clear the comment field
        chatPanel.clearCommentField();

        return msg.substring(0, ++i);
    }

    /**
     * Sends the given message, unless said message is of 0 length or no topic
     * is selected. Inserts newlines at suitable places for easier reading.
     * 
     * @param msg
     *            The message to send
     */
    private void sendMessage(String msg) {
        // Insert line breaks
        for (int j = 34; j < msg.length(); j += 54) {
            int i = j - 15; // Don't backtrack more than 15 characters.
            while ( msg.charAt(j) != ' ' && j > i ) {
                --j; // Backtrack to the next space to insert newline.
            }
            if ( j == i ) {
                // If no space was found at a resounable place, insert '-' and split a word
                j += 14;
                msg = msg.substring(0, j) + "-<br>" + msg.substring(j, msg.length());
            } else {
                msg = msg.substring(0, j) + "<br>" + msg.substring(j, msg.length());
            }
        }

        // Checks comment fields current text length
        int currentTextLength = msg.length();

        if (chatPanel.chatModel.getCurrentChat().getTopic() == null
                && currentTextLength > 0) {
            Message message = new Message(new Subscriber(0, "Error"),
                    "Join a topic to send messages");
            chatPanel.addMessage(message);
        } else if (currentTextLength > 0) { // Ignore 0-length strings
            Message message = new Message(chatPanel.chatModel.getCurrentChat()
                    .getSelf(), msg);
            chatPanel.addMessage(message);
            networking.sendMessage(message);
        }
    }
}