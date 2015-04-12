## Apps for counting words or categories, viewing them in context

This is experimental code with no guarantee of working at all.
All subject to change.

### Applications

Speaking of which...

These tools now run off a common root, usually a shell script called `jca`
(but equally well a `java -jar jca-<version>.jar` if you're not on a proper
operating system).  From that root you launch the different programms:

In case you're wondering, this code deals with multi-word pattern
matches in dictionaries and outside them.  The code just hasn't made it into
the Yoshikoder yet.

* `yk cat [options] [files]`

  Apply a Yoshikoder dictionary to a bunch of files and record the
  result in a CSV file.  

* `yk word [options] [files]`

  Count words and drop the results in LDAC or Matrix Market format into
  a folder.

* `yk conc [options] [files]`
  
  Construct concordances ('keyword in context' lines) for a word, a phrase, 
  or a category from a content analysis dictionary, for a bunch of documents.

* `yk line [files]`

  Turn folders of documents into Mallet's prefered one-per-line
  format.  This has three columns.  The first two are the file's old
  name and the final one is the text in one long line.

### Compilation

To compile, type `ant dist`.  You know, `ant`.  That thing your
parents talked about.

The resulting file `jca-<version>.zip` contains the jar file and tiny
bash script to drive it.

Installation details are out of date on the [project
wiki](https://github.com/conjugateprior/jca/wiki).  But I'm getting to
it.

### Usage: R (even more experimental than usual)

This thing has been refactored enough to be drivable using
`rJava`. However, I'm holding off an intergration while rJava, Oracle,
and Apple decide how they are going to deal with the path screwups
they've collectively generated.

There's a small pile of `source`able R functions in `rfuncs.R`, but
they're now out of date so don't use them in versions greater than
0.2.3

[![Build Status](https://travis-ci.org/conjugateprior/jca.svg?branch=master)](https://travis-ci.org/conjugateprior/jca)

Will Lowe, April 2015


