ANTLR_JAR := $(shell find ./lib -name "antlr-complete.jar")

ANTLR := java -Dfile.encoding=UTF-8 -jar $(ANTLR_JAR) -listener -visitor -long-messages
JAVAC := javac -g
JAVA := java

CLASSPATH := ./classes:$(ANTLR_JAR)
PFILE := $(shell find . -name "ToyCParser.g4")
LFILE := $(shell find . -name "ToyCLexer.g4")
JAVAFILES := $(shell find . -name "*.java")


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
	$(JAVAC) -cp "$(ANTLR_JAR)" -d classes $(JAVAFILES)


PHONY += run
run: compile
	@echo "Running with input file: $(FILEPATH)"
	$(JAVA) -cp "$(CLASSPATH)" Main $(FILEPATH)


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
	rm -rf src/.antlr

