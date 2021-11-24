# docker-commands

### npm clean 
`npm cache clean --force`

### clean image
`rm -rf /var/lib/apt/lists/*`

### clone git from a single branch and latest commit
`git clone --branch <branchName> --single-branch --depth 1 ..`

### for all commands (manage command and sub command)
`docker`

### check docker version
`docker version`

### config values of engine
`docker info`

### delete all containers 
`docker rm -vf $(docker ps -a -q)`

`docker container prune`

### delete all images
`docker rmi -f $(docker images -a -q)`

`docker image prune`

### delete all volumes
`docker volume prune`

### clean up images, containers, volumes, and networks
`docker system prune`

### run container port with default latest version
`docker container run --publish/-p <hostPort>:<containerPort> <imageName>`

`docker run --publish/-p <hostPort>:<containerPort> <imageName>`

### run container port with version
`docker container run --publish/-p <hostPort>:<containerPort> <imageName>:<version>`

### run container port name
`docker container run --publish/-p <hostPort>:<containerPort> --name <containerName> <localhostPort>:<containerPort> <imageName>`

### run container port name environment
`docker container run --publish/-p <hostPort>:<containerPort> --name <containerName> --env/-e <key>=<value> <localhostPort>:<containerPort> <imageName>`

### run container port detach
`docker container run --publish/-p <hostPort>:<containerPort> --detach/-d <localhostPort>:<containerPort> <imageName>`

### automatically remove the container when it exits
`docker container run .. --rm`

### list running container
`docker container ls`

`docker ps`

### list all container
`docker container ls -a`

`docker ps -a`

### stop running container
`docker container stop <id>`

### start container
`docker container start <id/name>`

### log container
`docker container logs <id/name>`

### log container.. watching 
`docker container logs -f <id/name>`

### details of one container config
`docker container inspect <id/name>`

### performance stats for all containers
`docker container stats`

### running process of a container / process list in a container
`docker container top <id/name>`

### removal of a container
`docker container rm -f(force running container) <id/name>`

### start new container interactively
`docker container run -it .. bash`

`docker container run -it .. sh`

### run additional command in existing running container
`docker container exec -it <name> bash`

`docker container exec -it <name> sh`

### start existing stopped container interactively
`docker container start -ai  <name>`

### pull an image
`docker pull <imageName>`

### container port check
`docker container port <name>`

### container network ip address
`docker container inspect --format '{{.NetworkSettings.IPAddress}}' <name>`

### show netwrok list
`docker network ls`

### inspect a network
`docker netwrok inspect <name>`

### create a network default driver
`docker network create  <name>`

### create a network with thirty party driver
`docker network create  <name> --driver <thirdpirtydriverName>`

### attach a network to container
`docker network connect <networkId> <containerId>`

### detach a network from container
`docker netwrok disconnect <networkId> <containerId>`

### run container within a network
`docker container run --network/--net <networkName> .. `

### inside a network containers connecting with each other
`docker container exec -it <containername1fromNetwork1> <containername2fromNetwork1>`

### creating a container with alias 
`docker container run --network/net <networkName>  --network-alias/--net-alias <alias> ..` 
 
example:

`docker container run -d --net my-app --net-alias search elasticsearch:2`

`docker container run -d --net my-app --net-alias search elasticsearch:2`

`docker container run --rm --net my-app alpine nslookup search`

we will get two containers with same alias

`docker container run --rm --net my-app centos curl -s search:9200`

we will alternatively get those two containers --> load balancing .. this will create two container with different name from same image and they have same alias

### list of images
`docker image ls`

### history of image
`docker history  <imageName> : <imageTag>`

### inspect image
`docker image inspect  <name>`

### retagging image
`docker image tag <imageName> <newimageTag>: <newimageTag>`

### push image
`docker image push <imageName>: <imageTag>`

### docker login
`docker login`

### docker build image from some docker file not default Dockerfile 
`docker build -t  <imageName> -f <dockerfileName> .`

### docker build image from default Dockerfile 
`docker build -t <imageName> .`

### docker push image
`docker push <imageName> `

### list docker volumes
`docker volume ls`

### inspect a volume
`docker volume inspect <id/name>`

### create named volumes in docker container run
`docker container run -v <volumeName>:<volumeofImage> ..`

### volume creation
`docker volume create `

### bind mount starts with // in windows and / in mac
`docker container run -v <//c/users/..(full path)>:</path/container> ..`

`docker container run -v $(pwd):</path/container> ..`

