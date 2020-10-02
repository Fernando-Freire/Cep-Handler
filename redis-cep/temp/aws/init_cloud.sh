#!/bin/bash

#gcloud init

eksctl create cluster --name experiment --nodes-min 4 --nodes-max 20 --node-type r3.large --node-volume-size 20 --asg-access

#kubectl apply -f begin_rabbit.yaml


#cd InputSimilarity/
#docker build --no-cache -t fernandofreire/cep-worker:inputsimilarity . 
#docker push fernandofreire/cep-worker:inputsimilarity
#cd ..
#cd StateUsage/
#docker build --no-cache -t fernandofreire/cep-worker:stateusage . 
#docker push fernandofreire/cep-worker:stateusage
#cd ..
