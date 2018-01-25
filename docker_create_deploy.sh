ip=`cat /var/tmp/publicip`
chmod 400 PWC-DEMO-K.pem
cd ${WORKSPACE}
ls -lrt
cp ${WORKSPACE}/target/Presentation-0.0.1-SNAPSHOT.war .
chmod +x docker_build.sh
sudo ./docker_build.sh
sudo docker save pwc_demo -o pwc_demo_${BUILD_NUMBER}.tar
sudo /usr/bin/gzip pwc_demo_${BUILD_NUMBER}.tar
sudo chown jenkins:jenkins pwc_demo_${BUILD_NUMBER}.tar.gz 
scp -i PWC-DEMO-K.pem -o "StrictHostKeyChecking no" pwc_demo_${BUILD_NUMBER}.tar.gz ec2-user@${ip}:/var/tmp/ 
ssh -i PWC-DEMO-K.pem ec2-user@${ip} /bin/bash <<EOF
cd /var/tmp/
sudo docker stop pwc_demo
sudo docker rm pwc_demo
gunzip pwc_demo_${BUILD_NUMBER}.tar.gz
sudo docker load -i pwc_demo_${BUILD_NUMBER}.tar
sudo docker run   -d --name pwc_demo -p 9080:8080 pwc_demo
EOF

