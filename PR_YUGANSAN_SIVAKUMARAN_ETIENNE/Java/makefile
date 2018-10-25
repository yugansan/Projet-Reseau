JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        Client.java \
        BDServer.java \
        InfoClient.java \
        Message.java \
        Notification.java \
				Promoteur.java \
				Serveur_ClientThread.java \
				Serveur_PromoteurThread.java \
				Serveur.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
