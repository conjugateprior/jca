## Apps for counting words or categories, and viewing them in context

This is experimental code with no guarantee of working at all.
All subject to change.

### Compiling and usage

* `ykreporter`

  A command line application to 
  apply a Yoshikoder dictionary to a bunch of files and
  record the result in a CSV file.

  To compile: ```ant cljar-cats```

* `ykwordcounter`

  A command line application to
  count words and drop the results in LDAC or Matrix Market format in a folder.

  To compile ```ant cljar-words```

* `ykconcordancer`
  
  A command line application to 
  construct concordances ('keyword in context' lines) for a word or phrase 
  across documents.

  To compile ```ant cljar-conc```
  
