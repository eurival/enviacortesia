mvn clean package
docker login
docker build -t cortesia-processor-service .
docker tag cortesia-processor-service:latest eurival/cortesia-processor-service:latest 
docker push eurival/cortesia-processor-service:latest  
echo "Processo concluido"
