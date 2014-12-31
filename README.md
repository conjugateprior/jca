## Apps for counting words or categories, and viewing them in context

This is experimental code with no guarantee of working at all.
All subject to change.

### Applications

* `ykcats`

  A command line application to 
  apply a Yoshikoder dictionary to a bunch of files and
  record the result in a CSV file.

* `ykwords`

  A command line application to
  count words and drop the results in LDAC or Matrix Market format in a folder.

* `ykconc`
  
  A command line application to 
  construct concordances ('keyword in context' lines) for a word, phrase, 
  or category from a content analysis dictionary, for a bunch of documents.

### Compilation

To compile, type `ant dist`. 

The resulting file `yktools-<version>.zip` contains three jar files and
three bash scripts to call them.  Installation details are on the 
[project wiki](https://github.com/conjugateprior/jca/wiki).

### Usage: command line

The [project wiki](https://github.com/conjugateprior/jca/wiki)
describes how to install and use these on a Unix system (Linux or OSX)

Windows executables *might* be on the way.  If I can find a Windows
machine to test them on.  Or I guess Windows people could get a proper
command line interface from <http://unxutils.sourceforge.net/>.

### Usage: R

There's now a small pile of `source`able R functions in `rfuncs.R`,
these work like the `ykcats` script only from within R.
Instructions on how to use it are on the project wiki (link above).

While these are not wrapped up in their own R package you'll have to
hand in the location of the relevant jarfile.
