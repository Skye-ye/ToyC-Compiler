GRADLEW := gradlew.bat
GRADLEW_ARGS := --warning-mode all

.PHONY: all build run test clean
all: build

build:
	@$(GRADLEW) build $(GRADLEW_ARGS)

run:
ifndef ARGS
	@echo "Usage: make run ARGS='<arguments>'"
	@echo "Example: make run ARGS='-op=options.yml'"
	@echo "Example: make run ARGS='--help'"
else
	@$(GRADLEW) run $(GRADLEW_ARGS) --args='$(ARGS)'
endif

test:
	@$(GRADLEW) test $(GRADLEW_ARGS)

clean:
	@$(GRADLEW) clean