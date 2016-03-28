 - notify just before autorevert
 - change size counting can be not intuitive because of the way text diff works,
   e.g. adding new line can change deleted fragment into edited fragment and bump amount of changes
 - link/action to temporary bump change size limit (instead of skipping notifications till next commit)
 - change count to go up for a second after moving files until IDE refreshes VCS changes
