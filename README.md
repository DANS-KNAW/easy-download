easy-download
===========
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-download.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-download)


SYNOPSIS
--------

    easy-download run-service


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

HTTP service
------------

When started with the sub-command `run-service` a REST API becomes available with HTTP method `GET` only.
In a path pattern `*` refers to any completion of the path, placeholders for variables start with a colon,
and optional parts are enclosed in square brackets.

Path       | Action
-----------|------------------------------------
`/`        | Return a simple message to indicate that the service is up: "File Download Servlet running..."
`/:uuid/*` | Return the contents of the file with bag-id `:uuid` and bag local path `*`


EXAMPLES
--------

    curl http://test.dans.knaw.nl:20110/
    curl http://test.dans.knaw.nl:20110/40594b6d-8378-4260-b96b-13b57beadf7c/data/pakbon.xml


INSTALLATION AND CONFIGURATION
------------------------------


### Depending on services

* [easy-bag-store](https://github.com/DANS-KNAW/easy-bag-store/)


### Installation steps

1. Unzip the tarball to a directory of your choice, typically `/usr/local/`
2. A new directory called easy-download-<version> will be created
3. Add the command script to your `PATH` environment variable by creating a symbolic link to it from a directory that is
   on the path, e.g. 
   
        ln -s /usr/local/easy-download-<version>/bin/easy-download /usr/bin


### Configuration

General configuration settings can be set in `cfg/application.properties` and logging can be configured
in `cfg/logback.xml`. The available settings are explained in comments in aforementioned files.

### Security advice

Keep the depending services behind a firewall.
Only expose the download servlet through a proxy, map for example:
`http://easy.dans.knaw.nl/ark:/73189/` to `http://localhost:20150/` 


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

        git clone https://github.com/DANS-KNAW/easy-download.git
        cd easy-download
        mvn install
