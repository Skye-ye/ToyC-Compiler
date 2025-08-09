GRADLEW := ./gradlew
GRADLEW_ARGS := --warning-mode all

.PHONY: all build compile run test clean dot
all: build

build:
	@$(GRADLEW) build $(GRADLEW_ARGS)

run:
ifndef ARGS
	$(error ARGS is not set. Usage: make run ARGS='<arguments to pass to the program>')
endif
	@$(GRADLEW) run $(GRADLEW_ARGS) --args='$(ARGS)'

test:
	@$(GRADLEW) test $(GRADLEW_ARGS)

clean:
	@$(GRADLEW) clean