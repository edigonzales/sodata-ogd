# sodata-ogd


## Konfigurieren und Starten

Die Anwendung kann am einfachsten mittels Env-Variablen gesteuert werden. Es stehen aber auch die normalen Spring Boot Konfigurationsmöglichkeiten zur Verfügung (siehe "Externalized Configuration").

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `CONFIG_DIR` | Pfad des Verzeichnisses mit den Meta-XTF-Dateien der Datensätze. | `/config/` |

### Java

```
java -jar sodata-ogd-server/target/sodata-ogd-exec.jar --app.configDir=sodata-ogd-server/src/test/resources/config
```

### Native Image

Analog Java:

```
./sodata-ogd-server/target/sodata-ogd-server --app.configDir=sodata-ogd-server/src/test/resources/config
```

### Docker

Die _meta-xxxx.xtf_-Dateien können direkt in das Image gebrannt werden. In diesem Fall sollten sie in den Ordner _/config/_ gebrannt werden, was zu folgendem Start-Befehl führt:

```
docker run -p8080:8080 sogis/sodata-ogd:latest
```

Wird die Datei nicht in das Image gebrannt, ergibt sich folgender Befehl:

```
docker run -p8080:8080 -v $PWD/src/test/resources/config/:/config/ sogis/sodata-ogd-jvm:latest
```


## Externe Abhängigkeiten

## Konfiguration und Betrieb in der GDI

## Interne Struktur

## Entwicklung

### Run 

First Terminal:
```
./mvnw spring-boot:run -Penv-dev -pl *-server -am -Dspring-boot.run.profiles=dev
```

Second Terminal:
```
./mvnw gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:
```
./mvnw gwt:codeserver -pl *-client -am -nsu 
```

### Build

#### JVM
```
./mvnw -Penv-prod clean package
```

```
cd sodata-ogd-server
docker build -t sogis/sodata-ogd-jvm:latest -f Dockerfile.jvm .
```


#### Native
Damit die Tests mit dem Native Image funktionieren, muss mittels Env-Variablen passend konfiguriert werden:

```
CONFIG_DIR=$PWD/sodata-ogd-server/src/test/resources/config/ ./mvnw clean -Pnative test
CONFIG_DIR=$PWD/sodata-ogd-server/src/test/resources/config/ ./mvnw -DskipTests -Penv-prod,native package
```

```
cd sodata-ogd-server
docker build -t sogis/sodata-ogd:latest -f Dockerfile.native-alpaquita .
```

In diesem Fall muss das Native Image auf Linux gebuildet werden.

