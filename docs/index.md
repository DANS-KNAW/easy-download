---
title: Manual
layout: home
---

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

      -h, --help      Show help message
      -v, --version   Show version of this program

    Subcommand: run-service - Starts EASY Download as a daemon that services HTTP requests
      -h, --help   Show help message
    ---

INSTALLATION AND CONFIGURATION
------------------------------

Currently this project is built only as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/easy-download`, the configuration files to `/etc/opt/dans.knaw.nl/easy-download`,
and will install the service script for `systemd`.

### Depending on services

* [easy-bag-store]({{ easy_bag_store }})


### Security advice

Keep the depending services behind a firewall.
Only expose the download servlet through a proxy.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

        git clone https://github.com/DANS-KNAW/easy-download.git
        cd easy-download
        mvn install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.
