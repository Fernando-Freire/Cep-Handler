FROM fernandofreire/javagolang:latest
# basic image with go and maven pre installed
ENV GO111MODULE on
# go variable necessary for build
RUN mkdir /cep-worker
COPY . /cep-worker
WORKDIR /cep-worker
RUN go get k8s.io/client-go@v0.17.0 && go get k8s.io/apimachinery@release-1.17 && go build main.go
# building go program for controlling k8s pods
RUN mvn -Dmaven.test.skip=true package
# packaging jar - test are done on git repo
CMD  java -jar target/CEPWorker-1.0.jar