# sodata-ogd


## Konfigurieren und Starten

Die Anwendung kann am einfachsten mittels Env-Variablen gesteuert werden. Es stehen aber auch die normalen Spring Boot Konfigurationsmöglichkeiten zur Verfügung (siehe "Externalized Configuration").

| Name | Beschreibung | Standard |
|-----|-----|-----|
| `CONFIG_FILE` | Vollständiger, absoluter Pfad der Themebereitstellungs-Konfigurations-XML-Datei. | `/config/datasearch.xml` |
| `ITEMS_GEOJSON_DIR` | Verzeichnis, in das die GeoJSON-Dateien der Regionen gespeichert werden. Sämtliche JSON-Dateien in diesem Verzeichnis werden öffentlich exponiert. | `#{systemProperties['java.io.tmpdir']}` (= Temp-Verzeichnis des OS) |
| ~~`FILES_SERVER_URL`~~ | ~~Url des Servers, auf dem die Geodaten gespeichert sind.~~ | ~~`https://files.geo.so.ch`~~ |

### Java

```
java -jar sodata-ogd-server/target/sodata-ogc-exec.jar --app.configDir=/path/to/config
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
