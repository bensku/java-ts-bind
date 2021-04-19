# TypeScript binding generator for Java
java-ts-bind takes your Java source code and generates TypeScript types for it.
It is meant to be used with [GraalJS](https://github.com/oracle/graaljs)
to provide a strongly-typed scripting environment.

This project was created for [CraftJS](https://github.com/Valtakausi/craftjs),
a Bukkit plugin for writing plugins in JavaScript. It is based on earlier work
by [Ap3teus](https://github.com/Ap3teus).

No releases are currently provided. If you need it, compile it yourself
(or open a bug in the issue tracker).

## Usage
This is a command-line application.

* --format: output format
  * Currently only TS_TYPES is supported
* --in: input directory or source jar
* --symbols: symbol sources (compiled jars)
* --repo: Maven repo to fetch the source jar from
* --artifact: Artifact to fetch from given repo
  * tld.domain:artifact:version (Gradle-style)
* --offset: path offset inside the input
  * Mainly used for Java core types; see .github/workflows for an example
* --include: prefixes for included paths
  * By default, everything is included
* --exclude: prefixes for excluded paths
  * Processed after includes; nothing is excluded by default
* --blacklist: blacklisted type fragments
  * Types that have names which contain any of these are omitted
  * Methods and fields that would use them are also omitted!
* --packageJson: read these options from a JSON file
  * The options should be placed under `tsbindOptions` object
  * Names of options lack -- prefixes but are otherwise same
  * Handy when you already have package.json for publishing
* --index: generate index.d.ts that references other generated files

## Limitations
java-ts-bind does not necessarily generate *valid* TypeScript declarations.
The results are good enough to allow strongly-typed scripts, but it is
recommended that `noLibCheck` is used.

Please also note that java-ts-bind provides *only the types*. Implementing
a module loading system for importing them is left as an exercise for the
reader. For pointers, see [CraftJS](https://github.com/Valtakausi/craftjs)
which (at time of writing) implements a CommonJS module loader with
Java and TypeScript.