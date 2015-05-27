## Matrix Market format

This folder contains three files:

	words.csv
	docs.csv
	data.mtx
	
'docs.csv' lists the files that were successfully processed and 'words.csv'
 lists the word types found in the readable documents.

The file 'data.mtx' is in Matrix Market 'coordinate' format. This consists 
of a header and lines of the form

    A B C
    
where A, B, and C are the row number, column number, and value respectively 
of asingle element of the document by term word count matrix.  The document
named on line A of documents.csv contains the word named on line B of words.csv
C times. Further details are at http://math.nist.gov/MatrixMarket/formats.html

## R

You can drive the word counting code from the rjca package, if you'd prefer.
See the function 'jca_word'.

If you'd prefer to just use base R then define the following function

    read_mtx <- function(folder){ 
      require(Matrix)
      ff <- readMM(file.path(folder, "data.mtx"))
      rown <- read.csv(file.path(folder, "docs.csv"), header=FALSE)$V1 
      coln <- read.csv(file.path(folder, "words.csv"), header=FALSE)$V1
      dimnames(ff) <- list(documents=rown, words=coln)
      ff
    } 

To read the word counts stored here in Matrix Market format into R from 
an output folder called 'folder' all this function like

    wcm <- read_mtx("folder")
