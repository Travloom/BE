# 1단계: JAR 파일 실행만 하는 가벼운 환경
FROM eclipse-temurin:21-jre
WORKDIR /app

# 로컬에서 빌드한 JAR 파일 복사
COPY build/libs/*.jar app.jar
# COPY target/*.jar app.jar     # Maven 사용 시 주석 해제

# Spring Boot 포트 오픈
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]