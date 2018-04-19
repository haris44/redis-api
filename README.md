![logo](https://i.imgur.com/qFUHvPW.jpg)

# Kubernetes-Redis

k8s-redis est un ensemble de projets réalisés dans le cadre d'un cours de **Redis** 
Celui-ci s'articule autour de plusieurs composants, qui seront tous présentés dans ce readme. 

Le projet initial doit permettre de visualiser sur une carte toutes les personnes qui sont aux alentours (en fonction du zoom de la map)  
Il s'articule autour d'une DB NoSQL Redis, d'une API et d'un front-end. 

Nous avons décidé de compléter le projet avec la découverte d'un outil et d'un framework : Kubernetes et Akka-HTTP

Concernant la partie Frontend, nous avons couplé nos connaissances d'OpenLayers3 avec un framework front nouveau pour nous : VueJS

### Développeurs : 

* Nathan Grimaud [@grimaudnathan](https://github.com/NathanGrimaud) (Front)
* Denyse Sylvain [@SylvainDNS](https://github.com/sylvainDNS) (Node/Socket.io) 
* Bouriez Cyril [@B-Cyril](https://github.com/https://github.com/B-cyril) (Cordova) 
* Bertrand Alexandre [@haris44](https://github.com/haris44/) (Scala Akka-HTTP)

### Système et réseau : 

* Balaud Bastien [@BastienBalaud](https://github.com/BastienBalaud)

## Architecture logicielle du projet

Nous allons détailler ici les choix d'architecture

### Back-end

Pour le projet, nous avons décidé de coupler la puissance de Akka-Http et la scalabilité de Kubernetes. 
Le principe est simple, l'Actor System de chaque application est connecté en mode "cluster" avec les autres. 

#### Actor System

Le système d'Acteur est inspiré d'Erlang. Pour faire simple, chaque "bloc" de l'application est complètement séparé, aussi bien en termes de partages de variables que de ressources (chaque acteur doit être **indépendant** en RAM, en CPU ...) 

Chaque passage de données se fait via le message passing implémenter par le language ou le framework. Ainsi, l'application est totalement **asynchrone et multithreadé**. 

Chaque acteur possède également **une adresse** réseau (et peut communiquer avec les autres via TCP). Ainsi, il est très facile de distribuer l'application, sans aucune modification de code. Ainsi, l'application peut tourner en mode standalone, puis se scale automatiquement lorsque une montée en charge apparait. 

C'est exactement ce que nous avons voulu faire avec notre application. 

#### Akka HTTP et Kubernetes 

Nous avons décidé d'utiliser ce modèle avec un language que Alexandre connaissait et affectionnait déja : Scala. (Akka étant disponible également pour Java, avec les mêmes principes) 

Pour la partie scalabilité, nous avons décidé d'utiliser Kubernetes. Et cela tombe bien, car SBT (Gestionnaire de paquet de Scala) intégre un module pour créer une image Docker, et Akka-HTTP permet de s'interconnecter avec Kubernetes, et ainsi récupéré les adresses des actor-system des nouveaux arrivant automatiquement 

Ainsi, la route ``GET /cluster`` permet d'avoir une vision en temps réel de son cluster. 


####  Les avantages de ce système sont très nombreux : 

* Très adapté pour de **petit projet**, car il permet de ne pas avoir à séparer son code comme en micro-services, tout en gardant une scalabilité optimale (Pas besoin de créer une API Gateway + utiliser RabbitMQ + créer des microservices par exemples)  
* Intègre un système de Message-Passing, et donc évite l'utilisation d'outil comme RabbitMQ
* Multithreadé et asynchrone par défaut
* Permet de gérer de la haute disponibilité
* Simple d'utilisation, mécanisme agréable

#### Bibliographie autour de Akka-HTTP et k8s :

[Doc officiel de Lightbend permettant de réaliser la connexion avec Kube](https://developer.lightbend.com/guides/akka-cluster-kubernetes-k8s-deploy/)

[Minikube & AkkaHTTP](https://www.lotharschulz.info/2016/10/19/akkahttp-docker-kubernetes/)

[Exemple d'intégration par Typesafe](https://github.com/typesafehub/prod-suite-management-doc/tree/master/guides/akka-cluster-kubernetes-k8s-deploy/akka-cluster-example)

#### Docker-repo :

Afin de nous faciliter la tache, nous avons créé un docker repository sur un serveur à côté du Kubernetes. En effet, en cas de crash complet, il nous suffit de relancer un cluster neuf, et de relancer toutes les template, sans avoir besoin de repartir du code.   

(Les JSON de déploiement sont en repo privée, n'hesitez pas à demander pour y avoir accès) 

#### Redis

Nous avons utilisé Redis afin de stocker nos données. Avec le sujet, c'était la seule brique imposé. Nous utilisons donc le système de GEORADIUS, ainsi que des SET.

Redis est déployé dans Kubernetes, avec une persistance assurée sur un GlusterFS (cf Archi Réseau)

Modèle : 

```
SET users:$USERNAME {username, name, password}
GEO maps $UUID {lat, lng}
SET userGeo:$UUID {username, timestamp, lat, lng}
```

### Frontend :

La partie front-end est basé sur VueJS OpenLayers3. OpenLayers offre une API très puissante pour gérer toute la partie cartographie, mais manipule beaucoup le DOM (et est ainsi assez peu utilisable avec React par exemple, qui lui oblige l'utilisation d'un Virtual DOM, pas supporté par OpenLayers). 

Ainsi, nous avons fait le choix d'utiliser VueJS, et d'optimiser l'affichage pour mobile. 


## Architecture système du projet

Ce système est quasiment entièrement scalable, grâce aux choix architecturaux qui ont été faits. 


![sys](https://i.imgur.com/2mzlFo8.png)


### Processus de déploiement de l'API 

Il suffit de recréer l'image docker et de la mettre à jour sur le Kubernetes 


## Références

Variable d'env de l'application : 

```
[{
        "name": "AKKA_ACTOR_SYSTEM_NAME",
        "value": "redis-k8s"
    },
    {
        "name": "AKKA_REMOTING_BIND_PORT",
        "value": "2551"
    },
    {
        "name": "AKKA_REMOTING_BIND_HOST",
        "value": "$HOSTNAME.redis-k8s.default.svc.cluster.local"
    },
    {
        "name": "AKKA_SEED_NODES",
        "value": "redis-k8s-0.redis-k8s.default.svc.cluster.local:2551,redis-k8s-1.redis-k8s.default.svc.cluster.local:2551,redis-k8s-2.redis-k8s.default.svc.cluster.local:2551"
    },
    {
        "name": "HTTP_HOST",
        "value": "0.0.0.0"
    },
    {
        "name": "HTTP_PORT",
        "value": "9000"
    },
    {
        "name": "CLUSTER_MEMBERSHIP_ASK_TIMEOUT",
        "value": "5000"
    },
    {
        "name": "REDIS_URL",
        "value": ""
    },
    {
        "name": "REDIS_PORT",
        "value": "6379"
    }, 
    {
        "name": "JWT_TOKEN",
        "value": ""
    }
}]
```







