<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
<div align="center">
  <h1 align="center">jCommandLine</h1>
  <p align="center">
    A library that can be used to execute commands in a shell or terminal.
    <br />
    <a href="https://github.com/padaiyal/jMonocle/issues/new/choose">Report Bug/Request Feature</a>
  </p>

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Apache License][license-shield]][license-url] <br>
![Maven build - Ubuntu latest](https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20clean%20test%20verify/badge.svg?branch=main)
[![Publish package to the Maven Central Repository](https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml/badge.svg)](https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml) <br>
[![Latest version in Maven Central](https://img.shields.io/maven-central/v/io.github.padaiyal.libs/commandline)](https://search.maven.org/artifact/io.github.padaiyal.libs/commandline)
</div>

<!--
*** To avoid retyping too much info. Do a search and replace with your text editor for the following:
    'jCommandLine'
 -->

<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
        <a href="#usage">Usage</a>
    </li>
    <li>
        <a href="#roadmap">Roadmap</a>
    </li>
    <li>
        <a href="#contributing">Contributing</a>
    </li>
    <li>
        <a href="#license">License</a>
    </li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
A library that can be used to execute commands in a shell or terminal.

<!-- USAGE -->
## Usage
This project is to be used as a dependency to other projects.
Adding this project as a dependency is as follows:
 1. Download the latest jar for this project from [GitHub packages](https://github.com/orgs/padaiyal/packages?repo_name=jCommandLine) and place it within 
    the dependant project.
 2. Add the following dependency tag to the pom.xml of the dependant project:
    ```
    <dependency>
        <groupId>org.java.padaiyal.utilities</groupId>
        <artifactId>commandline</artifactId>
        <version>2021.01.22</version>
        <scope>system</scope>
        <systemPath>${basedir}/<PATH_TO_JAR></systemPath>
    </dependency>
    ```
    NOTE: Refer the [GitHub packages](https://github.com/orgs/padaiyal/packages?repo_name=jCommandLine) 
    / [releases](https://github.com/padaiyal/jMonocle/releases) section for this repo to know 
    the latest released version of this project.

Here's a sample snippet showing the usage of CommandLineUtility:
```
Command command = new Command();

// Set equivalent commands for different command lines.
command.setCommand(CommandLine.BASH, "ls -al /");
command.setCommand(CommandLine.CMD, "dir");
command.setCommand(CommandLine.POWERSHELL, "dir");
command.setCommand(CommandLine.ZSH, "ls -al /");

Duration timeOutDuration = Duration.ofSeconds(5);

...

/*
Execute command and obtain response. The CommandLinUtility tries to identify appropriate 
command lines to run the command in by identifying the operating system.
*/
Response response1 = CommandLineUtility.executeCommand(command, duration);

// Executes the command specified using BASH and obtains the response 
Response response2 = CommandLineUtility.executeCommand(command, CommandLine.BASH, duration); 

// Command return code.
int returnCode = response1.getReturnCode();

// STDOUT content.
String stdOut = response1.getOutput(StdType.STDOUT);

// STDERR content.
String stdErr = response1.getOutput(StdType.STDERR);

...
```
For more such examples, checkout [CommandLineUtilityTest](https://github.com/padaiyal/jMonocle/tree/main/src/test/java/org/padaiyal/utilities/commandline/CommandLineUtilityTest.java)

<!-- ROADMAP -->
## Roadmap
See the [open issues](https://github.com/padaiyal/jMonocle/issues) for a list of proposed features (and known issues).

<!-- CONTRIBUTING -->
## Contributing
Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project.
2. Create your branch. (`git checkout -b contribution/AmazingContribution`)
3. Commit your changes. (`git commit -m 'Add some AmazingContribution'`)
4. Push to the branch. (`git push origin contribution/AmazingContribution`)
5. Open a Pull Request.


<!-- LICENSE -->
## License
Distributed under the Apache License. See [`LICENSE`](https://github.com/padaiyal/jMonocle/blob/main/LICENSE) for more information.


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/padaiyal/jMonocle.svg?style=for-the-badge
[contributors-url]: https://github.com/padaiyal/jMonocle/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/padaiyal/jMonocle.svg?style=for-the-badge
[forks-url]: https://github.com/padaiyal/jMonocle/network/members
[stars-shield]: https://img.shields.io/github/stars/padaiyal/jMonocle.svg?style=for-the-badge
[stars-url]: https://github.com/padaiyal/jMonocle/stargazers
[issues-shield]: https://img.shields.io/github/issues/padaiyal/jMonocle.svg?style=for-the-badge
[issues-url]: https://github.com/padaiyal/jMonocle/issues
[license-shield]: https://img.shields.io/github/license/padaiyal/jMonocle.svg?style=for-the-badge
[license-url]: https://github.com/padaiyal/jMonocle/blob/master/LICENSE
