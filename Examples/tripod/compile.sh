javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/tac/util/*.java
javac -classpath . se/sics/tac/aw/*.java
jar cfm tripodagent.jar AWManifest.txt com/botbox/util/*.class se/sics/tac/aw/*.class se/sics/tac/util/*.class
