clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew install

run-dist:
	./build/install/app/bin/app

generate-migrations:
	./gradlew generateMigrations

run:
	./gradlew run

test:
	./gradlew test

lint:
	./gradlew checkstyleMain checkstyleTest

check-updates:
	./gradlew dependencyUpdates

.PHONY: build