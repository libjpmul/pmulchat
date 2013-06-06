package no.ntnu.acp142.chatapp;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;
import java.net.UnknownHostException;

/*
 * Copyright (c) 2013, Luka Cetusic, Thomas Martin Schmid
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
 * The ChatConfigurationModel class is responsible for the pmulchat- specific
 * configuration parameters.
 * 
 * @author Luka Cetusic, Thomas Martin Schmid
 * 
 */
public class ChatConfigurationModel {

	private static PropertyChangeSupport propertyChangeSupport;

	public static final String BROADCAST_GROUP_PROPERTY = "BroadcastGroup";
	public static final String BROADCAST_PORT_PROPERTY = "BroadcastPort";
	public static final String MAXIMUM_WAIT_FOR_RESPONSE_ON_DELAYED_SEND_PROPERTY = "MaximumWaitForResponseOnDelayedSend";
	public static final String USE_DYNAMIC_MULTICAST_PROPERTY = "UseDynamicMulticast";
	public static final String USE_PERSISTANT_MULTICAST_PROPERTY = "UsePersistantMulticast";
	public static final String DEFAULT_TIME_TO_LIVE_PROPERTY = "DefaultTimeToLive";
	public static final String WAIT_FOR_IN_USE_RESPONSE_PROPERTY = "WaitForInUseResponse";

