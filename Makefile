clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew install

start:
	APP_ENV=development ./gradlew run

install:
	./gradlew install

start-dist:
	APP_ENV=production ./build/install/app/bin/app

generate-migrations:
	./gradlew generateMigrations

run:
	./gradlew run

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

check-updates:
	./gradlew dependencyUpdates

lint:
	./gradlew checkstyleMain checkstyleTest

check-updates:
	./gradlew dependencyUpdates

.PHONY: build