# OS
CA321 Operating Systems

# The supplied applet plays one second of audio data from the file toto.wav.
# The applet also comes with a text area for entering commands. (Currently
# the text area listener simply echoes whatever text is entered.)

# To compile the applet:
$ javac StudentPlayerApplet.java

# To launch the applet:
$ appletviewer -J"-Djava.security.policy=all.policy" audio.html
