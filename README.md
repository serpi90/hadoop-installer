# HadoopInstaller 1.0 #

## What is this for? ##

HadoopInstaller is a software to remotely deploy Apache Hadoop and it's configuration files in a linux cluster.

## Runtime

Run with `java -jar HadoopInstaller.jar [-log:<level>] [-deploy]`

 - -deploy indicates that the software bundles should be uploaded and uncompressed. (else only the configuration files are uploaded)
 - -log:<level> indicates the level of logging to use.
   - Where level is any of: all,off,trace,debug,info,warn,error,fatal. Defaults to *info*.

### Set Up ###

#### Requires ####

* SSH access to the cluster using passwordless authentication.
  * From the installation terminal to all the cluster.
  * From the master to all the slaves as required by Hadoop.
* A tar bundle of Hadoop 2.5.1
  * Not tested with other versions of hadoop yet.
* A tar bundle of JRE 7 x64

#### Setup summary ####
* Put the hadoop and java bundles in the **dependencies** directory.
* Configure the installer using the **configuration.xml** file using **configuration.xml.example** as a template.

## Contact ###

You can contact me at *serpi90@gmail.com*
