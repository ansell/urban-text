# urban-text
Get a feeling for which encoding neighbourhood your unknown text file is living in and attempt to convert between formats.

[![Build Status](https://travis-ci.org/ansell/urban-text.svg?branch=master)](https://travis-ci.org/ansell/urban-text) [![Coverage Status](https://coveralls.io/repos/ansell/urban-text/badge.svg?branch=master)](https://coveralls.io/r/ansell/urban-text?branch=master)

# Setup

Install Maven and Git

Download the Git repository.

Set the relevant programs to be executable.

    chmod a+x ./urbanreporter
    chmod a+x ./urbanengineer

# Urban Text Reporter

Checks input across all of the supported JVM character encodings to quickly verify which encodings match, and for the encodings that fail, show the byte position and the 3-5 bytes surrounding the first location that the character decoder failed at.

## Usage

Run urbanreporter with --help to get usage details:

    ./urbanreporter --help

Run urbanreporter with a file and write a CSV document containing results to standard out:

    ./urbanreporter --input /path/to/my/file.txt

Run urbanreporter with a file and write a CSV document containing results to a file:

    ./urbanreporter --input /path/to/my/file.txt --output encoder-results.csv

Run urbanreporter with input from standard in and write a CSV document containing results to standard out:

    ./urbanreporter < /path/to/my/file.txt


# Urban Text Engineer

Attempts to convert between two different encodings, failing if the input does not match the given input encoding or the output cannot consistently map all of the bytes from the input encoding to the output encoding.

## Usage

Run urbanengineer with --help to get usage details:

    ./urbanengineer --help

Run urbanengineer with a file for input and a file for output:

    ./urbanengineer --input-charaset ISO8859-1 --output-charaset UTF-8 --input /path/to/my/file.txt --output /path/to/my/file-utf8.txt

Run urbanengineer with input from standard in and output to standard out:

    ./urbanengineer --input-charaset ISO8859-1 --output-charaset UTF-8 < /path/to/my/file.txt

# Maven

    <dependency>
        <groupId>com.github.ansell.text</groupId>
        <artifactId>urban-text</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

# Changelog

## 2016-09-29
* Add urbanengineer for conversion between encodings

## 2016-09-28
* Initial version 
* Add urbanreporter for checking validity across all of the available JVM encodings
