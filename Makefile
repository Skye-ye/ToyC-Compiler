GRADLEW := ./gradlew

.PHONY: all build compile run test clean
all: build

build:
	@$(GRADLEW) build

compile:
	@$(GRADLEW) assemble

run:
ifndef FILE
	$(error FILE is not set. Usage: make run FILE=<path/to/your/file.toyc>(relative to src/test/resources/toyc))
endif
	@$(GRADLEW) run --args='src/test/resources/toyc/$(FILE)'

test:
	@$(GRADLEW) test

clean:
	@$(GRADLEW) clean

