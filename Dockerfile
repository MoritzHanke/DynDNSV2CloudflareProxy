FROM openjdk:8-alpine
WORKDIR /usr/APP/

#RUN npm install maven

#COPY pom.xml ./
#RUN mvn dependency:resolve

#RUN mkdir -p "dependencies"
#RUN mvn dependency:copy-dependencies -DoutputDirectory=/dependencies

COPY target/DyndnsV2_Cloudflare_Proxy.jar DyndnsV2_Cloudflare_Proxy.jar

EXPOSE 5000 5001

CMD ["java", "-jar", "DyndnsV2_Cloudflare_Proxy.jar", "Main"]