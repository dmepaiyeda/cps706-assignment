# Cps 706 Final Project

## Group #30 Members
- Frank Vumbaca
- Mitchell Mohorovich
- Yan Jedrasik

## Usage

### Compiling
**Intellij**
- To compile, simply load the project into Intellij (with this directory as root) and click the build button.

**Manual/Terminal**
- To compile the project, simply run `javac *.java` on the src directory.

### Running
The application contains multiple sub-commands for each component of the project. The default usage is:

```java Main <client|dns|web> <port> [config.txt[config2.txt[...]]]```

- **client | dns | web*** are each a different application; the client, dns server, and web servers respectively.
- **port** is the port which the server should run on
- **config files** vary depending on the command that is used:
  - **client** no config files are necessary
  - **dns** a single configuration file for the initial dns records.
  - **web** each listed file is content viewable on the server (does not support directories).