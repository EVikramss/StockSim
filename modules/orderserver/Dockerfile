FROM public.ecr.aws/amazoncorretto/amazoncorretto:22-headful
COPY OrderServer.war /tmp/OrderServer.war
WORKDIR /tmp
EXPOSE 8101
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "OrderServer.war"]