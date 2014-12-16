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

To compile, type `ant`. 

This will create a folder called `dist` containing bash
scripts and the jar files that they refer to.  If you are on a Unix system, e.g.
OSX or Linux, then `mv` all these into `~/bin/` (making sure that's on your 
path) and use them as you would other command line tools. 

Windows executables are on the way.  Probably.  If I can find a Windows machine
to test them on.  Or I guess Windows people could get 
a proper command line interface from http://unxutils.sourceforge.net/