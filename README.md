## Apps for counting words or categories and viewing them in context

This is experimental code with no guarantee of working at all.
All subject to change.

### Installing Java 

You'll need an up to date Java install for all these things.
Specifically, the JDK.  You can get the currently latest one
[here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) after accepting the terms and conditions radio button and running the installer. 

Mac users will want to choose the link marked
'jdk-8u45-macosx-x64.dmg'.  Mac (and probably Linux) users will also
probably want to put the following lines in the `.bash_profile` file
in their home directories

    export JAVA_HOME="`/usr/libexec/java_home -v 1.8`"
    export PATH=$JAVA_HOME/bin:$PATH

(It doesn't much matter where you put them).

Note: If you're new to this sort of thing, you may not have this file
already.  That's not a problem.  Just type the following:
   
    touch ~/.bash_profile   ## creates the file if it doesn't exist 
    open -e ~/.bash_profile ## edits the file in TextEdit

then add the lines, and save when you're done.  Now open a fresh Terminal window and type

    java -version

This should give you 

    java version "1.8.0_45"
    Java(TM) SE Runtime Environment (build 1.8.0_45-b14)
    Java HotSpot(TM) 64-Bit Server VM (build 25.45-b02, mixed mode)

All good.

### Applications

These command line applications now run off a common root, this is
either the jarfile `jca-<version>.jar` or a bash script called `jca`
(that just wraps a `java -jar jca-<version>.jar` call).

In case you're wondering, this code deals with multi-word pattern
matches in dictionaries and outside them.  That code just hasn't made it into
the Yoshikoder yet.

Here are the applications.  (To get usage information replace the
options and files arguments with` -help`)

* `jca cat [options] [files]`

  Apply a Yoshikoder dictionary to a bunch of files and record the
  result in a CSV file.  

* `jca word [options] [files]`

  Count words and drop the results in LDAC or Matrix Market format into
  a folder.

* `jca conc [options] [files]`
  
  Construct concordances ('keyword in context' lines) for a word, a phrase, 
  or a category from a content analysis dictionary, for a bunch of documents.

* `jca line [files]`

  Turn folders of documents into Mallet's prefered one-per-line
  format.  This has three columns.  The first two are the file's old
  name and the final one is the text in one long line.

Note: you can give any mixture of files and folder names.  If a folder
name is provided documents directly inside it will be analysed.
Folders within the folder you provided, or *their* contents, will not
be analysed.

### Compilation

To compile, type `ant dist`.  You know, `ant`.  That thing your
parents talked about.

The resulting file `jca-<version>.zip` contains the jar file and tiny
bash script `jca` to drive it.  You can ignore
the script and type `java -jar /path/to/jca-<version>.jar` instead of `jca`,
provided you know the full path to the jar file.

Installation details are out of date on the [project
wiki](https://github.com/conjugateprior/jca/wiki).  But I'm getting to
it.

### Usage: R 

If you don't really love the commandline, you might prefer to interact with these
tools via an R package.  That R package is [here](https://github.com/conjugateprior/rjca).
In particular, don't bother with the functions in `rfuncs.R`

[![Build Status](https://travis-ci.org/conjugateprior/jca.svg?branch=master)](https://travis-ci.org/conjugateprior/jca)

Will Lowe, May 2015


