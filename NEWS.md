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

Changes in 0.2.1:

  Bug fixes:

  * bash scripts now have the proper paths and should work according to 
    installation instructions on the Github project wiki

  * No mysterious nulls turn up when there are no input arguments to ykconc 

  Developer:

  * The `dist` task now makes its own zip file with bash scripts and jars

Changes in 0.2.2:
 
   Bug fixes:
   
   * Shell scripts were losing quoted elements
   
   * More sensible error messages for no arguments
   
   * category headers in ykcats output were not showing nested structure
   
   * More instructions on the project wiki
   
Changes in 0.2.3:

   (Internal changes mostly)

   Developer:

   * R functions to run the jar files   
