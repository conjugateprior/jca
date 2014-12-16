Changes in 0.2.0:

  New features:

  * ykconcordancer now reports documents that have no matches to stdout

  * ykconcordancer now takes dictionary and category arguments as an 
    alternative to pattern.  These read in a dictionary and show the 
    concordances for the specified dictionary category

  * ykconcordancer latex output now uses booktabs (commented out in output)

  * ykconcordancer html output tries to be a bit better aligned

  * ykreporter now pushes CSV output to stdout (in the platform encoding)
    if output argument is not provided

  * ykreporter no longer pushes anything except errors and warnings to
    stderr
