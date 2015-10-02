## Apps for counting words or categories and viewing them in context

This is experimental code with no guarantee of working at all.
All subject to change.

### Installing Java 

You'll need an up to date Java install for all these things.
Specifically, the JDK.  You can get the currently latest one
[here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
after accepting the terms and conditions radio button and running the
installer.

You can check on Unix / Mac by opening a terminal window and typing 

    java -version

This should give you three lines of output. The top one should be

    java version "1.8.0_60"

All good.  

### Tools

The command line tools now run off a common root, this is
either the jarfile `jca-<version>.jar` or a bash script called `jca`
(that just wraps a `java -jar jca-<version>.jar` call).

Here are the tools:

* `cat`: Apply a Yoshikoder dictionary to a bunch of files and record the
  result in a CSV file.  

* `word`: Count words and drop the results in LDAC, Matrix Market, or
  CSV format into a folder.  If you have more than a handful of
  documents you probably don't want to choose the CSV option.

* `conc`: Construct concordances (a.k.a. 'keyword in context') for a
  word, a phrase, or a category from a content analysis dictionary,
  for a bunch of documents.

* `line`: Turn *folders* of documents into Mallet's prefered
  one-per-line format.  This has three columns.  The first two are the
  file's old name and the final one is the text in one long line.

* `desc`: List some summary statistics for your files.

In case you're wondering, this code deals with *multi-word pattern
matches* in dictionaries and outside them.  That code just hasn't made
it into the Yoshikoder yet.

### Usage: Command line

The release file `jca-<version>.zip` contains a jar file and tiny
bash script `jca` to drive it.  

Put the jar file anywhere you like and note down its path.  Then type

    java -Xmx512M -jar <path>jca-<version>.jar <tool> [options] <files>

where `<path>` is the folder where you left the jar file (`~/bin/` is a
pretty good place for Unix / Mac folk), `<version>` is the latest
version (currently 0.2.4.1), `<tool>` is one of the tools listed above,
`options` vary by tool, and `<files>` are names of text files and
folders full of text files.

You can give any mixture of files and folder names.  If a folder
name is provided then the documents within that folder will be
analysed, but the tools don't look into any subfolders that might be
down there too.

Unix / Mac folk might find it more convenient to add 

    alias jca='java -Xmx512M -jar /path/to/jca-<version>.jar'

to their `~/.bash_profile` and then use

    jca <tool> [options] <files>
 
instead.

In case you're curious `-Xmx512M` gives the application half a gigabyte
of memory (512M) for the tools to work within. This should be plenty.
If it isn't, then either you're building CSV files from really large
numbers of documents, your documents are absurdly large, or my code is
taking a memory leak.  Let me know if you think it's the latter.

## Usage: R

If you don't really love the command line, you might prefer to interact
with these tools via an R package.  That R package is
[here](https://github.com/conjugateprior/rjca).

### Compilation

If you want to compile the code, pull the source and type `ant dist`.
You know, `ant` - that thing they built back when XML seemed like a
good idea.

Does she go? 
[![Build Status](https://travis-ci.org/conjugateprior/jca.svg?branch=master)](https://travis-ci.org/conjugateprior/jca)

Will Lowe, October 2015
