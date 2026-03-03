This repository contains documents and code to support this blog post:
XXXX

The file ***MarchAcrossTheBelts.pdf*** is used as an example homework, and is just a dump of this Wikipedia page \
https://en.wikipedia.org/wiki/March_Across_the_Belts

Compile the project with
```
mvn clean compile
```

And run it like this:
```
mvn exec:java -Dexec.args="DB_URL DB_NAME USER PASSWORD API_KEY"
```

***DB_URL*** is the URL to a Neo4j instance with the homework data (e.g. ```neo4j://localhost:7687``` or ```neo4j+s://xxxxxxxx.databases.neo4j.io```)\
***DB_NAME*** is the name of the Database within the Neo4j instance, usually this is just ```neo4j```\
***USER*** is your Neo4j user name
***PWD*** is your Neo4j user password
***API_KEY*** is an API key for OpenAI that you need to request from your OpenAI user account
