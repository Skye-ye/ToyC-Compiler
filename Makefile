ANTLR_JAR = $(shell find ./lib -name "antlr-complete.jar")

ANTLR = java -jar $(ANTLR_JAR) -listener -visitor -long-messages
JAVAC = javac -g
JAVA = java

export CLASSPATH := ./classes:$(ANTLR_JAR)

PFILE = $(shell find . -name "ToyCParser.g4")
LFILE = $(shell find . -name "ToyCLexer.g4")
JAVAFILES = $(shell find . -name "*.java")


PHONY += all
all: compile


PHONY += antlr
antlr: $(LFILE) $(PFILE) 
	@echo "Generating ANTLR parser and lexer..."
	$(ANTLR) $(PFILE) $(LFILE)


PHONY += compile
compile: antlr
	@echo "Compiling Java sources..."
	mkdir -p classes
	$(JAVAC) -classpath $(ANTLR_JAR) $(JAVAFILES) -d classes


PHONY += run
run: compile
	@echo "Running with input file: $(FILEPATH)"
	$(JAVA) -classpath $(CLASSPATH) Main $(FILEPATH)


PHONY += test
test: compile
	@echo "Running test on ./tests/test1.toy"
	$(JAVA) -cp "$(CLASSPATH)" Main ./tests/test1.toy


PHONY += clean
clean:
	rm -f src/*.tokens
	rm -f src/*.interp
	rm -f src/ToyCLexer.java src/ToyCParser.java src/ToyCParserBaseListener.java src/ToyCParserBaseVisitor.java src/ToyCParserListener.java src/ToyCParserVisitor.java
	rm -rf classes
	rm -rf out

