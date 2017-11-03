easy-download
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-download.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-download)

<!-- Remove this comment and extend the descriptions below -->


SYNOPSIS
--------

    easy-download (synopsis of command line parameters)
    easy-download (... possibly multiple lines for subcommands)


DESCRIPTION
-----------

Download files from the archive


ARGUMENTS
---------

    Options:

      --help      Show help message
      --version   Show version of this program
    
    Subcommand: run-service - Starts EASY Download as a daemon that services HTTP requests
      --help   Show help message
    ---

EXAMPLES
--------

    easy-download -o value


INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps

1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-download-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-download-<version>/bin/easy-download /usr/bin


### Configuration

General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-download.git
        cd easy-download
        mvn install