### docker compose not default file
`docker compose -f <ymlfileName> ..`

### setup volumes/networks and start all containers
`docker-compose up`

### setup volumes/networks and start all containers in the background
`docker-compose up -d`

### rebuild docker compose
`docker-compose build`

`docker-compose up --build`

### stop all containers and remove cont/vol/net
`docker-compose down`

### remove all volumes with compose down
`docker-compose down -v`

### remove local images with compose down (only do it when no custom tag is set by the image)
`docker compose down --rmi local`

example: to delete image with this command --> build image with no image: in docker-compose.yml file

### remove all related images with compose down 
`docker compose down --rmi all`

### other docker compose commands
`docker-compose logs`

`docker-compose top`

`docker-compose ps`

### swarm commands
`docker swarm`

`docker node`

`docker update`

`docker stack`

`docker secret`

### check if swarm is active
`docker info`

(Swarm:inactive)

### swarm initialization
`docker swarm init --advertise-addr <ipaddress>`

### list of nodes
`docker node ls`

### create a service
`docker service create <imageName>`

### create a service with replicas
`docker service create <imageName>`

### create a service and ping 
`docker service create --name <serviceName> --replicas <noofreplicas> <imageName> ping <address>`

### list of services
`docker service ls`

### details of a service
`docker service ps <serviceName>`

### update docker service (scale up)
`docker service update <serviceId/Name> --replicas <noofreplica>`

### logs docker service
`docker service logs <serviceName>`

### remove docker service
`docker service rm <serviceName>`

### inspect docker service
`docker service inspect <serviceName>`

### create node
`docker-machine create <nodeName>`

### start the node(now docker machine will talk to my node instead of default one check docker info --> name)
`docker-machine ssh <nodeName>`

`docker-machine env <nodeName>`

### swarm join a node 
`docker swarm join --token <token> <ipaddress>`

### update the role of a node
`docker node update --role <roleName> <nodeName>`

### get token to add a node as a role
`docker swarm join-token <roleName>`

### list of nodes in the service
`docker node ps`

### details of a node
`docker node ps <nodeName>`

### docker creation network with driver 
`docker netwrok create --driver <driverName> <networkName>`

### create a service and connect to a network 
`docker service create --name <serviceName> --network <networkName> --e  <key>=<value> --replicas <noofreplicas> <imageName> ping <address>`

### create a service and connect to a port 
`docker service create --name <serviceName> -p <hostPort>:<container  Port> --network <networkName> --e  <key>=<value> --replicas <noofreplicas> <imageName> ping <address> `
 
### named volume for perserving data
`docker service cfeate --mount type=volume,source=db-data,target=/var/lib/postgresql/data   ..`

###  stack deploy in docker(-c = compose)
`docker stack deploy -c <ymlfilename> <stackName>`

### list of stacks
`docker stack ls`

### info about a stack
`docker stack ps <stackName>`

### all info about services in stack
`docker stack services <serviceName>`

### secret creation using docker 
`docker secret create <secretName> <secretfileName>`

### set the value for a secret using docker secret
`echo "<secretvalue>" | docker secret create <secretfileName> - `

### secret list
`docker secret ls`

### inspect secret
`docker secret inspect <secretName>`

### creating secret with service
`docker service create --name <serviceName> --secret <secretName> --secret <secretName2> -e <key>=/run/secrets/<secretName> -e <key>=/run/secrets/<secretName2> <imageName>`

### logs of service
`docker logs <serviceName>`

### remove secret
`docker service update --secret-rm `

### add secret 
`docker service update --secret-add`

### remove stack 
`docker stack rm <stackName>`

### compose exec (not recommended)
`docker-compose exec <name> cat /run/secrets/<secretName>`

### docker compose for test
`docker-compose -f <normalYML> -f <testYML> up -d`

### docker compose for prod
`docker-compose -f <normalYML> -f <prodYML> config`

`docker-compose -f <normalYML> -f <prodYML> config <outputYML>`

### change no of replicas of multiple service
`docker service scale <servicename1>=<replicaforservice1> <servicename2>=<replicaforservice2 >`

`docker service rollback web`

### update image of the service
`docker service update --image <imageName>:<tag> <servicename>`

### adding env var to service and removing port and adding port
`docker service update --env-add <key>=<value> --publish-rm <hostport> --publish-add <newhostPort>:<containerPort>`

### cause the tasks to be recreated anyway
`docker service update --force <serviceName>`

### container attach
`docker attach <containerName>`
