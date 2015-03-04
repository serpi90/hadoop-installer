# HadoopInstaller 1.0 #

## What is this for? ##

HadoopInstaller is a software to remotely deploy Apache Hadoop and it's configuration files in a linux cluster.

## Development

For developement, please add the src/main folder to your classpath.

## Runtime

### Set Up ###

#### Requires ####

* SSH access to the cluster using passwordless authentication.
* A tar bundle of Hadoop 2.5.1
* A tar bundle of JRE 7 x64

#### Setup summary ####
* Put the hadoop and java bundles in the `dependencies` directory.
* Configure the installer using the `configuration.xml` file.
* `java -jar HadoopInstaller.jar`

## Contact ###

You can contact me at *serpi90@gmail.com*