
# rJava::.jpackage(pkgname, jars="*", lib.loc = libname)
# .jaddClassPath("/Users/will/wip/jca/ykreporter-0.2.3.jar")
# /Users/will/Dropbox/blogposts/speeches
ykdesc <- function(files){
  desc <- .jnew("org/conjugateprior/ca/app/Description")
  ## laboriously make an array of references to files
  flist <- rep(NULL, len(files))
  for (i in 1:len(flist))
    flist[i] <- .jnew("java/io/File", files[i])
  fl <- .jarray(files, contents.class="java/io/File")
  .jcall(desc,,method="setFiles",fl)
  .jcall(desc,,"processFiles")
}

ykcats <- function(dictionary, args, ...){
  fs <- c(...) ## the files
  jarfile = args$jarfile
  args$jarfile <- NULL
  outfile <- tempfile(pattern="ykcats-output", tmpdir = getwd())
  
  sc <- c()
  for (n in names(args)){
   if (is.logical(args[[n]]) && args[[n]])
      sc <- c(sc, paste0("-", n))
    else 
      sc <- c(sc, paste0("-", n, " ", args[[n]]))
  }
  str <- c("-jar", jarfile, "-dictionary", dictionary, 
           sc, "-output", outfile, fs)
  
  message("Running\n", "java ", paste(str), "\n")
  system2('java', args=str)
  message("\nOutput file:", outfile)
  read.csv(file.path(outfile, "data.csv"), row.names=1, header=TRUE)
}

ykconc <- function(args, ...){
  fs <- c(...) ## the files
  jarfile = args$jarfile
  args$jarfile <- NULL
  
  
  sc <- c()
  for (n in names(args)){
    if (is.logical(args[[n]]) && args[[n]])
      sc <- c(sc, paste0("-", n))
    else 
      sc <- c(sc, paste0("-", n, " ", args[[n]]))
  }
  
  suf <- switch(tolower(args$format), 
                    html='.html',
                    text='.txt',
                    utf8='-utf8.txt',
                    "utf-8"="-utf8.txt",
                    latex='.tex')
  outfile <- tempfile(pattern="ykconc-output", fileext=suf,
                      tmpdir = getwd())
  
  str <- c("-jar", jarfile, sc, "-output", outfile, fs)
  
  message("Running\n", "java ", paste(str), "\n")
  system2('java', args=str)
  message("\nOutput file:", outfile)
  
  
  read.csv(file.path(outfile, "data.csv"), row.names=1, header=TRUE)
}

