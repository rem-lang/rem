## TODO:

- Build executable by default and fail to build if there is no function named `main` in the file pointed to by the user.
- If the entry file imports a file, build the file automatically and add it to the list of link dependencies.
- Allow building if the user specifies the `--lib` flag to the compiler and build a static library when it exists (essentially an object file).
- Allow specifying extra link libraries to the compiler which must be a Rem library or a C library (see if you can support C++ later).

## Package Manager - rema
- The entrypoint for an executable is index.r
- The entrypoint for a library is lib.r