ANTLR_JAR := $(shell find ./lib -name "antlr-*-complete.jar" | head -n 1)
ANTLR_CMD := java -jar $(ANTLR_JAR)

JAVAC := javac -g
JAVA  := java

SRC_JAVA_DIR := src/main/java
SRC_ANTLR_DIR := src/main/antlr4
BUILD_DIR := target
GEN_DIR := $(BUILD_DIR)/generated-sources/antlr4
CLASSES_DIR := $(BUILD_DIR)/classes

JAVA_SOURCES := $(shell find $(SRC_JAVA_DIR) -name "*.java")
PARSER_G4  := $(shell find $(SRC_ANTLR_DIR) -name "*Parser.g4")
LEXER_G4   := $(shell find $(SRC_ANTLR_DIR) -name "*Lexer.g4")

CLASSPATH := $(CLASSES_DIR):$(ANTLR_JAR)

PACKAGE := toyc
MAIN_CLASS := $(PACKAGE).Compiler


.PHONY: all antlr compile run test clean

all: compile


antlr: $(PARSER_G4) $(LEXER_G4)
	@echo "Generating ANTLR sources into $(GEN_DIR)..."
	@mkdir -p $(GEN_DIR)/$(PACKAGE)
	@echo "  -> Generating Lexer..."
	$(ANTLR_CMD) -o $(GEN_DIR)/$(PACKAGE) -package $(PACKAGE) -Xexact-output-dir $(LEXER_G4)

	@echo "  -> Generating Parser..."
	$(ANTLR_CMD) \
		-o $(GEN_DIR)/$(PACKAGE) \
		-lib $(GEN_DIR)/$(PACKAGE) \
		-listener -visitor -package $(PACKAGE) \
		-Xexact-output-dir \
		$(PARSER_G4)


compile: antlr
	@echo "Compiling Java sources..."
	@mkdir -p $(CLASSES_DIR)
	$(JAVAC) -d $(CLASSES_DIR) -cp "$(ANTLR_JAR)" \
		-sourcepath "$(SRC_JAVA_DIR):$(GEN_DIR)" \
		$(JAVA_SOURCES) $(shell find $(GEN_DIR) -name "*.java")


run: compile
ifndef FILEPATH
	$(error FILEPATH is not set. Usage: make run FILEPATH=<path/to/your/file.toyc>)
endif
	@echo "Running compiler on $(FILEPATH)..."
	$(JAVA) -cp "$(CLASSPATH)" $(MAIN_CLASS) $(FILEPATH)


test: compile
	@echo "Running test on ./tests/test1.toyc..."
	$(JAVA) -cp "$(CLASSPATH)" $(MAIN_CLASS) ./tests/test1.toyc


clean:
	@echo "Cleaning up build artifacts..."
	@rm -rf $(BUILD_DIR)