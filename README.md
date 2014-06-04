ICGC DCC - System
===

Parent project of the ICGC DCC system.

Setup
---

Clone the repository:

`git clone https://github.com/icgc-dcc/dcc.git`

Install Maven 3.2.1:

[http://maven.apache.org/download.cgi](http://maven.apache.org/download.cgi)
	
Install MongoDB 2.4.1:

[http://www.mongodb.org/downloads](http://www.mongodb.org/downloads)

Install PostgreSQL 9.2.4:

[http://www.postgresql.org/download/](http://www.postgresql.org/download/)

Install ElasticSearch 0.90.1:
	
[http://www.elasticsearch.org/downloads](http://www.elasticsearch.org/downloads)

Install UI development environment:
	
- [dcc-submission-ui](dcc-submission/dcc-submission-ui/README.md)
- [dcc-portal-ui](dcc-portal/dcc-portal-ui/README.md)

Extract reference genome to `/tmp`

- http://seqwaremaven.oicr.on.ca/artifactory/simple/dcc-dependencies/org/icgc/dcc/dcc-reference-genome

Build
---

To build, test and install _all_ modules in the system:

`mvn`
	
To build, test and install _only_ the Submission sub-system modules:

`mvn -amd -pl dcc-submission`

To build, test and install _only_ the ID sub-system module:

`mvn -amd -pl dcc-identifier`

To build, test and install _only_ the ETL sub-system modules:

`mvn -amd -pl dcc-etl`

To build, test and install _only_ the Download sub-system modules:

`mvn -amd -pl dcc-downloader`
	
To build, test and install _only_ the Portal sub-system modules:

`mvn -amd -pl dcc-portal`
	
Run
---

See specific module documentation below.

Modules
---
Top level system modules:

- [Submission](dcc-submission/README.md)
- [ID](dcc-identifier/README.md)
- [ETL](dcc-etl/README.md)
- [Download](dcc-downloader/README.md)
- [Portal](dcc-portal/README.md)
	
Changes
---
Change log for the user-facing system modules may be found [here](CHANGES.md).
