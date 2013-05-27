pmulchat
=====================

# About
pmulchat is a simple chat application, similar to IRC, but relying on libjpmul to multicast messages.

# Getting started
## Step 1: Get pmulchat
[Click here to download the runnable jar](https://github.com/libjpmul/libjpmul-configurator/blob/master/build/libjpmul-configurator.jar)
## Step 2: Starting it
pmulchat offers a range of command line options to the user. While it is possible to start it by simply clicking the JAR file, this will most likely cause libjpmul to bind to the wrong interface. See [libjpmul configuration](https://github.com/libjpmul/libjpmul/wiki/Configuration#important-parameters) and [libjpmul FAQ](https://github.com/libjpmul/libjpmul/wiki/FAQ#usage-questions) for details on these problems. Running 
```bash
> java -jar pmulchat.jar --help
```
will yield the available command line options.
## Step 3: Configuring it
Follow these steps to get a default configuration file:

1. start pmulchat
2. click the config button
3. select file in the menu bar
4. click write to file
5. select a location to write the file

Alternatively use the file located at _/res/default.conf_.

Once you have the default configuration file, the parameters described in [libjpmul configuration](https://github.com/libjpmul/libjpmul/wiki/Configuration#all-available-parameters) can be set in this file. It can then be loaded using the following command:
```bash
> java -jar pmulchat.jar -c [path to file]
```
All parameters pertaining purely to pmulchat and not libjpmul can either be edited at runtime in the corresponding configuration pane or set with command line flags described in the _--help_. If nescessary, they can also be specified with the _-p_ flag.

An example of a topic file used when running the application in static multicast mode can be found at _/res/topics.list_.

## User Manual
A more extensive user manual explaining the functionality within pmulchat can be found [here](https://github.com/libjpmul/pmulchat/blob/master/manual.pdf?raw=true).

## Building pmulchat
### Step 1: Get prerequisites
To build pmulchat you will need [libjpmul](https://github.com/libjpmul/libjpmul) and [libjpmul-Configurator](https://github.com/libjpmul/libjpmul-configurator). Visit their project pages for the latest prebuilt JAR files, or for the source code.
### Step 2: Get pmulchat source
[Click here to download the source archive](https://github.com/libjpmul/libjpmul-configurator/archive/master.zip)
### Step 3: Setting up the project
Include either the libraries or the source code downloaded in step 1 in the build path of pmulchat.
### Step 4: Build
Build the project. The _main_ method is located in the _MainView_ class.

# Licence
libjpmul-Configurator is available under the modified 3-clause BSD license. See the LICENSE file for more information.
