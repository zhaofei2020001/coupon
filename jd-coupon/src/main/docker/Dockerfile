FROM java:8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ADD jd-coupon-1.0-SNAPSHOT.jar /jd-coupon-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","jd-coupon-1.0-SNAPSHOT.jar"]
EXPOSE 8088