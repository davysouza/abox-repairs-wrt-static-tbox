#! /bin/bash

EXPERIMENT_PATH=$1
EXPERIMENT_CLASS=$2

if [ -z ${GITHUB_WORKSPACE+x} ]; then
    WORKSPACE="."
else
    WORKSPACE=$GITHUB_WORKSPACE
fi

# Linux classpath format
CLASSPATH="${WORKSPACE}/app/experiments/${EXPERIMENT_PATH}/target/classes:/home/runner/.m2/repository/org/apache/logging/log4j/log4j-api/2.14.0/log4j-api-2.14.0.jar:/home/runner/.m2/repository/org/apache/logging/log4j/log4j-core/2.14.0/log4j-core-2.14.0.jar:/home/runner/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.14.0/log4j-slf4j-impl-2.14.0.jar:/home/runner/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:${WORKSPACE}/core/target/classes:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-distribution/5.1.17/owlapi-distribution-5.1.17.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-compatibility/5.1.17/owlapi-compatibility-5.1.17.jar:/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.10/jackson-core-2.9.10.jar:/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.9.10.5/jackson-databind-2.9.10.5.jar:/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.9.10/jackson-annotations-2.9.10.jar:/home/runner/.m2/repository/org/apache/commons/commons-rdf-api/0.5.0/commons-rdf-api-0.5.0.jar:/home/runner/.m2/repository/org/tukaani/xz/1.6/xz-1.6.jar:/home/runner/.m2/repository/org/slf4j/jcl-over-slf4j/1.7.22/jcl-over-slf4j-1.7.22.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-model/3.4.3/rdf4j-model-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-api/3.4.3/rdf4j-rio-api-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-languages/3.4.3/rdf4j-rio-languages-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-datatypes/3.4.3/rdf4j-rio-datatypes-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-binary/3.4.3/rdf4j-rio-binary-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-n3/3.4.3/rdf4j-rio-n3-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-nquads/3.4.3/rdf4j-rio-nquads-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-ntriples/3.4.3/rdf4j-rio-ntriples-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-rdfjson/3.4.3/rdf4j-rio-rdfjson-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-jsonld/3.4.3/rdf4j-rio-jsonld-3.4.3.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpclient/4.5.10/httpclient-4.5.10.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpcore/4.4.12/httpcore-4.4.12.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpclient-cache/4.5.10/httpclient-cache-4.5.10.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-rdfxml/3.4.3/rdf4j-rio-rdfxml-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-trix/3.4.3/rdf4j-rio-trix-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-turtle/3.4.3/rdf4j-rio-turtle-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-trig/3.4.3/rdf4j-rio-trig-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-hdt/3.4.3/rdf4j-rio-hdt-3.4.3.jar:/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-util/3.4.3/rdf4j-util-3.4.3.jar:/home/runner/.m2/repository/com/github/jsonld-java/jsonld-java/0.13.0/jsonld-java-0.13.0.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpclient-osgi/4.5.10/httpclient-osgi-4.5.10.jar:/home/runner/.m2/repository/commons-codec/commons-codec/1.11/commons-codec-1.11.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpmime/4.5.10/httpmime-4.5.10.jar:/home/runner/.m2/repository/org/apache/httpcomponents/fluent-hc/4.5.10/fluent-hc-4.5.10.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpcore-osgi/4.4.12/httpcore-osgi-4.4.12.jar:/home/runner/.m2/repository/org/apache/httpcomponents/httpcore-nio/4.4.12/httpcore-nio-4.4.12.jar:/home/runner/.m2/repository/com/github/vsonnier/hppcrt/0.7.5/hppcrt-0.7.5.jar:/home/runner/.m2/repository/com/github/ben-manes/caffeine/caffeine/2.8.1/caffeine-2.8.1.jar:/home/runner/.m2/repository/org/checkerframework/checker-qual/3.1.0/checker-qual-3.1.0.jar:/home/runner/.m2/repository/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar:/home/runner/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:/home/runner/.m2/repository/commons-io/commons-io/2.6/commons-io-2.6.jar:/home/runner/.m2/repository/au/csiro/elk-owlapi5/0.5.0/elk-owlapi5-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-owlapi4/0.5.0/elk-owlapi4-0.5.0-sources.jar:/home/runner/.m2/repository/au/csiro/elk-owlapi4/0.5.0/elk-owlapi4-0.5.0-test-sources.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-apibinding/5.1.10/owlapi-apibinding-5.1.10.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-parsers/5.1.10/owlapi-parsers-5.1.10.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-oboformat/5.1.10/owlapi-oboformat-5.1.10.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-tools/5.1.10/owlapi-tools-5.1.10.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-rio/5.1.10/owlapi-rio-5.1.10.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-api/5.1.10/owlapi-api-5.1.10.jar:/home/runner/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar:/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-impl/5.1.10/owlapi-impl-5.1.10.jar:/home/runner/.m2/repository/org/liveontologies/puli/0.1.0/puli-0.1.0.jar:/home/runner/.m2/repository/org/liveontologies/owlapi-proof/0.1.0/owlapi-proof-0.1.0.jar:/home/runner/.m2/repository/com/google/guava/guava/18.0/guava-18.0.jar:/home/runner/.m2/repository/au/csiro/elk-owl-model/0.5.0/elk-owl-model-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-owl-implementation/0.5.0/elk-owl-implementation-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-reasoner/0.5.0/elk-reasoner-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-proofs/0.5.0/elk-proofs-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-common/0.5.0/elk-util-common-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-concurrent/0.5.0/elk-util-concurrent-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-hashing/0.5.0/elk-util-hashing-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-io/0.5.0/elk-util-io-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-collections/0.5.0/elk-util-collections-0.5.0.jar:/home/runner/.m2/repository/au/csiro/elk-util-logging/0.5.0/elk-util-logging-0.5.0.jar:/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-owlapi/0.7.0/rulewerk-owlapi-0.7.0.jar:/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-core/0.7.0/rulewerk-core-0.7.0.jar:/home/runner/.m2/repository/org/apache/commons/commons-lang3/3.9/commons-lang3-3.9.jar:/home/runner/.m2/repository/org/apache/commons/commons-csv/1.5/commons-csv-1.5.jar:/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-vlog/0.7.0/rulewerk-vlog-0.7.0.jar:/home/runner/.m2/repository/org/semanticweb/rulewerk/vlog-java/1.3.4/vlog-java-1.3.4.jar"

