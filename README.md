# md-detect
Find code blocks in markdown

This script was borne out of a [discussion about adding syntax highlighting to code snippets](https://github.com/OfficeDev/VBA-content/pull/168) embedded inside of markdown.

Because code snippets are, by their very nature, probably not compilable, this script takes a fairly brute-force approach in determining what language a code snippet is most likely to be.

Currently, the script iterates line-by-line tokenizing input and comparing against programming-language keywords. When keywords are discovered, they are scored. The score is used to determine the likelihood that a code snippet is one language vs another.
