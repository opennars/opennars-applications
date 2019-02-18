# opennars-applications
Applications for OpenNARS v3.0.0 and later. Differently than OpenNARS-Lab which is under GPL, OpenNARS-Applications is MIT licensed.

How to build OpenNARS
---------------------

Install git https://git-scm.com/downloads

Install OpenJDK 11 https://jdk.java.net/11/

Install community edition IntelliJ https://www.jetbrains.com/idea/download/

Checkout https://github.com/opennars/opennars.git

Checkout https://github.com/opennars/opennars-lab.git

Checkout https://github.com/opennars/opennars-applications.git

You can either checkout within Intellij or use the Github desktop (availble from the github clone button in the repo)

Build opennars
--------------
If this is a fresh install you will be prompted to enter the jdk path (where you installed it above)
You may be prompted to update maven dependencies - do this if prompted

Build opennars-lab
------------------
Select org.opennars.lab.launcher.Launcher as the main entry point

Build opennars-applications
---------------------------
Select org.opennars.applications.Launcher


The launchers are the easiest way to run the various apps

opennars-lab 
------------
Main GUI	Main user interface for NARS
Test Chamber	Simulation environment for testing behaviours
Micro world	Behaviour learning by simple insect like creature
NAR Pong	The classic pong game

Language Lab	For experimenting with parts of speech (POS) and grammar learning
Perception Test Pattern matching experiment

Prediction Test Predicts a waveform - Can be run directly from Intellij 
		(Current issue with running with launcher)
Vision		Vision expeirment - Can be run direcly from Intellij 
		(Current issue with running with launcher)

opennars-applications
---------------------
Main GUI - A simple MIT license GUI - 
Crossing - A snart city traffic intersection suimulation
Identity mapping - An experimental setup for testing aspects of Relations Frame Theory (RVT)

opennars
--------
Core - Launchers run this directly
