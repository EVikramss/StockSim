FROM public.ecr.aws/amazoncorretto/amazoncorretto:22-headful
RUN yum update -y && yum install -y bind-utils
COPY orderserver.war /tmp/orderserver.war
WORKDIR /tmp
EXPOSE 8101
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "orderserver.war"]
