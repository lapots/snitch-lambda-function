appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
    }
}

appender("FILE", FileAppender) {
    file = 'snitch-log.txt'
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
    }
}

logger("com.lapots.breed.snitch", INFO)
root(DEBUG, ["STDOUT", "FILE"])