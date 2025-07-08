GRADLEW := ./gradlew

.PHONY: all build compile run test clean dot
all: build

build:
	@$(GRADLEW) build

run:
ifndef ARGS
	$(error ARGS is not set. Usage: make run ARGS='<arguments to pass to the program>')
endif
	@$(GRADLEW) run --args='$(ARGS)'

test:
	@$(GRADLEW) test

clean:
	@$(GRADLEW) clean