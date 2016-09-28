# urban-text
Get a feeling for which encoding neighbourhood your unknown text file is living in

[![Build Status](https://travis-ci.org/ansell/urban-text.svg?branch=master)](https://travis-ci.org/ansell/urban-text) [![Coverage Status](https://coveralls.io/repos/ansell/urban-text/badge.svg?branch=master)](https://coveralls.io/r/ansell/urban-text?branch=master)

# Setup

Install Maven and Git

Download the Git repository.

Set the relevant programs to be executable.

    chmod a+x ./urban-text

# Urban Text Reporter

## Usage

Run urban-text with --help to get usage details:

    ./urban-text --help

Run urban-text with a file and write a CSV document containing results to standard out:

    ./urban-text --input /path/to/my/file.txt

Run urban-text with a file and write a CSV document containing results to a file:

    ./urban-text --input /path/to/my/file.txt --output encoder-results.csv

# Maven

    <dependency>
        <groupId>com.github.ansell.text</groupId>
        <artifactId>urban-text</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

# Changelog

## 2016-09-28
* Initial version 

