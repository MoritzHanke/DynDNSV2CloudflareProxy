FROM amazoncorretto:8-alpine
WORKDIR /usr/APP/

COPY DyndnsV2_Cloudflare_Proxy.jar DyndnsV2_Cloudflare_Proxy.jar



EXPOSE 80 443

CMD ["java", "-jar", "DyndnsV2_Cloudflare_Proxy.jar", "Main"]