# Windows classpath format
# CLASSPATH="${WORKSPACE}\\app\\experiments\\${EXPERIMENT_PATH}\\target\\classes;C:\\Users\\davys\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-api\\2.14.0\\log4j-api-2.14.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-core\\2.14.0\\log4j-core-2.14.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\logging\\log4j\\log4j-slf4j-impl\\2.14.0\\log4j-slf4j-impl-2.14.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.7.25\\slf4j-api-1.7.25.jar;${WORKSPACE}\\core\\target\\classes;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-distribution\\5.1.17\\owlapi-distribution-5.1.17.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-compatibility\\5.1.17\\owlapi-compatibility-5.1.17.jar;C:\\Users\\davys\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.9.10\\jackson-core-2.9.10.jar;C:\\Users\\davys\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.9.10.5\\jackson-databind-2.9.10.5.jar;C:\\Users\\davys\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.9.10\\jackson-annotations-2.9.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\commons\\commons-rdf-api\\0.5.0\\commons-rdf-api-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\tukaani\\xz\\1.6\\xz-1.6.jar;C:\\Users\\davys\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.22\\jcl-over-slf4j-1.7.22.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-model\\3.4.3\\rdf4j-model-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-api\\3.4.3\\rdf4j-rio-api-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-languages\\3.4.3\\rdf4j-rio-languages-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-datatypes\\3.4.3\\rdf4j-rio-datatypes-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-binary\\3.4.3\\rdf4j-rio-binary-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-n3\\3.4.3\\rdf4j-rio-n3-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-nquads\\3.4.3\\rdf4j-rio-nquads-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-ntriples\\3.4.3\\rdf4j-rio-ntriples-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-rdfjson\\3.4.3\\rdf4j-rio-rdfjson-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-jsonld\\3.4.3\\rdf4j-rio-jsonld-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.5.10\\httpclient-4.5.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.4.12\\httpcore-4.4.12.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient-cache\\4.5.10\\httpclient-cache-4.5.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-rdfxml\\3.4.3\\rdf4j-rio-rdfxml-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-trix\\3.4.3\\rdf4j-rio-trix-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-turtle\\3.4.3\\rdf4j-rio-turtle-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-trig\\3.4.3\\rdf4j-rio-trig-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-rio-hdt\\3.4.3\\rdf4j-rio-hdt-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\org\\eclipse\\rdf4j\\rdf4j-util\\3.4.3\\rdf4j-util-3.4.3.jar;C:\\Users\\davys\\.m2\\repository\\com\\github\\jsonld-java\\jsonld-java\\0.13.0\\jsonld-java-0.13.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient-osgi\\4.5.10\\httpclient-osgi-4.5.10.jar;C:\\Users\\davys\\.m2\\repository\\commons-codec\\commons-codec\\1.11\\commons-codec-1.11.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpmime\\4.5.10\\httpmime-4.5.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\fluent-hc\\4.5.10\\fluent-hc-4.5.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore-osgi\\4.4.12\\httpcore-osgi-4.4.12.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore-nio\\4.4.12\\httpcore-nio-4.4.12.jar;C:\\Users\\davys\\.m2\\repository\\com\\github\\vsonnier\\hppcrt\\0.7.5\\hppcrt-0.7.5.jar;C:\\Users\\davys\\.m2\\repository\\com\\github\\ben-manes\\caffeine\\caffeine\\2.8.1\\caffeine-2.8.1.jar;C:\\Users\\davys\\.m2\\repository\\org\\checkerframework\\checker-qual\\3.1.0\\checker-qual-3.1.0.jar;C:\\Users\\davys\\.m2\\repository\\com\\google\\errorprone\\error_prone_annotations\\2.3.4\\error_prone_annotations-2.3.4.jar;C:\\Users\\davys\\.m2\\repository\\com\\google\\code\\findbugs\\jsr305\\3.0.2\\jsr305-3.0.2.jar;C:\\Users\\davys\\.m2\\repository\\commons-io\\commons-io\\2.6\\commons-io-2.6.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-owlapi5\\0.5.0\\elk-owlapi5-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-owlapi4\\0.5.0\\elk-owlapi4-0.5.0-sources.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-owlapi4\\0.5.0\\elk-owlapi4-0.5.0-test-sources.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-apibinding\\5.1.10\\owlapi-apibinding-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-parsers\\5.1.10\\owlapi-parsers-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-oboformat\\5.1.10\\owlapi-oboformat-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-tools\\5.1.10\\owlapi-tools-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-rio\\5.1.10\\owlapi-rio-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-api\\5.1.10\\owlapi-api-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\javax\\inject\\javax.inject\\1\\javax.inject-1.jar;C:\\Users\\davys\\.m2\\repository\\net\\sourceforge\\owlapi\\owlapi-impl\\5.1.10\\owlapi-impl-5.1.10.jar;C:\\Users\\davys\\.m2\\repository\\org\\liveontologies\\puli\\0.1.0\\puli-0.1.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\liveontologies\\owlapi-proof\\0.1.0\\owlapi-proof-0.1.0.jar;C:\\Users\\davys\\.m2\\repository\\com\\google\\guava\\guava\\18.0\\guava-18.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-owl-model\\0.5.0\\elk-owl-model-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-owl-implementation\\0.5.0\\elk-owl-implementation-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-reasoner\\0.5.0\\elk-reasoner-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-proofs\\0.5.0\\elk-proofs-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-common\\0.5.0\\elk-util-common-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-concurrent\\0.5.0\\elk-util-concurrent-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-hashing\\0.5.0\\elk-util-hashing-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-io\\0.5.0\\elk-util-io-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-collections\\0.5.0\\elk-util-collections-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\au\\csiro\\elk-util-logging\\0.5.0\\elk-util-logging-0.5.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\semanticweb\\rulewerk\\rulewerk-owlapi\\0.7.0\\rulewerk-owlapi-0.7.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\semanticweb\\rulewerk\\rulewerk-core\\0.7.0\\rulewerk-core-0.7.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.9\\commons-lang3-3.9.jar;C:\\Users\\davys\\.m2\\repository\\org\\apache\\commons\\commons-csv\\1.5\\commons-csv-1.5.jar;C:\\Users\\davys\\.m2\\repository\\org\\semanticweb\\rulewerk\\rulewerk-vlog\\0.7.0\\rulewerk-vlog-0.7.0.jar;C:\\Users\\davys\\.m2\\repository\\org\\semanticweb\\rulewerk\\vlog-java\\1.3.4\\vlog-java-1.3.4.jar"

