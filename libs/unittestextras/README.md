<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
<div align="center">
  <h1 align="center">jUnitTestExtras</h1>
  <p align="center">
    A library that contains functions and annotations for JUnit tests.
    <br />
    <a href="https://github.com/padaiyal/jMonocle/issues/new/choose">Report Bug/Request Feature</a>
  </p>

[![Contributors][contributors-shield]][contributors-url]
[![Issues][issues-shield]][issues-url]
[![Apache License][license-shield]][license-url] <br>
[![Maven build - Ubuntu latest][build-shield]][build-url]
[![Publish package to the Maven Central Repository][publish-workflow-badge]][publish-workflow-url] <br>
[![Latest version in Maven Central][maven-shield]][maven-url]
</div>

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
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
A library that contains functions and annotations for JUnit tests.
The following features are supported:
* Converts a string to an Array of specified type.

<!-- USAGE -->
## Usage
Add the following dependency tag to the pom.xml of the dependant project:
```
<dependency>
  <groupId>io.github.padaiyal.libs</groupId>
  <artifactId>unittestextras</artifactId>
  <version>2023.02.07</version>
</dependency>
```
NOTE: Refer to the [Maven repository][maven-url]
/ [releases][releases-url] to know the latest released version of this project.

Here's a sample snippet showing class conversion from the class' name:
```
String[] convertedArray = (String[]) ArrayConverter.convertStringToArray("hello, world", String[].class);
System.out.println(Arrays.toString(convertedArray)); // [hello,  world]
```

<!-- ROADMAP -->
## Roadmap
See the [open issues][issues-url] for a list of proposed features (and known issues).

<!-- CONTRIBUTING -->
## Contributing
Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project.
2. Create your branch. (`git checkout -b contribution/AmazingContribution`)
3. Commit your changes. (`git commit -m 'Add some AmazingContribution'`)
4. Push to the branch. (`git push origin contribution/AmazingContribution`)
5. Open a Pull Request.

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/padaiyal/jUnitTestExtras.svg?style=for-the-badge
[contributors-url]: https://github.com/padaiyal/jMonocle/graphs/contributors
[issues-shield]: https://img.shields.io/github/issues/padaiyal/jUnitTestExtras.svg?style=for-the-badge
[issues-url]: https://github.com/padaiyal/jMonocle/issues?q=is%3Aissue+is%3Aopen+label%3Aunittestextras
[license-shield]: https://img.shields.io/github/license/padaiyal/jUnitTestExtras.svg?style=for-the-badge
[license-url]: https://github.com/padaiyal/jMonocle/blob/main/libs/unittestextras/LICENSE
[build-shield]: https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20clean%20test%20verify/badge.svg?branch=main
[build-url]: https://github.com/padaiyal/jMonocle/actions/workflows/maven_build.yml?query=branch%3Amain
[publish-workflow-badge]:https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml/badge.svg
[publish-workflow-url]:https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml
[maven-shield]: https://img.shields.io/maven-central/v/io.github.padaiyal.libs/unittestextras
[maven-url]: https://central.sonatype.com/artifact/io.github.padaiyal.libs/unittestextras/2023.02.07/versions
[releases-url]: https://github.com/padaiyal/jMonocle/releases
