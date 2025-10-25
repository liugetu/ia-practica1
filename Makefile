all: compile

compile:
	javac -cp .:AIMA.jar:Gasolina.jar Main.java

clean:
	rm -f Main.class
	rm -f IA/Gasolina/*.class