# echo $CLASSPATH

echo "LS"
ls

echo "LS WORKSPACE"
ls $WORKSPACE

echo "LS WORKSPACE/APP"
ls $WORKSPACE/app

echo "ls ${WORKSPACE}/app/experiments/${EXPERIMENT_PATH}/target/classes"
ls ${WORKSPACE}/app/experiments/${EXPERIMENT_PATH}/target/classes

echo "PWD"
pwd

# java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath $CLASSPATH de.tudresden.inf.lat.aboxrepair.${EXPERIMENT_CLASS}

# for file in resources/ore2015/*.owl
# do
#     java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath "${WORKSPACE}/app/experiments/${EXPERIMENT_PATH}/target/classes;/home/runner/.m2/repository/org/apache/logging/log4j/log4j-api/2.14.0/log4j-api-2.14.0.jar;/home/runner/.m2/repository/org/apache/logging/log4j/log4j-core/2.14.0/log4j-core-2.14.0.jar;/home/runner/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.14.0/log4j-slf4j-impl-2.14.0.jar;/home/runner/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar;${WORKSPACE}/core/target/classes;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-distribution/5.1.17/owlapi-distribution-5.1.17.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-compatibility/5.1.17/owlapi-compatibility-5.1.17.jar;/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.9.10/jackson-core-2.9.10.jar;/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.9.10.5/jackson-databind-2.9.10.5.jar;/home/runner/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.9.10/jackson-annotations-2.9.10.jar;/home/runner/.m2/repository/org/apache/commons/commons-rdf-api/0.5.0/commons-rdf-api-0.5.0.jar;/home/runner/.m2/repository/org/tukaani/xz/1.6/xz-1.6.jar;/home/runner/.m2/repository/org/slf4j/jcl-over-slf4j/1.7.22/jcl-over-slf4j-1.7.22.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-model/3.4.3/rdf4j-model-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-api/3.4.3/rdf4j-rio-api-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-languages/3.4.3/rdf4j-rio-languages-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-datatypes/3.4.3/rdf4j-rio-datatypes-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-binary/3.4.3/rdf4j-rio-binary-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-n3/3.4.3/rdf4j-rio-n3-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-nquads/3.4.3/rdf4j-rio-nquads-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-ntriples/3.4.3/rdf4j-rio-ntriples-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-rdfjson/3.4.3/rdf4j-rio-rdfjson-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-jsonld/3.4.3/rdf4j-rio-jsonld-3.4.3.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpclient/4.5.10/httpclient-4.5.10.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpcore/4.4.12/httpcore-4.4.12.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpclient-cache/4.5.10/httpclient-cache-4.5.10.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-rdfxml/3.4.3/rdf4j-rio-rdfxml-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-trix/3.4.3/rdf4j-rio-trix-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-turtle/3.4.3/rdf4j-rio-turtle-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-trig/3.4.3/rdf4j-rio-trig-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-rio-hdt/3.4.3/rdf4j-rio-hdt-3.4.3.jar;/home/runner/.m2/repository/org/eclipse/rdf4j/rdf4j-util/3.4.3/rdf4j-util-3.4.3.jar;/home/runner/.m2/repository/com/github/jsonld-java/jsonld-java/0.13.0/jsonld-java-0.13.0.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpclient-osgi/4.5.10/httpclient-osgi-4.5.10.jar;/home/runner/.m2/repository/commons-codec/commons-codec/1.11/commons-codec-1.11.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpmime/4.5.10/httpmime-4.5.10.jar;/home/runner/.m2/repository/org/apache/httpcomponents/fluent-hc/4.5.10/fluent-hc-4.5.10.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpcore-osgi/4.4.12/httpcore-osgi-4.4.12.jar;/home/runner/.m2/repository/org/apache/httpcomponents/httpcore-nio/4.4.12/httpcore-nio-4.4.12.jar;/home/runner/.m2/repository/com/github/vsonnier/hppcrt/0.7.5/hppcrt-0.7.5.jar;/home/runner/.m2/repository/com/github/ben-manes/caffeine/caffeine/2.8.1/caffeine-2.8.1.jar;/home/runner/.m2/repository/org/checkerframework/checker-qual/3.1.0/checker-qual-3.1.0.jar;/home/runner/.m2/repository/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar;/home/runner/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar;/home/runner/.m2/repository/commons-io/commons-io/2.6/commons-io-2.6.jar;/home/runner/.m2/repository/au/csiro/elk-owlapi5/0.5.0/elk-owlapi5-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-owlapi4/0.5.0/elk-owlapi4-0.5.0-sources.jar;/home/runner/.m2/repository/au/csiro/elk-owlapi4/0.5.0/elk-owlapi4-0.5.0-test-sources.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-apibinding/5.1.10/owlapi-apibinding-5.1.10.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-parsers/5.1.10/owlapi-parsers-5.1.10.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-oboformat/5.1.10/owlapi-oboformat-5.1.10.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-tools/5.1.10/owlapi-tools-5.1.10.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-rio/5.1.10/owlapi-rio-5.1.10.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-api/5.1.10/owlapi-api-5.1.10.jar;/home/runner/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar;/home/runner/.m2/repository/net/sourceforge/owlapi/owlapi-impl/5.1.10/owlapi-impl-5.1.10.jar;/home/runner/.m2/repository/org/liveontologies/puli/0.1.0/puli-0.1.0.jar;/home/runner/.m2/repository/org/liveontologies/owlapi-proof/0.1.0/owlapi-proof-0.1.0.jar;/home/runner/.m2/repository/com/google/guava/guava/18.0/guava-18.0.jar;/home/runner/.m2/repository/au/csiro/elk-owl-model/0.5.0/elk-owl-model-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-owl-implementation/0.5.0/elk-owl-implementation-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-reasoner/0.5.0/elk-reasoner-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-proofs/0.5.0/elk-proofs-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-common/0.5.0/elk-util-common-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-concurrent/0.5.0/elk-util-concurrent-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-hashing/0.5.0/elk-util-hashing-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-io/0.5.0/elk-util-io-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-collections/0.5.0/elk-util-collections-0.5.0.jar;/home/runner/.m2/repository/au/csiro/elk-util-logging/0.5.0/elk-util-logging-0.5.0.jar;/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-owlapi/0.7.0/rulewerk-owlapi-0.7.0.jar;/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-core/0.7.0/rulewerk-core-0.7.0.jar;/home/runner/.m2/repository/org/apache/commons/commons-lang3/3.9/commons-lang3-3.9.jar;/home/runner/.m2/repository/org/apache/commons/commons-csv/1.5/commons-csv-1.5.jar;/home/runner/.m2/repository/org/semanticweb/rulewerk/rulewerk-vlog/0.7.0/rulewerk-vlog-0.7.0.jar;/home/runner/.m2/repository/org/semanticweb/rulewerk/vlog-java/1.3.4/vlog-java-1.3.4.jar" de.tudresden.inf.lat.aboxrepair.${EXPERIMENT_CLASS}
# done