	/**
	 * Constructor that initializes propertyChangeSupport
	 */
	public ChatConfigurationModel() {
		propertyChangeSupport = new PropertyChangeSupport(this);
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

	// -----------------------------------
	// Chat Application-specific settings
	// -----------------------------------

	/**
	 * Maximum length of message history for the active chat
	 */
	private static int maximumMessagesToKeepForActiveChat = 500;
	/**
	 * Maximum length of message history for the active chat
	 */
	private static int maximumMessagesToKeepForInactiveChat = 100;
	/**
	 * Multicast group to used to share lists of the users in coordinationGroup.
	 */
	private static String broadcastGroup = "239.1.1.117";
	/**
	 * Port to listen to broadcastGroup on.
	 */
	private static short broadcastPort = 27812;
	/**
	 * When we wait for responses before sending (see
	 * Networking.delayedConditionalSend), we wait a random amount of time, from
	 * 0 to this amount of milliseconds.
	 */
	private static long maximumWaitForResponseOnDelayedSend = 1000;
	/**
	 * When set to true, topics can be dynamically created and deleted. If set
	 * to false, the topics are set up on application startup from a table and
	 * are immutable.
	 */
	private static boolean useDynamicTopics = true;
	/**
	 * Whether to use dynamic multicast groups in libjpmul transmissions.
	 */
	private static boolean useDynamicMulticast = true;
	/**
	 * Whether to use persistant groups with dynamic multicast.
	 */
	private static boolean usePersistantGroups = false;
	/**
	 * Default time to live of ACP142 messages
	 */
	private static long defaultTimeToLive = 60;
	/**
	 * Time to wait for a response before deleting a topic.
	 */
	private static long waitForInUseResponse = 20;

	// -------------------------- GETTERS --------------------------------------
	/**
	 * Gets the multicast group used to coordinate IDs of nodes listening to
	 * coordinationGroup
	 * 
	 * @return Address of multicast group
	 */
	public static InetAddress getBroadcastGroup() {
		try {
			return InetAddress.getByName(broadcastGroup);
		} catch (UnknownHostException e) {
			// Since we test for exceptions when setting the field, this can
			// only happen for the default group
			System.out
					.println("Error in ConfigurationModel.getBroadcastGroup(): Unknown host attempted gotten. Returning null!");
			return null;
		}
	}

	/**
	 * Gets the port used by broadcast group.
	 * 
	 * @return Port to broadcast to.
	 */
	public static short getBroadcastPort() {
		return broadcastPort;
	}

	/**
	 * If true, topics are mutable to the extent of creation and deletion. If
	 * false, all topics must be defined on load, and be set in
	 * DefaultTopicList. This setting is hardcoded.
	 * 
	 * @return true if topics are mutable.
	 */
	public static boolean useDynamicTopics() {
		return useDynamicTopics;
	}

	/**
	 * Determines whether to use dynamic multicast groups when transmitting over
	 * ACP142.
	 * 
	 * @return true if we should use dynamic groups.
	 */
	public static boolean useDynamicMulticast() {
		return useDynamicMulticast;
	}

	/**
	 * Determines whether to use persistant multicast groups with dynamic
	 * multicast when transmitting over ACP142.
	 * 
	 * @return true if we should use persistant groups.
	 */
	public static boolean usePersistantGroups() {
		return usePersistantGroups;
	}

	/**
	 * Gets the default time to live for ACP142 messages.
	 * 
	 * @return default expiry time.
	 */
	public static long getDefaultTimeToLive() {
		return defaultTimeToLive;
	}

	/**
	 * Gets the time to wait for a response when querying the deletion of a
	 * topic.
	 * 
	 * @return time to wait.
	 */
	public static long getWaitForInUseResponse() {
		return waitForInUseResponse;
	}

	/**
	 * Gets the maximum time to wait for a response in delayedConditionalSend.
	 * 
	 * @return Maximum time to wait.
	 */
	public static long getMaximumWaitForResponseOnDelayedSend() {
		return maximumWaitForResponseOnDelayedSend;
	}

	/**
	 * Gets the maximum message count to keep in the Chat object of the
	 * currently active chat.
	 * 
	 * @return maximum length of chat history.
	 */
	public static int getMaximumMessagesToKeepForActiveChat() {
		return maximumMessagesToKeepForActiveChat;
	}

	/**
	 * Gets the maximum message count to keep in the Chat object of the
	 * currently inactive chats.
	 * 
	 * @return maximum length of chat history.
	 */
	public static int getMaximumMessagesToKeepForInactiveChat() {
		return maximumMessagesToKeepForInactiveChat;
	}

	// -------------------------- SETTERS ---------------------------------
	/**
	 * Sets the maximum message count to keep in the Chat object of the
	 * currently inactive chats.
	 * 
	 * @param maximumMessagesToKeepForActiveChat
	 *            maximum length of chat history.
	 */
	public static void setMaximumMessagesToKeepForActiveChat(
			int maximumMessagesToKeepForActiveChat) {
		ChatConfigurationModel.maximumMessagesToKeepForActiveChat = maximumMessagesToKeepForActiveChat;
	}

	/**
	 * Sets the maximum message count to keep in the Chat object of the
	 * currently inactive chats.
	 * 
	 * @param maximumMessagesToKeepForInactiveChat
	 *            maximum length of chat history.
	 */
	public static void setMaximumMessagesToKeepForInactiveChat(
			int maximumMessagesToKeepForInactiveChat) {
		ChatConfigurationModel.maximumMessagesToKeepForInactiveChat = maximumMessagesToKeepForInactiveChat;
	}

	/**
	 * Sets the multicast group used to coordinate IDs of nodes listening to
	 * coordinationGroup. Tests the address so exceptions are thrown here, and
	 * not when setup is attempted at a later time.
	 * 
	 * @param hostname
	 *            of address of multicast group
	 */
	public static void setBroadcastGroup(String hostname) {
		try {
			InetAddress.getByName(hostname);
			broadcastGroup = hostname;
		} catch (UnknownHostException e) {
			System.out
					.println("Error in ConfigurationModel.setBroadcastGroup(): Unknown host'"
							+ hostname + "', old hostname maintained.");
		}
	}

	/**
	 * Sets the port used by broadcast group.
	 * 
	 * @param broadcastPort
	 *            to use.
	 */
	public static void setBroadcastPort(short broadcastPort) {
		String oldValue = Short.toString(getBroadcastPort());
		ChatConfigurationModel.broadcastPort = broadcastPort;
		propertyChangeSupport.firePropertyChange(BROADCAST_PORT_PROPERTY,
				oldValue, broadcastPort);
	}

	/**
	 * Sets whether to use dynamic multicast groups when transmitting over
	 * libjpmul.
	 * 
	 * @param useDynamic
	 *            true if we are to use dynamic groups.
	 */
	public static void setUseDynamicMulticast(boolean useDynamic) {
		String oldValue = Boolean.toString(useDynamicMulticast());
		useDynamicMulticast = useDynamic;
		propertyChangeSupport.firePropertyChange(
				USE_DYNAMIC_MULTICAST_PROPERTY, oldValue, useDynamic);
	}

	/**
	 * Determines whether to use persistant multicast groups with dynamic
	 * multicast when transmitting over ACP142.
	 * 
	 * @param usePersistant
	 *            true if we should use persistant groups.
	 */
	public static void setUsePersistantGroups(boolean usePersistant) {
		String oldValue = Boolean.toString(usePersistantGroups());
		usePersistantGroups = usePersistant;
		propertyChangeSupport.firePropertyChange(
				USE_PERSISTANT_MULTICAST_PROPERTY, oldValue, usePersistant);
	}

	/**
	 * Sets whether to use dynamic topics or not. This should be set on startup
	 * and not be changed afterwards! Dynamic topics mean we allow the users to
	 * create and delete topics. Any topic set in the topic list file are not
	 * deletable.
	 * 
	 * @param useDynamic
	 *            true if we are to use dynamic topics.
	 */
	public static void setUseDynamicTopics(boolean useDynamic) {
		String oldValue = Boolean.toString(useDynamicTopics());
		useDynamicTopics = useDynamic;
		propertyChangeSupport.firePropertyChange(
				USE_DYNAMIC_MULTICAST_PROPERTY, oldValue, useDynamic);
	}

	/**
	 * Sets the default time to live for ACP142 messages.
	 * 
	 * @param time
	 *            before expiry.
	 */
	public static void setDefaultTimeToLive(long time) {
		String oldValue = Long.toString(getDefaultTimeToLive());
		defaultTimeToLive = time;
		propertyChangeSupport.firePropertyChange(DEFAULT_TIME_TO_LIVE_PROPERTY,
				oldValue, time);
	}

	/**
	 * Sets the time to wait for a response when querying the deletion of a
	 * topic, before deleting it.
	 * 
	 * @param wait
	 *            Time to wait.
	 */
	public static void setWaitForInUseResponse(long wait) {
		String oldValue = Long.toString(getWaitForInUseResponse());
		waitForInUseResponse = wait;
		propertyChangeSupport.firePropertyChange(
				WAIT_FOR_IN_USE_RESPONSE_PROPERTY, oldValue, wait);
	}

	/**
	 * Sets the maximum time to wait for a response in delayedConditionalSend.
	 * 
	 * @param wait
	 *            Maximum time to wait in milliseconds.
	 */

	public static void setMaximumWaitForResponseOnDelayedSend(long wait) {
		String oldValue = Long
				.toString(getMaximumWaitForResponseOnDelayedSend());
		maximumWaitForResponseOnDelayedSend = wait;
		propertyChangeSupport.firePropertyChange(
				MAXIMUM_WAIT_FOR_RESPONSE_ON_DELAYED_SEND_PROPERTY, oldValue,
				wait);
	}
}
