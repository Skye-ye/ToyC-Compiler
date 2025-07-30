GRADLEW := ./gradlew

.PHONY: all build run test clean
all: build

build:
	@$(GRADLEW) build

run:
ifndef ARGS
	@echo "Usage: make run ARGS='<arguments>'"
	@echo "Example: make run ARGS='-op=options.yml'"
	@echo "Example: make run ARGS='--help'"
else
	@$(GRADLEW) run --args='$(ARGS)'
endif

test:
	@$(GRADLEW) test

clean:
	@$(GRADLEW) clean